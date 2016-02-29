package ch.papers.payments;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.coinblesk.json.KeyTO;
import com.coinblesk.json.PrepareHalfSignTO;
import com.coinblesk.json.RefundTO;
import com.coinblesk.util.BitcoinUtils;
import com.coinblesk.util.SerializeUtils;
import com.google.common.collect.ImmutableList;

import org.bitcoinj.core.AbstractWalletEventListener;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.DownloadProgressTracker;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.Peer;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.Wallet;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.utils.ExchangeRate;
import org.bitcoinj.utils.Fiat;

import java.io.IOException;
import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ch.papers.objectstorage.UuidObjectStorage;
import ch.papers.objectstorage.listeners.DummyOnResultListener;
import ch.papers.objectstorage.listeners.OnResultListener;
import ch.papers.payments.communications.http.CoinbleskWebService;
import ch.papers.payments.models.ECKeyWrapper;
import ch.papers.payments.models.TransactionWrapper;
import ch.papers.payments.models.filters.ECKeyWrapperFilter;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 14/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class WalletService extends Service {

    private final static String TAG = WalletService.class.getName();

    private final Retrofit retrofit = new Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(Constants.COINBLESK_SERVER_BASE_URL)
            .build();

    private ExchangeRate exchangeRate;
    private WalletAppKit kit;

    private Script multisigAddressScript;
    private ECKey multisigClientKey;
    private ECKey multisigServerKey;

    public class WalletServiceBinder extends Binder {
        public Address getCurrentReceiveAddress() {
            if (multisigAddressScript != null) {
                return multisigAddressScript.getToAddress(Constants.PARAMS);
            } else {
                return WalletService.this.kit.wallet().currentReceiveAddress();
            }
        }

        public void setExchangeRate(ExchangeRate exchangeRate) {
            WalletService.this.exchangeRate = exchangeRate;
        }

        public Coin getBalance() {
            return WalletService.this.kit.wallet().getBalance();
        }

        public Fiat getBalanceFiat() {
            return WalletService.this.exchangeRate.coinToFiat(WalletService.this.kit.wallet().getBalance());
        }

        public List<TransactionWrapper> getTransactionsByTime() {
            final List<TransactionWrapper> transactions = new ArrayList<TransactionWrapper>();
            for (Transaction transaction : WalletService.this.kit.wallet().getTransactionsByTime()) {
                transactions.add(new TransactionWrapper(transaction, WalletService.this.kit.wallet()));
            }
            return transactions;
        }

        public List<TransactionOutput> getUnspentInstantOutputs() {
            List<TransactionOutput> unspentInstantOutputs = new ArrayList<TransactionOutput>();
            for (TransactionOutput unspentTransactionOutput : kit.wallet().calculateAllSpendCandidates(true, false)) {
                if (unspentTransactionOutput.getScriptPubKey().getToAddress(Constants.PARAMS).equals(this.getCurrentReceiveAddress())) {
                    unspentInstantOutputs.add(unspentTransactionOutput);
                }
            }
            return unspentInstantOutputs;
        }

        public void instantSendCoins(final Address address, final Coin amount) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        final CoinbleskWebService service = retrofit.create(CoinbleskWebService.class);
                        // let server sign first
                        final PrepareHalfSignTO clientHalfSignTO = new PrepareHalfSignTO();
                        clientHalfSignTO.amountToSpend(amount.longValue());
                        clientHalfSignTO.clientPublicKey(multisigClientKey.getPubKey());
                        clientHalfSignTO.p2shAddressTo(address.toString());

                        Response<PrepareHalfSignTO> prepareHalfSignTOResponse = service.prepareHalfSign(clientHalfSignTO).execute();
                        final PrepareHalfSignTO serverHalfSignTO = prepareHalfSignTOResponse.body();

                        // now let us sign and verify
                        final List<TransactionOutput> unspentTransactionOutputs = getUnspentInstantOutputs();


                        final Transaction transaction = BitcoinUtils.createTx(Constants.PARAMS, unspentTransactionOutputs, getCurrentReceiveAddress(), address, amount.longValue(), null);
                        final Script redeemScript =ScriptBuilder.createRedeemScript(2, ImmutableList.of(multisigServerKey,multisigClientKey));
                        Log.d(TAG,transaction.hashForSignature(0,redeemScript, Transaction.SigHash.ALL,false).toString());
                        final List<TransactionSignature> clientTransactionSignatures = BitcoinUtils.partiallySign(transaction, redeemScript, multisigClientKey);
                        final List<TransactionSignature> serverTransactionSignatures = SerializeUtils.deserializeSignatures(serverHalfSignTO.signatures());

                        for (int i = 0; i < clientTransactionSignatures.size(); i++) {
                            final TransactionSignature serverSignature = serverTransactionSignatures.get(i);
                            final TransactionSignature clientSignature = clientTransactionSignatures.get(i);

                            Script p2SHMultiSigInputScript = ScriptBuilder.createP2SHMultiSigInputScript(ImmutableList.of(serverSignature,clientSignature), redeemScript);
                            transaction.getInput(i).setScriptSig(p2SHMultiSigInputScript);
                            transaction.getInput(i).verify();
                        }

                        // generate refund
                        final Transaction halfSignedRefundTransaction = PaymentProtocol.getInstance().generateRefundTransaction(transaction.getOutput(1),getCurrentReceiveAddress());
                        final List<TransactionSignature> refundTransactionSignatures = BitcoinUtils.partiallySign(transaction, redeemScript, multisigClientKey);
                        final RefundTO clientRefundTO = new RefundTO();
                        clientRefundTO.clientPublicKey(multisigClientKey.getPubKey());
                        clientRefundTO.clientSignatures(SerializeUtils.serializeSignatures(refundTransactionSignatures));
                        clientRefundTO.refundTransaction(halfSignedRefundTransaction.bitcoinSerialize());

                        // let server sign
                        final RefundTO serverRefundTo = service.refund(clientRefundTO).execute().body();
                        final Transaction refundTransaction = new Transaction(Constants.PARAMS,serverRefundTo.refundTransaction());
                        refundTransaction.verify();
                        refundTransaction.getInput(0).verify();

                        // all good our refund tx is safe, we can broadcast
                        kit.peerGroup().broadcastTransaction(transaction);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }

        public void sendCoins(Address address, Coin amount) {
            if (multisigAddressScript != null && getUnspentInstantOutputs().size()>0) {
                instantSendCoins(address, amount);
            } else {
                try {
                    final Transaction transaction = WalletService.this.kit.wallet().createSend(address, amount);
                    WalletService.this.kit.peerGroup().broadcastTransaction(transaction);
                } catch (InsufficientMoneyException e) {
                    final Intent walletInsufficientBalanceIntent = new Intent(Constants.WALLET_INSUFFICIENT_BALANCE);
                    LocalBroadcastManager.getInstance(WalletService.this).sendBroadcast(walletInsufficientBalanceIntent);
                }
            }
        }

        public TransactionWrapper getTransaction(String transactionHash) {
            return new TransactionWrapper(WalletService.this.kit.wallet().getTransaction(Sha256Hash.wrap(transactionHash)), WalletService.this.kit.wallet());
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
                            UuidObjectStorage.getInstance().commit(DummyOnResultListener.getInstance());
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

                        Intent walletCoinsSentIntent = new Intent(Constants.WALLET_COINS_SENT);
                        walletCoinsSentIntent.putExtra("balance", newBalance.value);
                        LocalBroadcastManager.getInstance(WalletService.this).sendBroadcast(walletCoinsSentIntent);
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


                        Intent walletCoinsReceivedIntent = new Intent(Constants.WALLET_COINS_RECEIVED);
                        walletCoinsReceivedIntent.putExtra("balance", newBalance.value);
                        LocalBroadcastManager.getInstance(WalletService.this).sendBroadcast(walletCoinsReceivedIntent);
                    }
                });

                UuidObjectStorage.getInstance().getFirstMatchEntry(new ECKeyWrapperFilter(Constants.MULTISIG_SERVER_KEY_NAME), new OnResultListener<ECKeyWrapper>() {
                    @Override
                    public void onSuccess(final ECKeyWrapper serverKey) {
                        UuidObjectStorage.getInstance().getFirstMatchEntry(new ECKeyWrapperFilter(Constants.MULTISIG_CLIENT_KEY_NAME), new OnResultListener<ECKeyWrapper>() {
                            @Override
                            public void onSuccess(ECKeyWrapper clientKey) {
                                setupMultiSigAddress(clientKey.getKey(),
                                        serverKey.getKey());
                            }

                            @Override
                            public void onError(String s) {

                            }
                        }, ECKeyWrapper.class);
                    }


                    @Override
                    public void onError(String message) {
                        try {

                            CoinbleskWebService service = retrofit.create(CoinbleskWebService.class);
                            final ECKey clientMultiSigKey = new ECKey();
                            final KeyTO clientKey = new KeyTO();
                            clientKey.publicKey(clientMultiSigKey.getPubKey());
                            Response<KeyTO> response = service.keyExchange(clientKey).execute();
                            if(response.isSuccess()) {
                                final KeyTO serverKey = response.body();
                                final ECKey serverMultiSigKey = ECKey.fromPublicOnly(serverKey.publicKey());
                                UuidObjectStorage.getInstance().addEntry(new ECKeyWrapper(clientMultiSigKey.getPrivKeyBytes(), Constants.MULTISIG_CLIENT_KEY_NAME), DummyOnResultListener.getInstance(), ECKeyWrapper.class);
                                UuidObjectStorage.getInstance().addEntry(new ECKeyWrapper(serverMultiSigKey.getPubKey(), Constants.MULTISIG_SERVER_KEY_NAME, true), DummyOnResultListener.getInstance(), ECKeyWrapper.class);
                                UuidObjectStorage.getInstance().commit(DummyOnResultListener.getInstance());

                                setupMultiSigAddress(clientMultiSigKey, serverMultiSigKey);
                            } else {
                                Log.d(TAG,response.code()+"");
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }, ECKeyWrapper.class);

                try {
                    kit.peerGroup().addAddress(Inet4Address.getByName("144.76.175.228"));
                    kit.peerGroup().addAddress(Inet4Address.getByName("88.198.20.152"));
                    kit.peerGroup().addAddress(Inet4Address.getByName("52.4.156.236"));
                    kit.peerGroup().addAddress(Inet4Address.getByName("176.9.24.110"));
                    kit.peerGroup().addAddress(Inet4Address.getByName("144.76.175.228"));
                } catch (IOException e) {

                }
            }
        };

        kit.setDownloadListener(new DownloadProgressTracker() {

            @Override
            public void onChainDownloadStarted(Peer peer, int blocksLeft) {
                super.onChainDownloadStarted(peer, blocksLeft);
                Log.d(TAG, "started download of block:" + blocksLeft);
            }

            @Override
            public void progress(double pct, int blocksSoFar, Date date) {
                super.progress(pct, blocksSoFar, date);
                Intent walletProgressIntent = new Intent(Constants.WALLET_PROGRESS_ACTION);
                walletProgressIntent.putExtra("progress", pct);
                walletProgressIntent.putExtra("blocksSoFar", blocksSoFar);
                walletProgressIntent.putExtra("date", date);
                LocalBroadcastManager.getInstance(WalletService.this).sendBroadcast(walletProgressIntent);
                Log.d(TAG, "progress " + pct);
            }

            @Override
            protected void doneDownload() {
                super.doneDownload();
                Log.d(TAG, "done download");
            }
        });

        try {

            kit.setCheckpoints(this.getAssets().open("checkpoints-testnet"));
        } catch (IOException e) {

        }

        kit.setBlockingStartup(false);
        kit.startAsync().awaitRunning();

        Log.d(TAG, "wallet started");
        return Service.START_NOT_STICKY;
    }

    private void setupMultiSigAddress(ECKey clientKey, ECKey serverKey) {
        this.multisigServerKey = serverKey;
        this.multisigClientKey = clientKey;
        this.multisigAddressScript = ScriptBuilder.createP2SHOutputScript(2, ImmutableList.of(clientKey, serverKey));
        kit.wallet().removeWatchedScripts(kit.wallet().getWatchedScripts());
        // now add the right one
        kit.wallet().addWatchedScripts(ImmutableList.of(multisigAddressScript));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.kit.stopAsync().awaitTerminated();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "on bind");
        return this.walletServiceBinder;
    }

    private void clearMultisig(){
        UuidObjectStorage.getInstance().deleteEntries(new ECKeyWrapperFilter(Constants.MULTISIG_CLIENT_KEY_NAME), DummyOnResultListener.getInstance(), ECKeyWrapper.class);
        UuidObjectStorage.getInstance().deleteEntries(new ECKeyWrapperFilter(Constants.MULTISIG_SERVER_KEY_NAME), DummyOnResultListener.getInstance(), ECKeyWrapper.class);
        UuidObjectStorage.getInstance().commit(DummyOnResultListener.getInstance());
    }
}
