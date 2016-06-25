/*
 * Copyright 2016 The Coinblesk team and the CSG Group at University of Zurich
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

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

import com.coinblesk.bitcoin.AddressCoinSelector;
import com.coinblesk.bitcoin.TimeLockedAddress;
import com.coinblesk.client.config.Constants;
import com.coinblesk.client.utils.ClientUtils;
import com.coinblesk.client.utils.SharedPrefUtils;
import com.coinblesk.json.BaseTO;
import com.coinblesk.json.KeyTO;
import com.coinblesk.json.TimeLockedAddressTO;
import com.coinblesk.payments.communications.http.CoinbleskWebService;
import com.coinblesk.payments.communications.steps.cltv.CLTVInstantPaymentStep;
import com.coinblesk.client.models.ECKeyWrapper;
import com.coinblesk.client.models.TimeLockedAddressWrapper;
import com.coinblesk.client.models.TransactionWrapper;
import com.coinblesk.client.models.filters.ECKeyWrapperFilter;
import com.coinblesk.util.BitcoinUtils;
import com.coinblesk.util.CoinbleskException;
import com.coinblesk.util.InsufficientFunds;
import com.coinblesk.util.SerializeUtils;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.BlockChain;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Context;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Peer;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionBroadcast;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.listeners.DownloadProgressTracker;
import org.bitcoinj.core.listeners.TransactionConfidenceEventListener;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.net.discovery.DnsDiscovery;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.script.Script;
import org.bitcoinj.store.BlockStore;
import org.bitcoinj.uri.BitcoinURI;
import org.bitcoinj.utils.ContextPropagatingThreadFactory;
import org.bitcoinj.utils.ExchangeRate;
import org.bitcoinj.utils.Fiat;
import org.bitcoinj.wallet.Protos;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.WalletProtobufSerializer;
import org.bitcoinj.wallet.listeners.ScriptsChangeEventListener;
import org.bitcoinj.wallet.listeners.WalletCoinsReceivedEventListener;
import org.bitcoinj.wallet.listeners.WalletCoinsSentEventListener;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.LogManager;

import ch.papers.objectstorage.UuidObjectStorage;
import ch.papers.objectstorage.UuidObjectStorageException;
import retrofit2.Response;

/**
 * @author Andreas Albrecht
 * @author Alessandro De Carli
 */
public class WalletService extends Service {
    private final static String TAG = WalletService.class.getName();

    private String fiatCurrency;
    private ExchangeRate exchangeRate;

    private double progress = 0.0;

    private WalletAppKit kit;
    private volatile org.bitcoinj.core.Context bitcoinjContext;
    private boolean appKitInitDone = false;
    private ScheduledExecutorService scheduledPeerGroupShutdown;
    private final ContextPropagatingThreadFactory bitcoinjThreadFactory;

    private Wallet wallet;
    private File walletFile;
    private final CoinbleskWalletEventListener walletEventListener;
    private PeerGroup peerGroup;
    private BlockChain blockChain;
    private BlockStore blockStore;


    private Address refundAddress;

    private ECKeyWrapper multisigClientKeyWrapper;
    private ECKey multisigClientKey;
    private ECKeyWrapper multisigServerKeyWrapper;
    private ECKey multisigServerKey;

    /* Addresses:
     * - map for fast lookup by hash, hex encoded (e.g. get redeem data)
     * - sorted set for fast traversal in time order (e.g. most recent address)
     */
    private final Map<String, TimeLockedAddressWrapper> addressHashes;
    private final SortedSet<TimeLockedAddressWrapper> addresses;

    private final WalletServiceBinder walletServiceBinder;
    private final UuidObjectStorage storage = UuidObjectStorage.getInstance();

    public WalletService() {
        walletServiceBinder = new WalletServiceBinder();
        walletEventListener = new CoinbleskWalletEventListener();
        bitcoinjThreadFactory = new ContextPropagatingThreadFactory("WalletServiceThreads");

        addressHashes = new ConcurrentHashMap<>();
        addresses = new ConcurrentSkipListSet<>(new TimeLockedAddressWrapper.TimeCreatedComparator(true));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        ClientUtils.fixECKeyComparator();
        initLogging();

        bitcoinjContext = new Context(Constants.PARAMS);
        Context.propagate(bitcoinjContext);

        initExchangeRate();

        walletFile = new File(getFilesDir(), Constants.WALLET_FILES_PREFIX + ".wallet");
        kit = new WalletAppKit(bitcoinjContext, getFilesDir(), Constants.WALLET_FILES_PREFIX) {
            @Override
            protected void onSetupCompleted() {
                final long startTime = System.currentTimeMillis();
                try {
                    wallet = kit.wallet();
                    peerGroup = kit.peerGroup();
                    blockChain = kit.chain();
                    blockStore = kit.store();

                    // broadcast current wallet balance (not using serviceBinder!)
                    Coin coinBalance = wallet.getBalance();
                    broadcastBalanceChanged(coinBalance, exchangeRate.coinToFiat(coinBalance));

                    addPeers();
                    initWalletEventListener();

                    /*
                     * Check that we already have keys.
                     * - if yes: check whether current address is still locked. if no: create new one.
                     * - if no: (1) create client key, (2) execute key-exchange and store server key (3) init address
                     */
                    boolean keysAlreadyExist = loadKeysIfExist();
                    if (!keysAlreadyExist) {
                        keyExchange();
                        loadKeysIfExist();
                    }
                    initAddresses();

                    appKitInitDone = true;
                    broadcastWalletServiceInitDone();
                    broadcastBalanceChanged();
                } catch (Exception e) {
                    String errorMessage = "Error during wallet initialization: " + e.getMessage();
                    Log.e(TAG, errorMessage, e);
                    broadcastWalletError(errorMessage);
                } finally {
                    long duration = System.currentTimeMillis() - startTime;
                    Log.d(TAG, "WalletService - onSetupCompleted finished in "+duration+" ms");
                }
            }
        };

        setChainCheckpoints();
        kit.setDownloadListener(new DownloadListener());
        kit.setBlockingStartup(false);
        kit.startAsync();

        Log.d(TAG, "Wallet started");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind");
        cancelScheduledPeerGroupShutdown();
        startPeerGroupIfNotRunning();
        return walletServiceBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        super.onUnbind(intent);
        Log.d(TAG, "onUnbind");
        // All clients have unbound with unbindService()
        // schedule stopping the peerGroup to save resources.
        schedulePeerGroupShutdown();
        // allow rebind?
        return true;
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        Log.d(TAG, "onRebind");
        cancelScheduledPeerGroupShutdown();
        startPeerGroupIfNotRunning();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy - shutdown wallet service");

        stopPeerGroup();
        closeBlockChain();
        shutdownWallet();

        kit = null;
        scheduledPeerGroupShutdown = null;
    }

