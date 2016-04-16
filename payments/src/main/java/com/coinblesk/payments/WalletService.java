package com.coinblesk.payments;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.coinblesk.json.KeyTO;
import com.coinblesk.payments.communications.http.CoinbleskWebService;
import com.coinblesk.payments.communications.messages.DERObject;
import com.coinblesk.payments.communications.peers.steps.FullInstantPaymentStep;
import com.coinblesk.payments.communications.peers.steps.TopupPaymentStep;
import com.coinblesk.payments.models.ECKeyWrapper;
import com.coinblesk.payments.models.ExchangeRateWrapper;
import com.coinblesk.payments.models.TransactionWrapper;
import com.coinblesk.payments.models.filters.ECKeyWrapperFilter;
import com.coinblesk.util.BitcoinUtils;
import com.google.common.collect.ImmutableList;
import com.xeiam.xchange.Exchange;
import com.xeiam.xchange.ExchangeFactory;
import com.xeiam.xchange.bitstamp.BitstampExchange;
import com.xeiam.xchange.currency.CurrencyPair;
import com.xeiam.xchange.dto.marketdata.Ticker;

import org.bitcoinj.core.AbstractWalletEventListener;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.DownloadProgressTracker;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Peer;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.Wallet;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.script.Script;
import org.bitcoinj.store.WalletProtobufSerializer;
import org.bitcoinj.uri.BitcoinURI;
import org.bitcoinj.uri.BitcoinURIParseException;
import org.bitcoinj.utils.ExchangeRate;
import org.bitcoinj.utils.Fiat;
import org.bitcoinj.wallet.Protos;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogManager;

