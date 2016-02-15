package ch.papers.payments;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.bitcoinj.core.AbstractWalletEventListener;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.DownloadProgressTracker;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Peer;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.Wallet;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.script.Script;
import org.bitcoinj.utils.ExchangeRate;
import org.bitcoinj.utils.Fiat;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ch.papers.objectstorage.UuidObjectStorage;
import ch.papers.objectstorage.listeners.DummyOnResultListener;
import ch.papers.objectstorage.listeners.OnResultListener;
import ch.papers.payments.models.ECKeyWrapper;
import ch.papers.payments.models.TransactionWrapper;
import ch.papers.payments.models.filters.ECKeyWrapperFilter;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 14/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class WalletService extends Service{

    private final static String TAG = WalletService.class.getName();


    private ExchangeRate exchangeRate;
    private WalletAppKit kit;

    public class WalletServiceBinder extends Binder {
        public Address getCurrentReceiveAddress(){
            return WalletService.this.kit.wallet().currentReceiveAddress();
        }

        public void setExchangeRate(ExchangeRate exchangeRate){
            WalletService.this.exchangeRate = exchangeRate;
        }

        public Coin getBalance(){
            return WalletService.this.kit.wallet().getBalance();
        }

        public Fiat getBalanceFiat(){
            return WalletService.this.exchangeRate.coinToFiat(WalletService.this.kit.wallet().getBalance());
        }

        public List<TransactionWrapper> getTransactionsByTime(){
            final List<TransactionWrapper> transactions = new ArrayList<TransactionWrapper>();
            for (Transaction transaction:WalletService.this.kit.wallet().getTransactionsByTime()) {
                transactions.add(new TransactionWrapper(transaction,WalletService.this.kit.wallet()));
            }
            return transactions;
        }
    }

    private final WalletServiceBinder walletServiceBinder = new WalletServiceBinder();


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        UuidObjectStorage.getInstance().init(this.getFilesDir());

        this.kit = new WalletAppKit(Constants.PARAMS, this.getFilesDir(), Constants.WALLET_FILES_PREFIX) {
            @Override
            protected void onSetupCompleted() {
                if (wallet().getKeychainSize() < 1) {
                    UuidObjectStorage.getInstance().getFirstMatchEntry(new ECKeyWrapperFilter(Constants.WALLET_KEY_NAME), new OnResultListener<ECKeyWrapper>() {
                        @Override
                        public void onSuccess(ECKeyWrapper result) {
                            wallet().importKey(result.getKey());
                        }

                        @Override
                        public void onError(String message) {
                            ECKeyWrapper walletKey = new ECKeyWrapper(new ECKey().getPrivKeyBytes(), Constants.WALLET_KEY_NAME);
                            wallet().importKey(walletKey.getKey());
                            UuidObjectStorage.getInstance().addEntry(walletKey, DummyOnResultListener.getInstance(), ECKeyWrapper.class);
                        }
                    }, ECKeyWrapper.class);
                }

                kit.wallet().addEventListener(new AbstractWalletEventListener() {
                    @Override
                    public void onCoinsSent(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {
                        super.onCoinsSent(wallet, tx, prevBalance, newBalance);
                        Intent walletProgressIntent = new Intent(Constants.WALLET_BALANCE_CHANGED_ACTION);
                        walletProgressIntent.putExtra("balance", newBalance.value);
                        LocalBroadcastManager.getInstance(WalletService.this).sendBroadcast(walletProgressIntent);
                    }

                    @Override
                    public void onScriptsChanged(Wallet wallet, List<Script> scripts, boolean isAddingScripts) {
                        super.onScriptsChanged(wallet, scripts, isAddingScripts);
                        Intent walletProgressIntent = new Intent(Constants.WALLET_SCRIPTS_CHANGED_ACTION);
                        LocalBroadcastManager.getInstance(WalletService.this).sendBroadcast(walletProgressIntent);
                    }

                    @Override
                    public void onTransactionConfidenceChanged(Wallet wallet, Transaction tx) {
                        super.onTransactionConfidenceChanged(wallet, tx);
                        Intent walletProgressIntent = new Intent(Constants.WALLET_TRANSACTIONS_CHANGED_ACTION);
                        walletProgressIntent.putExtra("transactionHash", tx.getHashAsString());
                        LocalBroadcastManager.getInstance(WalletService.this).sendBroadcast(walletProgressIntent);
                    }

                    @Override
                    public void onCoinsReceived(Wallet w, Transaction tx, Coin prevBalance, Coin newBalance) {
                        Intent walletProgressIntent = new Intent(Constants.WALLET_BALANCE_CHANGED_ACTION);
                        walletProgressIntent.putExtra("balance", newBalance.value);
                        LocalBroadcastManager.getInstance(WalletService.this).sendBroadcast(walletProgressIntent);
                    }
                });

                Intent walletProgressIntent = new Intent(Constants.WALLET_READY_ACTION);
                LocalBroadcastManager.getInstance(WalletService.this).sendBroadcast(walletProgressIntent);
            }
        };



        kit.setDownloadListener(new DownloadProgressTracker() {

            @Override
            public void onChainDownloadStarted(Peer peer, int blocksLeft) {
                super.onChainDownloadStarted(peer, blocksLeft);
                Log.d(TAG,"started download of block:" + blocksLeft);
            }

            @Override
            public void progress(double pct, int blocksSoFar, Date date) {
                super.progress(pct, blocksSoFar, date);
                Intent walletProgressIntent = new Intent(Constants.WALLET_PROGRESS_ACTION);
                walletProgressIntent.putExtra("progress", pct);
                walletProgressIntent.putExtra("blocksSoFar", blocksSoFar);
                walletProgressIntent.putExtra("date", date);
                LocalBroadcastManager.getInstance(WalletService.this).sendBroadcast(walletProgressIntent);
                Log.d(TAG,"progress "+pct);
            }

            @Override
            protected void doneDownload() {
                super.doneDownload();
                Log.d(TAG,"done download");
            }
        });

        kit.startAsync();

        Log.d(TAG,"wallet started");

        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.kit.stopAsync().awaitTerminated();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG,"on bind");
        return this.walletServiceBinder;
    }
}