    private void stopPeerGroup() {
        try {
            if (peerGroup != null && peerGroup.isRunning()) {
                if (wallet != null) {
                    peerGroup.removeWallet(wallet);
                }
                peerGroup.stop();
                Log.d(TAG, "Peer group stopped.");
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not stop peer group: ", e);
        } finally {
            peerGroup = null;
        }
    }

    private void closeBlockChain() {
        try {
            if (blockStore != null) {
                blockStore.close();
                Log.d(TAG, "BlockStore/BlockChain closed.");
            }
        } catch (Exception e) {
            Log.w(TAG, "Cannot close blockStore: ", e);
        } finally {
            blockStore = null;
            blockChain = null;
        }
    }

    private void shutdownWallet() {
        try {
            wallet.saveToFile(walletFile);
            Log.d(TAG, "Wallet saved to file: " + walletFile);
        } catch (Exception e) {
            Log.w(TAG, "Could not shutdown wallet: ", e);
        } finally {
            wallet = null;
        }
    }

    private void schedulePeerGroupShutdown() {
        if (!appKitInitDone) {
            return;
        }
        scheduledPeerGroupShutdown = Executors.newScheduledThreadPool(1);
        ScheduledFuture shutdown = scheduledPeerGroupShutdown.schedule(new Runnable() {
            @Override
            public void run() {
                stopPeerGroup();
            }
        }, 15, TimeUnit.SECONDS);
    }

    private void cancelScheduledPeerGroupShutdown() {
        if (!appKitInitDone) {
            return;
        }
        if (scheduledPeerGroupShutdown != null) {
            scheduledPeerGroupShutdown.shutdownNow();
            scheduledPeerGroupShutdown = null;
        }
    }

    private void startPeerGroupIfNotRunning() {
        if (!appKitInitDone) {
            return;
        }
        if (peerGroup == null || !peerGroup.isRunning()) {
            peerGroup = new PeerGroup(Constants.PARAMS, blockChain);
            DnsDiscovery discovery = new DnsDiscovery(Constants.PARAMS);
            peerGroup.addPeerDiscovery(discovery);
            peerGroup.addWallet(wallet);
            Futures.addCallback(peerGroup.startAsync(), new FutureCallback() {
                @Override
                public void onSuccess(Object result) {
                    Log.i(TAG, "Peer group started.");
                    final DownloadProgressTracker l = new DownloadListener();
                    peerGroup.startBlockChainDownload(l);
                }

                @Override
                public void onFailure(Throwable t) {
                    Log.e(TAG, "Could not start downloading blockchain.");
                    throw new RuntimeException(t);
                }
            });
        }
    }

    private void initLogging() {
        LogManager.getLogManager().getLogger("").setLevel(Constants.JAVA_LOGGER_LEVEL);
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME))
                .setLevel(Constants.LOGBACK_LOGGER_LEVEL);
    }

    private void initWalletEventListener() {
        wallet.addCoinsReceivedEventListener(walletEventListener);
        wallet.addCoinsSentEventListener(walletEventListener);
        wallet.addTransactionConfidenceEventListener(walletEventListener);
        wallet.addScriptsChangeEventListener(walletEventListener);
    }

    private void setChainCheckpoints() {
        try {
            if (Constants.PARAMS.equals(TestNet3Params.get())) {
                kit.setCheckpoints(getAssets().open("checkpoints-testnet"));
            } else if (Constants.PARAMS.equals(MainNetParams.get())) {
                kit.setCheckpoints(getAssets().open("checkpoints-mainnet"));
            }
        } catch (IOException e) {
            Log.w(TAG, "Exception while setting checkpoints: ", e);
        }
    }

    private void addPeers() {
        String[] peers = {};
        if (Constants.PARAMS.equals(TestNet3Params.get())) {
            // testnet peers
            peers = new String[] {
                    "144.76.175.228",
                    "88.198.20.152",
                    "52.4.156.236",
                    "176.9.24.110",
                    "144.76.175.228"
            };
        } else if (Constants.PARAMS.equals(MainNetParams.get())) {
            // no mainnet peers at the moment
        }

        for (String ip : peers) {
            try {
                peerGroup.addAddress(Inet4Address.getByName(ip));
            } catch (IOException e) {
                Log.i(TAG, "Exception while adding peer with ip: " + ip, e);
            }
        }
    }


    private boolean loadKeysIfExist() {
        try {
            ECKeyWrapper clientKeyWrapper = storage.getFirstMatchEntry(
                    new ECKeyWrapperFilter(Constants.MULTISIG_CLIENT_KEY_NAME), ECKeyWrapper.class);
            ECKeyWrapper serverKeyWrapper = storage.getFirstMatchEntry(
                    new ECKeyWrapperFilter(Constants.MULTISIG_SERVER_KEY_NAME), ECKeyWrapper.class);

            multisigClientKeyWrapper = clientKeyWrapper;
            multisigClientKey = clientKeyWrapper.getKey();
            multisigServerKeyWrapper = serverKeyWrapper;
            multisigServerKey = serverKeyWrapper.getKey();
            Log.d(TAG, "Loaded client and server key from storage.");
            return true;
        } catch (UuidObjectStorageException e) {
            // no keys found yet --> do a key exchange.
            Log.d(TAG, "loadKeys - UuidObjectStorageException: " + e.getMessage());
        }
        return false;
    }

    private void keyExchange() throws CoinbleskException {
        final CoinbleskWebService service = coinbleskService();
        final ECKey clientECKey = new ECKey();
        final ECKey serverECKey;

        final KeyTO requestTO = new KeyTO();
        requestTO.publicKey(clientECKey.getPubKey());
        Response<KeyTO> response = null;
        try {
            response = service.keyExchange(requestTO).execute();
        } catch (IOException e) {
            throw new CoinbleskException("Could not connect to server: " + e.getMessage(), e);
        }

        if (response != null && response.body().isSuccess()) {
            final KeyTO responseTO = response.body();
            serverECKey = ECKey.fromPublicOnly(responseTO.publicKey());
            if (!SerializeUtils.verifyJSONSignature(responseTO, serverECKey)) {
                throw new CoinbleskException("Verification of server response failed.");
            }

            // store everything
            ECKeyWrapper clientKeyWrapper = new ECKeyWrapper(
                    clientECKey.getPrivKeyBytes(),
                    Constants.MULTISIG_CLIENT_KEY_NAME);
            ECKeyWrapper serverKeyWrapper = new ECKeyWrapper(
                    serverECKey.getPubKey(),
                    Constants.MULTISIG_SERVER_KEY_NAME,
                    true);
            try {
                storage.addEntry(clientKeyWrapper, ECKeyWrapper.class);
                storage.addEntry(serverKeyWrapper, ECKeyWrapper.class);
                storage.commit();
            } catch (UuidObjectStorageException e) {
                throw new CoinbleskException("Could not store keys.", e);
            }
            Log.i(TAG, "Key exchange with server completed.");
        } else {
            Log.e(TAG, "Error during key setup - code: " + response.code());
            throw new CoinbleskException("Could not execute key-exchange, code: " + response.code());
        }
    }

    private void initAddresses() throws CoinbleskException {
        loadAddresses();
        try {
            // new address: no address yet or current receive address expires soon.
            boolean needToCreateNewAddress = false;
            if (addresses.isEmpty()) {
                Log.d(TAG, "No address yet. Create new address.");
                needToCreateNewAddress = true;
            } else {
                long nowSec = org.bitcoinj.core.Utils.currentTimeSeconds();
                long currentExpiresInSec = addresses.last().getTimeLockedAddress().getLockTime() - nowSec;
                if (currentExpiresInSec < Constants.MIN_LOCKTIME_SPAN_SECONDS) {
                    Log.d(TAG, "Current address expires soon (in "+currentExpiresInSec+" seconds). Create new address.");
                    needToCreateNewAddress = true;
                }
            }

            if (needToCreateNewAddress) {
                createTimeLockedAddress();
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not initialize addresses. ", e);
            throw new CoinbleskException("Could not initialize addresses: " + e.getMessage(), e);
        }
    }

    private void loadAddresses() {
        addresses.clear();
        addressHashes.clear();
        try {
            List<TimeLockedAddressWrapper> storedAddresses = storage
                    .getEntriesAsList(TimeLockedAddressWrapper.class);
            addAddresses(storedAddresses);
        } catch (UuidObjectStorageException e) {
            Log.e(TAG, "Could not load addresses (probably no addresses exist yet): ", e);
            addresses.clear();
            addressHashes.clear();
        }
    }

    private void addAddresses(List<TimeLockedAddressWrapper> addressesToAdd) {
        StringBuilder addressLog = new StringBuilder();
        // we could add each single address - however, it is very slow (up to 30s)...
        // As an optimization, we first prepare the lists and then use addAll methods.
        // this avoids synchronization (locks) for each address
        int numAddresses = addressesToAdd.size();
        List<Script> addressScripts = new ArrayList<>(numAddresses);
        Map<String, TimeLockedAddressWrapper> addressMap = new HashMap<>(numAddresses);
        for (TimeLockedAddressWrapper addressWrapper : addressesToAdd) {
            TimeLockedAddress tla = addressWrapper.getTimeLockedAddress();
            addressMap.put(
                    org.bitcoinj.core.Utils.HEX.encode(tla.getAddressHash()),
                    addressWrapper);

            Script pubKeyScript = tla.createPubkeyScript();
            // timestamp optimizes wallet replay!
            pubKeyScript.setCreationTimeSeconds(addressWrapper.getTimeCreatedSeconds());
            addressScripts.add(pubKeyScript);

            addressLog.append("  ")
                    .append(tla.toString(Constants.PARAMS))
                    .append("\n");
        }
        // now add to wallet, address lists and hash index
        addresses.addAll(addressesToAdd);
        addressHashes.putAll(addressMap);
        wallet.addWatchedScripts(addressScripts);
        Log.d(TAG, "Added addresses (total "+numAddresses+"):\n" + addressLog.toString());
    }

    private void addAddress(TimeLockedAddressWrapper address) {
        // Note: do not use in loop, adding to wallet is slow!
        addresses.add(address);
        addressHashes.put(
                org.bitcoinj.core.Utils.HEX.encode(address.getTimeLockedAddress().getAddressHash()),
                address);
        Script pubKeyScript = address.getTimeLockedAddress().createPubkeyScript();
        pubKeyScript.setCreationTimeSeconds(address.getTimeCreatedSeconds());
        wallet.addWatchedScripts(ImmutableList.of(pubKeyScript));
        Log.d(TAG, "Added address:\n" + address.getTimeLockedAddress().toString(Constants.PARAMS));
    }

    private TimeLockedAddressWrapper createTimeLockedAddress() throws CoinbleskException, IOException {
        if (multisigClientKey == null || multisigServerKey == null) {
            throw new IllegalStateException("No client or server multisig key, key-exchange should be done first.");
        }

        CoinbleskWebService service = coinbleskService();
        final TimeLockedAddressTO request = new TimeLockedAddressTO();
        request.currentDate(System.currentTimeMillis());
        request.publicKey(multisigClientKey.getPubKey());
        request.lockTime(calculateNextLockTime());
        SerializeUtils.signJSON(request, multisigClientKey);

        Log.d(TAG, "Request new address from server: lockTime=" +
                org.bitcoinj.core.Utils.dateTimeFormat(request.lockTime()*1000L));
        Response<TimeLockedAddressTO> response = service
                .createTimeLockedAddress(request)
                .execute();

        if (!response.isSuccessful()) {
            throw new CoinbleskException("Could not create new address. Code: " + response.code());
        }

        final TimeLockedAddressTO responseTO = response.body();
        if (!responseTO.isSuccess()) {
            throw new CoinbleskException("Could not create new address: Error: " + responseTO.type().toString());
        }

        final TimeLockedAddress lockedAddress = responseTO.timeLockedAddress();
        if (lockedAddress == null) {
            throw new CoinbleskException("Could not create new address (server response empty)");
        }

        final ECKey serverPublicKey = ECKey.fromPublicOnly(lockedAddress.getServerPubKey());
        if (!checkResponse(responseTO, serverPublicKey)) {
            throw new CoinbleskException("Could not create new address. Type: " + responseTO.type());
        }

        if (!checkTimeLockedAddress(lockedAddress, multisigClientKey.getPubKey())) {
            throw new CoinbleskException("Could not create new address, address check failed.");
        }

        TimeLockedAddressWrapper lockedAddressWrapper = TimeLockedAddressWrapper.create(
                lockedAddress,
                multisigClientKeyWrapper,
                multisigServerKeyWrapper);
        try {
            storage.addEntry(lockedAddressWrapper, TimeLockedAddressWrapper.class);
            storage.commit();
        } catch (Exception e) {
            throw new CoinbleskException("Could not store address.", e);
        }

        addAddress(lockedAddressWrapper);
        Log.d(TAG, "Created new time locked address: " + lockedAddress.toStringDetailed(Constants.PARAMS));
        return lockedAddressWrapper;
    }

    private long calculateNextLockTime() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        int lockTimeMonths = Integer.parseInt(preferences.getString("pref_wallet_locktime_period", "3"));
        Calendar lockTimeCalendar = Calendar.getInstance();
        lockTimeCalendar.add(Calendar.MONTH, lockTimeMonths);
        long lockTime = lockTimeCalendar.getTimeInMillis()/1000L;
        return lockTime;
    }

    private boolean checkTimeLockedAddress(TimeLockedAddress receivedAddress, byte[] expectedClientPubKey) {
        if (receivedAddress == null) {
            return false;
        }

        final byte[] addressHash = receivedAddress.getAddressHash();
        if (addressHash == null || addressHash.length <= 0 || !receivedAddress.getAddress(Constants.PARAMS).isP2SHAddress()) {
            return false;
        }

        final byte[] clientPubKey = receivedAddress.getClientPubKey();
        if (clientPubKey == null || clientPubKey.length <= 0 || !Arrays.equals(expectedClientPubKey, clientPubKey)) {
            return false;
        }

        final byte[] serverPubKey = receivedAddress.getServerPubKey();
        if (serverPubKey == null || serverPubKey.length <= 0 || !ECKey.isPubKeyCanonical(serverPubKey)) {
            return false;
        }

        final long lockTime = receivedAddress.getLockTime();
        final long now = org.bitcoinj.core.Utils.currentTimeSeconds();
        if (lockTime <=  now || lockTime >= now + Constants.MAX_LOCKTIME_SPAN_SECONDS) {
            return false;
        }

        TimeLockedAddress address = new TimeLockedAddress(clientPubKey, serverPubKey, lockTime);
        if (!address.equals(receivedAddress)) {
            return false;
        }

        // all checks ok
        return true;
    }

    private boolean checkResponse(BaseTO baseTO, ECKey signKey) {
        if (baseTO == null) {
            return false;
        }
        if (!baseTO.isSuccess()) {
            return false;
        }
        if(baseTO.messageSig() == null || !SerializeUtils.verifyJSONSignature(baseTO, signKey)) {
            return false;
        }
        return true;
    }

    private void initExchangeRate() {
        if (fiatCurrency == null) {
            fiatCurrency = SharedPrefUtils.getCurrency(this);
        }
        loadExchangeRateFromStorage();
        Log.d(TAG, "init exchange rate - currency: " + fiatCurrency
                + ", exchangeRate: 1 Coin = " + exchangeRate.coinToFiat(Coin.COIN).toFriendlyString());

        fetchExchangeRate();
    }

    private void loadExchangeRateFromStorage() {
        exchangeRate = SharedPrefUtils.getExchangeRate(this, fiatCurrency);
    }

    private void fetchExchangeRate() {
        Thread t = bitcoinjThreadFactory.newThread(new ExchangeRateFetcher(fiatCurrency, walletServiceBinder));
        t.setName("WalletService.ExchangeRateFetcher");
        t.start();
    }

    private void setExchangeRate(ExchangeRate exchangeRate) {
        if (!fiatCurrency.equals(exchangeRate.fiat.getCurrencyCode())) {
            throw new IllegalArgumentException(String.format(Locale.US,
                    "Exchange rate currency code (%s) does not match current currencyCode (%s)",
                    exchangeRate.fiat.getCurrencyCode(), fiatCurrency));
        }

        this.exchangeRate = exchangeRate;
        saveExchangeRate();
        broadcastBalanceChanged();
        broadcastExchangeRateChanged();
    }

    private void saveExchangeRate() {
        SharedPrefUtils.setExchangeRate(this, exchangeRate.fiat);
    }

    private List<TransactionOutput> getUnlockedUnspentOutputs() {
        final List<TransactionOutput> outputs = new ArrayList<>();
        final List<TransactionOutput> candidates = wallet.calculateAllSpendCandidates(false, false);
        final long currentTimeSec = org.bitcoinj.core.Utils.currentTimeSeconds();
        for (TransactionOutput txOut : candidates) {
            byte[] addressHash = txOut.getScriptPubKey().getPubKeyHash();
            TimeLockedAddressWrapper tla = findTimeLockedAddressByHash(addressHash);
            if (tla != null) {
                long lockTime = tla.getTimeLockedAddress().getLockTime();
                if (BitcoinUtils.isAfterLockTime(currentTimeSec, lockTime)) {
                    outputs.add(txOut);
                    Log.d(TAG, "getUnlockedUnspentOutputs - unlocked output: " + txOut);
                }
            }
        }
        return outputs;
    }

    private Map<String, Long> createLockTimeForInputsMap(List<TransactionInput> inputs) {
        Map<String, Long> timeLocksOfInputs = new HashMap<>(inputs.size());
        for (TransactionInput txIn : inputs) {
            byte[] pubKeyHash = txIn.getConnectedOutput().getScriptPubKey().getPubKeyHash();
            String addressHashHex = org.bitcoinj.core.Utils.HEX.encode(pubKeyHash);
            TimeLockedAddressWrapper txInAddress = addressHashes.get(addressHashHex);
            if (txInAddress != null) {
                long lockTime = txInAddress.getTimeLockedAddress().getLockTime();
                timeLocksOfInputs.put(addressHashHex, lockTime);
            }
        }
        return timeLocksOfInputs;
    }

    private TimeLockedAddressWrapper findTimeLockedAddressByHash(byte[] addressHash) {
        String addressHashHex = org.bitcoinj.core.Utils.HEX.encode(addressHash);
        TimeLockedAddressWrapper address = addressHashes.get(addressHashHex);
        return address;
    }

    private Transaction createTransaction(Address addressTo, Coin amount) throws InsufficientFunds, CoinbleskException {
        Address changeAddress = walletServiceBinder.getCurrentReceiveAddress();
        List<TransactionOutput> outputs = walletServiceBinder.getUnspentInstantOutputs();
        Transaction transaction = BitcoinUtils.createTx(
                Constants.PARAMS,
                outputs,
                changeAddress,
                addressTo,
                amount.longValue());
        return transaction;
    }

    private List<TransactionSignature> signTransaction(Transaction tx) throws CoinbleskException {
        final List<TransactionInput> inputs = tx.getInputs();
        final List<TransactionSignature> signatures = new ArrayList<>(inputs.size());
        for (int i = 0; i < inputs.size(); ++i) {
            TransactionInput txIn = inputs.get(i);
            TransactionOutput prevTxOut = txIn.getConnectedOutput();
            byte[] sentToHash = prevTxOut.getScriptPubKey().getPubKeyHash();
            TimeLockedAddressWrapper redeemData = findTimeLockedAddressByHash(sentToHash);
            if (redeemData == null) {
                throw new CoinbleskException(String.format(
                        "Could not sign input (index=%d, pubKeyHash=%s)",
                        i, org.bitcoinj.core.Utils.HEX.encode(sentToHash)));
            }
            TimeLockedAddress address = redeemData.getTimeLockedAddress();
            ECKey signKey = redeemData.getClientKey().getKey();
            byte[] redeemScript = address.createRedeemScript().getProgram();
            TransactionSignature signature = tx.calculateSignature(
                    i, signKey, redeemScript, Transaction.SigHash.ALL, false);
            signatures.add(signature);
        }
        return signatures;
    }

    private CoinbleskWebService coinbleskService() {
        return Constants.RETROFIT.create(CoinbleskWebService.class);
    }

    private void broadcastWalletServiceInitDone() {
        Intent initDone = new Intent(Constants.WALLET_INIT_DONE_ACTION);
        getLocalBroadcaster().sendBroadcast(initDone);
    }

    private void broadcastWalletError(String errorMessage) {
        Intent error = new Intent(Constants.WALLET_ERROR_ACTION);
        error.putExtra(Constants.ERROR_MESSAGE_KEY, errorMessage);
        getLocalBroadcaster().sendBroadcast(error);
    }

    private void broadcastBalanceChanged() {
        Coin coinBalance = walletServiceBinder.getBalance();
        broadcastBalanceChanged(coinBalance);
    }

    private void broadcastBalanceChanged(Coin coinBalance) {
        Fiat fiatBalance = exchangeRate.coinToFiat(coinBalance);
        broadcastBalanceChanged(coinBalance, fiatBalance);
    }

    private void broadcastBalanceChanged(Coin coinBalance, Fiat fiatBalance) {
        Intent balanceChanged = new Intent(Constants.WALLET_BALANCE_CHANGED_ACTION);
        balanceChanged.putExtra("coinBalance", coinBalance);
        balanceChanged.putExtra("fiatBalance", fiatBalance);
        getLocalBroadcaster().sendBroadcast(balanceChanged);
    }

    private void broadcastCoinsSent() {
        Intent coinsSent = new Intent(Constants.WALLET_COINS_SENT_ACTION);
        getLocalBroadcaster().sendBroadcast(coinsSent);
    }

    private void broadcastCoinsReceived() {
        Intent coinsReceived = new Intent(Constants.WALLET_COINS_RECEIVED_ACTION);
        getLocalBroadcaster().sendBroadcast(coinsReceived);
    }

    private void broadcastScriptsChanged() {
        Intent scriptsChanged = new Intent(Constants.WALLET_SCRIPTS_CHANGED_ACTION);
        getLocalBroadcaster().sendBroadcast(scriptsChanged);
    }

    private void broadcastConfidenceChanged(final String txHash) {
        Intent txChanged = new Intent(Constants.WALLET_TRANSACTIONS_CHANGED_ACTION);
        txChanged.putExtra("transactionHash", txHash);
        getLocalBroadcaster().sendBroadcast(txChanged);
    }

    private void broadcastExchangeRateChanged() {
        Intent exchangeRateChanged = new Intent(Constants.EXCHANGE_RATE_CHANGED_ACTION);
        getLocalBroadcaster().sendBroadcast(exchangeRateChanged);
    }

    private void broadcastInstantPaymentSuccess() {
        Intent paymentSuccess = new Intent(Constants.INSTANT_PAYMENT_SUCCESSFUL_ACTION);
        getLocalBroadcaster().sendBroadcast(paymentSuccess);
    }

    private void broadcastInstantPaymentFailure() {
        Intent paymentFailure = new Intent(Constants.INSTANT_PAYMENT_FAILED_ACTION);
        LocalBroadcastManager.getInstance(WalletService.this).sendBroadcast(paymentFailure);
    }

    private LocalBroadcastManager getLocalBroadcaster() {
        return LocalBroadcastManager.getInstance(WalletService.this);
    }

    public class WalletServiceBinder extends Binder {

        public Coin getBalance() {
            Coin amount = Coin.ZERO;
            for (TransactionOutput transactionOutput : getUnspentInstantOutputs()) {
                amount = amount.add(transactionOutput.getValue());
            }
            return amount;
        }

        public Fiat getBalanceFiat() {
            return exchangeRate.coinToFiat(getBalance());
        }

        public Coin getBalanceUnlocked() {
            List<TransactionOutput> unlockedTxOut = getUnlockedUnspentOutputs();
            Coin balance = Coin.ZERO;
            for (TransactionOutput txOut : unlockedTxOut) {
                balance = balance.add(txOut.getValue());
            }
            return balance;
        }

        /* given the wallet balance, calculates the max spendable amount (i.e. balance minus fee estimate) */
        public Coin getMaxSpendableAmount() {
            Coin maxSpendableAmount;
            try {
                Transaction dummyTx = BitcoinUtils.createSpendAllTx(
                        Constants.PARAMS,
                        getUnspentInstantOutputs(),
                        getCurrentReceiveAddress(),
                        getCurrentReceiveAddress());
                maxSpendableAmount = dummyTx.getOutputSum();
            } catch (CoinbleskException | InsufficientFunds e) {
                // ignore since not interested in Tx anyways.
                maxSpendableAmount = getBalance();
            }

            if (maxSpendableAmount.isNegative()) {
                maxSpendableAmount = Coin.ZERO;
            }

            return maxSpendableAmount;
        }

        public ExchangeRate getExchangeRate() {
            return exchangeRate;
        }

        protected void setExchangeRate(ExchangeRate exchangeRate) {
            WalletService.this.setExchangeRate(exchangeRate);
        }

        public String getCurrency() {
            return fiatCurrency;
        }

        public void setCurrency(String currency) {
            fiatCurrency = currency;
            fetchExchangeRate();
        }

        public Address getCurrentReceiveAddress() {
            if (addresses.isEmpty()) {
                throw new IllegalStateException("No address created yet.");
            }
            return addresses.last().getTimeLockedAddress().getAddress(Constants.PARAMS);
        }

        public Address getRefundAddress() {
            return refundAddress;
        }

        public Collection<TimeLockedAddressWrapper> getAddresses() {
            return addresses;
        }

        public Map<Address, Coin> getBalanceByAddress() {
            AddressCoinSelector selector = new AddressCoinSelector(null, Constants.PARAMS);
            selector.select(Coin.ZERO, getUnspentInstantOutputs());
            return selector.getAddressBalances();
        }

        public TimeLockedAddressWrapper createTimeLockedAddress() throws CoinbleskException, IOException {
            return WalletService.this.createTimeLockedAddress();
        }

        public TransactionWrapper getTransaction(final String transactionHash) {
            Transaction tx = wallet.getTransaction(Sha256Hash.wrap(transactionHash));
            tx.setExchangeRate(getExchangeRate());
            return new TransactionWrapper(tx, wallet);
        }

        public List<TransactionWrapper> getTransactionsByTime() {
            final List<TransactionWrapper> transactions = new ArrayList<TransactionWrapper>();
            if (wallet != null) {
                for (Transaction transaction : wallet.getTransactionsByTime()) {
                    transaction.setExchangeRate(getExchangeRate());
                    transactions.add(new TransactionWrapper(transaction, wallet));
                }
            }
            return transactions;
        }

        public ListenableFuture<Transaction> commitAndBroadcastTransaction(final Transaction tx) {
            Log.d(TAG, "commitAndBroadcastTransaction: " + tx.getHashAsString());
            wallet.commitTx(tx);
            return broadcastTransaction(tx);
        }

        public ListenableFuture<Transaction> maybeCommitAndBroadcastTransaction(final Transaction tx) {
            Log.d(TAG, "maybeCommitAndBroadcastTransaction: " + tx.getHashAsString());
            if (!wallet.maybeCommitTx(tx)) {
                Log.d(TAG, "Tx was already committed to wallet (probably received over network)");
            };
            return broadcastTransaction(tx);
        }

        public ListenableFuture<Transaction> broadcastTransaction(final Transaction tx) {
            TransactionBroadcast broadcast = peerGroup.broadcastTransaction(tx);
            broadcast.setProgressCallback(new TransactionBroadcast.ProgressCallback() {
                final String txHash = tx.getHashAsString();
                @Override
                public void onBroadcastProgress(double progress) {
                    Log.d(TAG, "Transaction broadcast - tx: "+txHash+", progress: " + progress);
                }
            });
            return broadcast.future();
        }

        public Transaction createTransaction(Address addressTo, Coin amount) throws InsufficientFunds, CoinbleskException {
            return WalletService.this.createTransaction(addressTo, amount);
        }

        public List<TransactionSignature> signTransaction(Transaction tx) throws CoinbleskException {
            return WalletService.this.signTransaction(tx);
        }

        public ListenableFuture<Transaction> sendCoins(final Address address, final Coin amount) {
            ExecutorService executor = Executors.newSingleThreadExecutor(bitcoinjThreadFactory);
            ListeningExecutorService sendCoinsExecutor = MoreExecutors.listeningDecorator(executor);
            ListenableFuture<Transaction> txFuture = sendCoinsExecutor.submit(new Callable<Transaction>() {
                @Override
                public Transaction call() throws Exception {
                    BitcoinURI payment = new BitcoinURI(BitcoinURI.convertToBitcoinURI(address, amount, null, null));
                    CLTVInstantPaymentStep step = new CLTVInstantPaymentStep(walletServiceBinder, payment);
                    step.process(null);
                    Transaction fullySignedTx = step.getTransaction();
                    commitAndBroadcastTransaction(fullySignedTx);
                    Log.i(TAG, "Send Coins - address=" + address + ", amount=" + amount
                            + ", txHash=" + fullySignedTx.getHashAsString());
                    return fullySignedTx;
                }
            });
            sendCoinsExecutor.shutdown();
            Futures.addCallback(txFuture, new FutureCallback<Transaction>() {
                @Override
                public void onSuccess(Transaction result) {
                    Log.i(TAG, "sendCoins - onSuccess - tx " + result.getHashAsString());
                    broadcastInstantPaymentSuccess();
                }

                @Override
                public void onFailure(Throwable t) {
                    Log.e(TAG, "sendCoins - onFailure - failed with the following throwable: ", t);
                    broadcastInstantPaymentFailure();
                }
            });
            return txFuture;
        }

        public ListenableFuture<Transaction> collectRefund(final Address sendTo) {
            /*
             * Bitcoin nodes consider the median time of the last couple of blocks when
             * comparing the nLockTime (BIP 113). The median time is behind the current
             * time (unix seconds). As a consequence, a transaction is not relayed
             * by the nodes even though the lock time expired, i.e. lockTime < currentTime.
             * The median lags behind ~1h. In other words, a transaction with lock time t should be
             * broadcasted not earlier than t+1h. Otherwise, the transaction must be re-broadcasted
             * and it takes a long time for the transaction to be included in a block.
             * - https://bitcoincore.org/en/releases/0.12.1/
             * - https://github.com/bitcoin/bips/blob/master/bip-0113.mediawiki
             */

            ExecutorService executor = Executors.newSingleThreadExecutor(bitcoinjThreadFactory);
            ListeningExecutorService collectRefundExecutor = MoreExecutors.listeningDecorator(executor);
            ListenableFuture<Transaction> txFuture = collectRefundExecutor.submit(new Callable<Transaction>() {
                @Override
                public Transaction call() throws Exception {
                    final List<TransactionOutput> unlockedTxOut = getUnlockedUnspentOutputs();
                    Transaction transaction = BitcoinUtils.createSpendAllTx(
                            Constants.PARAMS,
                            unlockedTxOut,
                            getCurrentReceiveAddress(),
                            sendTo);

                    // since we sign with 1 key (without server key), we need to set
                    // the nLockTime and sequence number flags of CLTV inputs.
                    Map<String, Long> timeLocksOfInputs = createLockTimeForInputsMap(transaction.getInputs());
                    BitcoinUtils.setFlagsOfCLTVInputs(
                            transaction,
                            timeLocksOfInputs,
                            org.bitcoinj.core.Utils.currentTimeSeconds());

                    // code is veriy similar to signTransaction, but we use a different scriptSig!
                    final List<TransactionInput> inputs = transaction.getInputs();
                    for (int i = 0; i < inputs.size(); ++i) {
                        TransactionInput txIn = inputs.get(i);
                        TransactionOutput prevTxOut = txIn.getConnectedOutput();
                        byte[] sentToHash = prevTxOut.getScriptPubKey().getPubKeyHash();
                        TimeLockedAddressWrapper redeemData = findTimeLockedAddressByHash(sentToHash);
                        if (redeemData == null) {
                            throw new CoinbleskException(String.format(Locale.US,
                                    "Signing error: did not find redeem script for input: %s, ", txIn));
                        }

                        ECKey signKey = redeemData.getClientKey().getKey();
                        TimeLockedAddress tla = redeemData.getTimeLockedAddress();
                        byte[] redeemScript = tla.createRedeemScript().getProgram();
                        TransactionSignature signature = transaction.calculateSignature(
                                i, signKey, redeemScript, Transaction.SigHash.ALL, false);

                        Script scriptSig = tla.createScriptSigAfterLockTime(signature);
                        txIn.setScriptSig(scriptSig);
                    }

                    BitcoinUtils.verifyTxFull(transaction);
                    commitAndBroadcastTransaction(transaction);
                    Coin amount = transaction.getOutputSum();
                    Log.i(TAG, "Collect Refund - address=" + sendTo + ", amount=" + amount
                            + ", txHash=" + transaction.getHashAsString());
                    return transaction;
                }
            });
            collectRefundExecutor.shutdown();
            Futures.addCallback(txFuture, new FutureCallback<Transaction>() {
                @Override
                public void onSuccess(Transaction result) {
                    Log.i(TAG, "collectRefund - onSuccess - tx " + result.getHashAsString());
                    broadcastInstantPaymentSuccess();
                }

                @Override
                public void onFailure(Throwable t) {
                    Log.e(TAG, "collectRefund - onFailure - failed with the following throwable: ", t);
                    broadcastInstantPaymentFailure();
                }
            });
            return txFuture;
        }

        public List<TransactionOutput> getUnspentInstantOutputs() {
            List<TransactionOutput> unspentInstantOutputs = new ArrayList<TransactionOutput>();
            if (wallet != null) {
                List<TransactionOutput> candidates = wallet.calculateAllSpendCandidates(false, false);
                for (TransactionOutput unspentOutput : candidates) {
                    byte[] addressHash = unspentOutput.getScriptPubKey().getPubKeyHash();
                    String addressHashHex = org.bitcoinj.core.Utils.HEX.encode(addressHash);
                    if (addressHashes.keySet().contains(addressHashHex)) {
                        unspentInstantOutputs.add(unspentOutput);
                    }
                }
            }
            return unspentInstantOutputs;
        }

        public TimeLockedAddressWrapper findTimeLockedAddressByHash(byte[] addressHash) {
            return WalletService.this.findTimeLockedAddressByHash(addressHash);
        }

        public void lockFundsForInstantPayment() {
            //new TopupPaymentStep(walletServiceBinder).process(DERObject.NULLOBJECT);
        }

        public ECKey getMultisigClientKey() {
            return multisigClientKey;
        }

        public ECKey getMultisigServerKey() {
            return multisigServerKey;
        }

        public Wallet getWallet() {
            return wallet;
        }

        public byte[] getSerializedWallet() {
            Protos.Wallet proto = new WalletProtobufSerializer().walletToProto(wallet);
            return proto.toByteArray();
        }

        public void prepareWalletReset() {
            Log.i(TAG, "Wallet reset");
            wallet.reset();
            // delete chain file - this triggers re-downloading the chain (wallet replay) in the next start
            kit.stopAsync().awaitTerminated();
            blockStore = null;
            File blockstore = new File(getFilesDir(), Constants.WALLET_FILES_PREFIX + ".spvchain");
            if (blockstore.delete()) {
                Log.i(TAG, "Deleted blockchain file: " + blockstore.toString());
            }
            stopSelf();
        }

        public void deleteWalletFile() {
            File wallet = new File(getFilesDir(), Constants.WALLET_FILES_PREFIX + ".wallet");
            wallet.delete();
        }

        public boolean isReady() {
            return wallet != null;
        }

        public double getProgress() {
            return progress;
        }

        public LocalBroadcastManager getLocalBroadcastManager(){
            return LocalBroadcastManager.getInstance(WalletService.this);
        }
    }

    private class CoinbleskWalletEventListener implements WalletCoinsReceivedEventListener,
                                                          WalletCoinsSentEventListener,
                                                          ScriptsChangeEventListener,
                                                          TransactionConfidenceEventListener {
        @Override
        public void onCoinsReceived(Wallet w, Transaction tx, Coin prevBalance, Coin newBalance) {
            broadcastBalanceChanged();
            broadcastCoinsReceived();
        }

        @Override
        public void onCoinsSent(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {
            broadcastBalanceChanged();
            broadcastCoinsSent();
        }

        @Override
        public void onScriptsChanged(Wallet wallet, List<Script> scripts, boolean isAddingScripts) {
            broadcastScriptsChanged();
        }

        @Override
        public void onTransactionConfidenceChanged(Wallet wallet, Transaction tx) {
            if (progress >= 100.0) {
                broadcastConfidenceChanged(tx.getHashAsString());
            }
        }
    }

    private class DownloadListener extends DownloadProgressTracker {

        @Override
        public void onChainDownloadStarted(Peer peer, int blocksLeft) {
            super.onChainDownloadStarted(peer, blocksLeft);
            Log.d(TAG, "DownloadListener - started downloading blocks: " + blocksLeft + " blocks left.");
        }

        @Override
        public void progress(double pct, int blocksSoFar, Date date) {
            super.progress(pct, blocksSoFar, date);
            Log.d(TAG, "DownloadListener - progress: " + pct
                    + ", blocks to go: " + blocksSoFar
                    + ", date: " + org.bitcoinj.core.Utils.dateTimeFormat(date));
            progress = pct;

            Intent walletProgress = new Intent(Constants.WALLET_PROGRESS_ACTION);
            walletProgress.putExtra("progress", pct);
            walletProgress.putExtra("blocksSoFar", blocksSoFar);
            walletProgress.putExtra("date", date);
            LocalBroadcastManager
                    .getInstance(WalletService.this)
                    .sendBroadcast(walletProgress);
        }

        @Override
        protected void doneDownload() {
            super.doneDownload();
            Log.i(TAG, "DownloadListener - doneDownload");
            progress = 100.0;

            Intent walletProgress = new Intent(Constants.WALLET_READY_ACTION);
            LocalBroadcastManager
                    .getInstance(WalletService.this)
                    .sendBroadcast(walletProgress);
        }
    }
}