import ch.papers.objectstorage.UuidObjectStorage;
import ch.papers.objectstorage.UuidObjectStorageException;
import ch.papers.objectstorage.filters.MatchAllFilter;
import ch.papers.objectstorage.listeners.DummyOnResultListener;
import retrofit2.Response;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 14/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class WalletService extends Service {

    private final static String TAG = WalletService.class.getName();

    private final String REFUND_ADDRESS_SETTINGS_PREF_KEY = "pref_refund_address_settings";

    private String fiatCurrency = "USD";
    private ExchangeRate exchangeRate = new ExchangeRate(Fiat.parseFiat("CHF", "430"));
    private WalletAppKit kit;

    private Address refundAddress;

    private Script multisigAddressScript;
    private ECKey multisigClientKey;
    private ECKey multisigServerKey;
    private double progress = 0.0;

    public class WalletServiceBinder extends Binder {
        
        public WalletServiceBinder() {
            try {
                exchangeRate = UuidObjectStorage.getInstance().getFirstMatchEntry(new MatchAllFilter(), ExchangeRateWrapper.class).getExchangeRate();
            } catch (UuidObjectStorageException e) {
                Log.d(TAG, "could not retrieve old exchangerate from storage, staying with preshiped default");
            }
        }

        public ExchangeRate getExchangeRate() {
            return exchangeRate;
        }

        public Address getCurrentReceiveAddress() {
            return WalletService.this.kit.wallet().currentReceiveAddress();
        }

        public Address getMultisigReceiveAddress() {
            return multisigAddressScript.getToAddress(Constants.PARAMS);
        }

        public void setCurrency(String currency) {
            fiatCurrency = currency;
            this.fetchExchangeRate();
        }

        public Coin getBalance() {
            long coinAmount = 0;

            for (TransactionOutput transactionOutput : getUnspentInstantOutputs()) {
                coinAmount += transactionOutput.getValue().getValue();
            }

            return Coin.valueOf(coinAmount);
        }

        public Fiat getBalanceFiat() {
            return WalletService.this.exchangeRate.coinToFiat(getBalance());
        }

        public void commitAndBroadcastTransaction(Transaction tx) {
            kit.wallet().commitTx(tx);
            kit.peerGroup().broadcastTransaction(tx);
        }

        public List<TransactionWrapper> getTransactionsByTime() {
            final List<TransactionWrapper> transactions = new ArrayList<TransactionWrapper>();
            if (kit.wallet() != null) {
                for (Transaction transaction : WalletService.this.kit.wallet().getTransactionsByTime()) {
                    transaction.setExchangeRate(getExchangeRate());
                    transactions.add(new TransactionWrapper(transaction, WalletService.this.kit.wallet()));
                }
            }
            return transactions;
        }

        public List<TransactionOutput> getUnspentInstantOutputs() {
            List<TransactionOutput> unspentInstantOutputs = new ArrayList<TransactionOutput>();
            if (kit.wallet() != null) {
                for (TransactionOutput unspentTransactionOutput : kit.wallet().calculateAllSpendCandidates(false, false)) {
                    if (unspentTransactionOutput.getScriptPubKey().getToAddress(Constants.PARAMS).equals(this.getMultisigReceiveAddress())) {
                        unspentInstantOutputs.add(unspentTransactionOutput);
                    }
                }
            }
            return unspentInstantOutputs;
        }

        public void lockFundsForInstantPayment() {
            new TopupPaymentStep(walletServiceBinder).process(DERObject.NULLOBJECT);
        }

        public void sendCoins(final Address address, final Coin amount) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        new FullInstantPaymentStep(walletServiceBinder, new BitcoinURI(BitcoinURI.convertToBitcoinURI(address,amount,null,null))).process(DERObject.NULLOBJECT);
                    } catch (BitcoinURIParseException e) {
                        e.printStackTrace();
                    }
                }
            }, "WalletService.SendCoins").start();
        }

        public TransactionWrapper getTransaction(String transactionHash) {
            return new TransactionWrapper(WalletService.this.kit.wallet().getTransaction(Sha256Hash.wrap(transactionHash)), WalletService.this.kit.wallet());
        }

        public Address getRefundAddress() {
            return refundAddress;
        }

        public ECKey getMultisigClientKey() {
            return multisigClientKey;
        }

        public Script getMultisigAddressScript() {
            return multisigAddressScript;
        }

        public ECKey getMultisigServerKey() {
            return multisigServerKey;
        }

        public void fetchExchangeRate() {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Exchange bitstamp = ExchangeFactory.INSTANCE.createExchange(BitstampExchange.class.getName());
                        Ticker bitstampTicker = bitstamp.getPollingMarketDataService().getTicker(CurrencyPair.BTC_USD);
                        final CoinbleskWebService service = Constants.RETROFIT.create(CoinbleskWebService.class);

                        double conversionRate = 1.0;
                        if (fiatCurrency.equals("USD")) {

                        } else if (fiatCurrency.equals("CHF")) {
                            conversionRate = (1 / Double.parseDouble(service.usdToChf().execute().body().getExchangeRates().get("CHF")));
                        } else {
                            conversionRate = (1 / Double.parseDouble(service.usdToChf().execute().body().getExchangeRates().get("CHF")));
                            conversionRate *= Double.parseDouble(service.eurToChf().execute().body().getExchangeRates().get("CHF"));
                        }

                        Log.d(TAG, "conversion" + conversionRate);
                        WalletService.this.exchangeRate = new ExchangeRate(Fiat.valueOf(fiatCurrency, (long) (bitstampTicker.getAsk().longValue() * 10000 * (1 / conversionRate))));
                        Intent walletProgressIntent = new Intent(Constants.WALLET_BALANCE_CHANGED_ACTION);
                        walletProgressIntent.putExtra("balance", kit.wallet().getBalance().value);
                        LocalBroadcastManager.getInstance(WalletService.this).sendBroadcast(walletProgressIntent);
                        Intent exchangeRateChangeIntent = new Intent(Constants.EXCHANGE_RATE_CHANGED_ACTION);
                        LocalBroadcastManager.getInstance(WalletService.this).sendBroadcast(exchangeRateChangeIntent);

                        UuidObjectStorage.getInstance().deleteEntries(new MatchAllFilter(), ExchangeRateWrapper.class);
                        UuidObjectStorage.getInstance().addEntry(new ExchangeRateWrapper(exchangeRate), ExchangeRateWrapper.class);
                        UuidObjectStorage.getInstance().commit();
                    } catch (Exception e) {
                        Log.w(TAG, "Exception in fetchExchangeRate: ", e);
                    }
                }
            }, "WalletService.ExchangeRate").start();
        }

        public byte[] getSerializedWallet() {
            Protos.Wallet proto = new WalletProtobufSerializer().walletToProto(WalletService.this.kit.wallet());
            return proto.toByteArray();
        }

        public Wallet getWallet() {
            return WalletService.this.kit.wallet();
        }


        public void prepareWalletReset() {
            Log.i(TAG, "Wallet reset");
            WalletService.this.kit.wallet().reset();
            // delete chain file - this triggers re-downloading the chain (wallet replay) in the next start
            WalletService.this.kit.stopAsync().awaitTerminated();
            File blockstore = new File(getFilesDir(), Constants.WALLET_FILES_PREFIX + ".spvchain");
            if (blockstore.delete()) {
                Log.i(TAG, "Deleted blockchain file: " + blockstore.toString());
            }
        }

        public void deleteWalletFile() {
            File wallet = new File(getFilesDir(), Constants.WALLET_FILES_PREFIX + ".wallet");
            wallet.delete();
        }

        public boolean isReady() {
            return progress >= 100;
        }

        public double getProgress() {
            return progress;
        }

        public LocalBroadcastManager getLocalBroadcastManager(){
            return LocalBroadcastManager.getInstance(WalletService.this);
        }
    }

    private final WalletServiceBinder walletServiceBinder = new WalletServiceBinder();

    private void setupMultiSigAddress(ECKey clientKey, ECKey serverKey) {
        this.multisigServerKey = serverKey;
        this.multisigClientKey = clientKey;

        this.multisigAddressScript = BitcoinUtils.createP2SHOutputScript(2, ImmutableList.of(clientKey, serverKey));

        for (Script watchedScript : kit.wallet().getWatchedScripts()) {
            if (!watchedScript.getToAddress(Constants.PARAMS).equals(multisigAddressScript.getToAddress(Constants.PARAMS))) {
                kit.wallet().removeWatchedScripts(ImmutableList.<Script>of(watchedScript));
            }
        }

        // now add the right one
        kit.wallet().addWatchedScripts(ImmutableList.of(multisigAddressScript));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        if (this.kit != null) {
            this.kit.stopAsync().awaitTerminated();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "on bind");

        LogManager.getLogManager().getLogger("").setLevel(Level.SEVERE);
        Utils.fixECKeyComparator();

        this.kit = new WalletAppKit(Constants.PARAMS, this.getFilesDir(), Constants.WALLET_FILES_PREFIX) {

            @Override
            protected void onSetupCompleted() {
                if (wallet().getKeychainSize() < 1) {
                    ECKeyWrapper walletKey;
                    try {
                        walletKey = UuidObjectStorage.getInstance().getFirstMatchEntry(new ECKeyWrapperFilter(Constants.WALLET_KEY_NAME), ECKeyWrapper.class);
                    } catch (UuidObjectStorageException e) {
                        walletKey = new ECKeyWrapper(new ECKey().getPrivKeyBytes(), Constants.WALLET_KEY_NAME);
                        try {
                            UuidObjectStorage.getInstance().addEntry(walletKey, ECKeyWrapper.class);
                            UuidObjectStorage.getInstance().commit();
                        } catch (UuidObjectStorageException e1) {
                            Log.d(TAG, "couldn't store freshly generated ECKey: " + e.getMessage());
                        }
                    }
                    wallet().importKey(walletKey.getKey());

                }
                //walletServiceBinder.lockFundsForInstantPayment();
                kit.wallet().addEventListener(new AbstractWalletEventListener() {
                    @Override
                    public void onCoinsSent(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {
                        super.onCoinsSent(wallet, tx, prevBalance, newBalance);
                        Intent walletProgressIntent = new Intent(Constants.WALLET_BALANCE_CHANGED_ACTION);
                        walletProgressIntent.putExtra("balance", newBalance.value);
                        LocalBroadcastManager.getInstance(WalletService.this).sendBroadcast(walletProgressIntent);

                        Intent walletCoinsSentIntent = new Intent(Constants.WALLET_COINS_SENT_ACTION);
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
                        if (progress >= 100) {
                            Intent walletProgressIntent = new Intent(Constants.WALLET_TRANSACTIONS_CHANGED_ACTION);
                            walletProgressIntent.putExtra("transactionHash", tx.getHashAsString());
                            LocalBroadcastManager.getInstance(WalletService.this).sendBroadcast(walletProgressIntent);
                        }
                    }

                    @Override
                    public void onCoinsReceived(Wallet w, Transaction tx, Coin prevBalance, Coin newBalance) {
                        for(TransactionOutput transactionOutput : tx.getOutputs()) {
                            if(transactionOutput.isMine(w) && transactionOutput.isAvailableForSpending()) {
                                walletServiceBinder.lockFundsForInstantPayment();
                            }
                        }

                        Intent walletProgressIntent = new Intent(Constants.WALLET_BALANCE_CHANGED_ACTION);
                        walletProgressIntent.putExtra("balance", newBalance.value);
                        LocalBroadcastManager.getInstance(WalletService.this).sendBroadcast(walletProgressIntent);


                        Intent walletCoinsReceivedIntent = new Intent(Constants.WALLET_COINS_RECEIVED_ACTION);
                        walletCoinsReceivedIntent.putExtra("balance", newBalance.value);
                        LocalBroadcastManager.getInstance(WalletService.this).sendBroadcast(walletCoinsReceivedIntent);
                    }
                });

                try {
                    ECKeyWrapper serverKey = UuidObjectStorage.getInstance().getFirstMatchEntry(new ECKeyWrapperFilter(Constants.MULTISIG_SERVER_KEY_NAME), ECKeyWrapper.class);
                    ECKeyWrapper clientKey = UuidObjectStorage.getInstance().getFirstMatchEntry(new ECKeyWrapperFilter(Constants.MULTISIG_CLIENT_KEY_NAME), ECKeyWrapper.class);
                    setupMultiSigAddress(clientKey.getKey(), serverKey.getKey());


                } catch (UuidObjectStorageException e) {
                    try {
                        CoinbleskWebService service = Constants.RETROFIT.create(CoinbleskWebService.class);
                        final ECKey clientMultiSigKey = new ECKey();
                        final KeyTO clientKey = new KeyTO();
                        clientKey.publicKey(clientMultiSigKey.getPubKey());
                        Response<KeyTO> response = service.keyExchange(clientKey).execute();
                        if (response.isSuccess()) {
                            final KeyTO serverKey = response.body();
                            final ECKey serverMultiSigKey = ECKey.fromPublicOnly(serverKey.publicKey());
                            UuidObjectStorage.getInstance().addEntry(new ECKeyWrapper(clientMultiSigKey.getPrivKeyBytes(), Constants.MULTISIG_CLIENT_KEY_NAME), ECKeyWrapper.class);
                            UuidObjectStorage.getInstance().addEntry(new ECKeyWrapper(serverMultiSigKey.getPubKey(), Constants.MULTISIG_SERVER_KEY_NAME, true), ECKeyWrapper.class);
                            UuidObjectStorage.getInstance().commit();
                            setupMultiSigAddress(clientMultiSigKey, serverMultiSigKey);
                        } else {
                            Log.d(TAG, "error during key setup:" + response.code());
                        }
                    } catch (Exception e2) {
                        Log.d(TAG, "error while setting up multisig address:" + e2.getMessage());
                    }
                }

                final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                final String refundAddressString = sharedPreferences.getString(REFUND_ADDRESS_SETTINGS_PREF_KEY, kit.wallet().currentReceiveAddress().toString());
                try {
                    refundAddress = new Address(Constants.PARAMS, refundAddressString);
                } catch (AddressFormatException e) {
                    refundAddress = kit.wallet().currentReceiveAddress();
                }
                sharedPreferences.edit().putString(REFUND_ADDRESS_SETTINGS_PREF_KEY, refundAddress.toString()).commit();

                if (Constants.PARAMS.equals(TestNet3Params.get())) {
                    // these are testnet peers
                    try {
                        kit.peerGroup().addAddress(Inet4Address.getByName("144.76.175.228"));
                        kit.peerGroup().addAddress(Inet4Address.getByName("88.198.20.152"));
                        kit.peerGroup().addAddress(Inet4Address.getByName("52.4.156.236"));
                        kit.peerGroup().addAddress(Inet4Address.getByName("176.9.24.110"));
                        kit.peerGroup().addAddress(Inet4Address.getByName("144.76.175.228"));
                    } catch (IOException e) {
                        Log.i(TAG, "Exception while adding peers: ", e);
                    }
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
                progress = pct;
                Log.d(TAG, "progress " + pct);
            }

            @Override
            protected void doneDownload() {
                super.doneDownload();
                progress = 100.0;
                Intent walletProgressIntent = new Intent(Constants.WALLET_READY_ACTION);
                LocalBroadcastManager.getInstance(WalletService.this).sendBroadcast(walletProgressIntent);
                Log.d(TAG, "done download");
            }
        });


        if (Constants.PARAMS.equals(TestNet3Params.get())) {
            try {

                kit.setCheckpoints(this.getAssets().open("checkpoints-testnet"));
            } catch (IOException e) {
                Log.w(TAG, "Exception while setting checkpoints: ", e);
            }
        } else {
            try {
                kit.setCheckpoints(this.getAssets().open("checkpoints-mainnet"));
            } catch (IOException e) {
                Log.w(TAG, "Exception while setting checkpoints: ", e);
            }
        }

        kit.setBlockingStartup(false);
        kit.startAsync();

        //kit.wallet().reset();
        //clearMultisig();

        Log.d(TAG, "wallet started");
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)).setLevel(ch.qos.logback.classic.Level.OFF);


        return this.walletServiceBinder;
    }

    private void clearMultisig() {
        UuidObjectStorage.getInstance().deleteEntries(new ECKeyWrapperFilter(Constants.MULTISIG_CLIENT_KEY_NAME), DummyOnResultListener.getInstance(), ECKeyWrapper.class);
        UuidObjectStorage.getInstance().deleteEntries(new ECKeyWrapperFilter(Constants.MULTISIG_SERVER_KEY_NAME), DummyOnResultListener.getInstance(), ECKeyWrapper.class);
        UuidObjectStorage.getInstance().commit(DummyOnResultListener.getInstance());
    }
}
