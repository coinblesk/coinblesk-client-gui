package com.coinblesk.payments;

import com.coinblesk.client.models.ECKeyWrapper;
import com.coinblesk.client.models.User;
import com.coinblesk.client.models.filters.ECKeyWrapperFilter;

import junit.framework.Assert;

import org.bitcoinj.core.AbstractWalletEventListener;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.DownloadProgressTracker;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.Peer;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionBroadcast;
import org.bitcoinj.core.Wallet;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.junit.Test;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import ch.papers.objectstorage.UuidObjectStorage;
import ch.papers.objectstorage.listeners.DummyOnResultListener;
import ch.papers.objectstorage.listeners.OnResultListener;
import com.coinblesk.payments.communications.http.MockServerInterface;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
public class PaymentProtocolUnitTest {

    private final static String MERCHANT_ADDRESS = "n2KzA211MGEHzeB8SkS4Tr54UFx7buG6Vc";
    private final static String FILE_PREFIX = "testnet";
    private final static String WALLET_KEY_NAME = "walletKey";
    private final static String CLIENT_KEY_NAME = "clientKey";
    private final static String SERVER_KEY_NAME = "serverKey";
    private final static File FILE_ROOT = new File(".");

    private ECKey clientKey;
    private ECKey serverKey;
    private ECKey publicServerKey;
    private ECKey publicClientKey;


    private final TransactionBroadcast.ProgressCallback progressCallback = new TransactionBroadcast.ProgressCallback() {
        @Override
        public void onBroadcastProgress(double progress) {
            System.out.println("progress:" + progress);
        }
    };


    @Test
    public void coinbleskBootstrapPOC() throws Exception {
        UuidObjectStorage.getInstance().init(FILE_ROOT);


        final CountDownLatch bootstrapCountdownLatch = new CountDownLatch(4);
        UuidObjectStorage.getInstance().getFirstMatchEntry(new ECKeyWrapperFilter(CLIENT_KEY_NAME), new OnResultListener<ECKeyWrapper>() {
            @Override
            public void onSuccess(ECKeyWrapper result) {
                System.out.println("importing client key");
                clientKey = result.getKey();
                publicClientKey = ECKey.fromPublicOnly(clientKey.getPubKey());
                bootstrapCountdownLatch.countDown();
            }

            @Override
            public void onError(String message) {
                System.out.println("creating new client key");
                ECKeyWrapper keyWrapper = new ECKeyWrapper(new ECKey().getPrivKeyBytes(), CLIENT_KEY_NAME);
                clientKey = keyWrapper.getKey();
                publicClientKey = ECKey.fromPublicOnly(clientKey.getPubKey());
                UuidObjectStorage.getInstance().addEntry(keyWrapper, DummyOnResultListener.getInstance(), ECKeyWrapper.class);
                bootstrapCountdownLatch.countDown();
            }
        }, ECKeyWrapper.class);

        UuidObjectStorage.getInstance().getFirstMatchEntry(new ECKeyWrapperFilter(SERVER_KEY_NAME), new OnResultListener<ECKeyWrapper>() {
            @Override
            public void onSuccess(ECKeyWrapper result) {
                System.out.println("importin server key");
                serverKey = result.getKey();
                publicServerKey = ECKey.fromPublicOnly(serverKey.getPubKey());
                bootstrapCountdownLatch.countDown();
            }

            @Override
            public void onError(String message) {
                System.out.println("creating new server key");
                ECKeyWrapper keyWrapper = new ECKeyWrapper(new ECKey().getPrivKeyBytes(), SERVER_KEY_NAME);
                serverKey = keyWrapper.getKey();
                publicServerKey = ECKey.fromPublicOnly(serverKey.getPubKey());
                UuidObjectStorage.getInstance().addEntry(keyWrapper, DummyOnResultListener.getInstance(), ECKeyWrapper.class);
                bootstrapCountdownLatch.countDown();
            }
        }, ECKeyWrapper.class);


        final WalletAppKit kit = new WalletAppKit(Constants.PARAMS, FILE_ROOT, FILE_PREFIX) {
            @Override
            protected void onSetupCompleted() {
                if (wallet().getKeychainSize() < 1) {
                    System.out.println("importing new key");
                    UuidObjectStorage.getInstance().getFirstMatchEntry(new ECKeyWrapperFilter(WALLET_KEY_NAME), new OnResultListener<ECKeyWrapper>() {
                        @Override
                        public void onSuccess(ECKeyWrapper result) {
                            System.out.println("imported key from storage");
                            wallet().importKey(result.getKey());
                        }

                        @Override
                        public void onError(String message) {
                            System.out.println("generated new key");
                            ECKeyWrapper walletKey = new ECKeyWrapper(new ECKey().getPrivKeyBytes(), WALLET_KEY_NAME);
                            wallet().importKey(walletKey.getKey());
                            UuidObjectStorage.getInstance().addEntry(walletKey, DummyOnResultListener.getInstance(), ECKeyWrapper.class);
                        }
                    }, ECKeyWrapper.class);
                }


                wallet().addEventListener(new AbstractWalletEventListener() {
                    @Override
                    public void onCoinsSent(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {
                        super.onCoinsSent(wallet, tx, prevBalance, newBalance);
                        System.out.println("sent:" + prevBalance + "->" + newBalance + " tid " + tx.getHashAsString());
                    }

                    @Override
                    public void onScriptsChanged(Wallet wallet, List<Script> scripts, boolean isAddingScripts) {
                        super.onScriptsChanged(wallet, scripts, isAddingScripts);
                        for (Script script : scripts) {
                            System.out.println("scriptschanges:" + isAddingScripts + "/" + script);

                        }
                    }

                    @Override
                    public void onTransactionConfidenceChanged(Wallet wallet, Transaction tx) {
                        super.onTransactionConfidenceChanged(wallet, tx);
                        System.out.println("confidencechange:" + tx.getConfidence()+ " tid "+tx.getHashAsString());
                    }

                    @Override
                    public void onCoinsReceived(Wallet w, Transaction tx, Coin prevBalance, Coin newBalance) {
                        System.out.println("received:" + prevBalance + "->" + newBalance);
                        System.out.println("wallet:" + w.getBalance());
                    }
                });

                setDownloadListener(new DownloadProgressTracker() {

                    @Override
                    public void onChainDownloadStarted(Peer peer, int blocksLeft) {
                        super.onChainDownloadStarted(peer, blocksLeft);
                        System.out.println("started download of block:" + blocksLeft);
                    }

                    @Override
                    public void progress(double pct, int blocksSoFar, Date date) {
                        super.progress(pct, blocksSoFar, date);
                        System.out.println("progress:" + pct);
                    }


                    @Override
                    protected void doneDownload() {
                        super.doneDownload();
                        System.out.println("download finished!");
                    }
                });

                System.out.println("setup completed:" + wallet().currentReceiveAddress());
                bootstrapCountdownLatch.countDown();
            }
        };

        UuidObjectStorage.getInstance().commit(new OnResultListener<String>() {
            @Override
            public void onSuccess(String result) {
                System.out.println("nice: " + result);
                bootstrapCountdownLatch.countDown();
            }

            @Override
            public void onError(String message) {
                System.out.println("oops, error during commit " + message);
                bootstrapCountdownLatch.countDown();
            }
        });
        kit.startAsync();
        bootstrapCountdownLatch.await();

        //Thread.sleep(30000);
        // Bitcoinj is ready, starting bootstrap
        final PaymentProtocol paymentProtocol = PaymentProtocol.getInstance();
        final ServerInterface service = new MockServerInterface();
        final User clientUser = new User(UUID.randomUUID(),clientKey);
        final User serverUser = service.createUser(new User(clientUser.getUuid(), ECKey.fromPublicOnly(clientUser.getEcKey().getPubKey()))).execute().body();


        // creating to multisig transaction
        Coin amount = Coin.valueOf(20000);
        Transaction toMultisigTransaction = paymentProtocol.generateToMultisigTransaction(serverUser.getEcKey(), clientUser.getEcKey(), amount);
        Wallet.SendRequest req = Wallet.SendRequest.forTx(toMultisigTransaction);
        try {
            kit.wallet().completeTx(req);
        } catch (InsufficientMoneyException e) {
            e.printStackTrace();
        }
        req.tx.verify();
        req.tx.getInput(0).verify();


        // before we broadcast creating refund transaction
        Transaction refundTransaction = paymentProtocol.generateRefundTransaction(req.tx.getOutput(0), kit.wallet().currentReceiveKey().toAddress(Constants.PARAMS));
        TransactionSignature refundTransactionServerSignature = service.createRefund(refundTransaction, paymentProtocol.signMultisigTransaction(req.tx.getOutput(0), refundTransaction, clientUser.getEcKey())).execute().body();

        Script refundTransactionInputScript = ScriptBuilder.createMultiSigInputScript(paymentProtocol.signMultisigTransaction(req.tx.getOutput(0), refundTransaction, clientUser.getEcKey()),refundTransactionServerSignature);
        refundTransaction.getInput(0).setScriptSig(refundTransactionInputScript);
        refundTransaction.getInput(0).verify();

        // refund transaction has been verified, we can proceed with multisig
       // Transaction tx = kit.peerGroup().broadcastTransaction(req.tx).future().get();
        service.createMultisig(req.tx);

        System.out.println("verified initial refund, we're good");

        //
        TransactionSignature fromMultisigSignature = service.createTransaction(new Address(Constants.PARAMS, MERCHANT_ADDRESS),Coin.valueOf(5000)).execute().body();
        final Transaction externalTransaction = PaymentProtocol.getInstance().generateFromMultisigTransaction(req.tx.getOutput(0), serverUser.getEcKey(),
                clientUser.getEcKey(), Coin.valueOf(5000), new Address(Constants.PARAMS, MERCHANT_ADDRESS));


        Script externalTransactionInputScript = ScriptBuilder.createMultiSigInputScript(paymentProtocol.signMultisigTransaction(req.tx.getOutput(0),
                externalTransaction, clientKey),fromMultisigSignature);
        externalTransaction.getInput(0).setScriptSig(externalTransactionInputScript);
        externalTransaction.getInput(0).verify(req.tx.getOutput(0));
        System.out.println("verified transaction to external, we're good");




        final Transaction newRefundTransaction = PaymentProtocol.getInstance().generateRefundTransaction(externalTransaction.getOutput(0),
                clientUser.getEcKey().toAddress(Constants.PARAMS));
        byte[] signedoutputSize = newRefundTransaction.bitcoinSerialize();

        TransactionSignature refundTransaction2ServerSignature = service.createRefund(newRefundTransaction, paymentProtocol.signMultisigTransaction(externalTransaction.getOutput(0), newRefundTransaction, clientUser.getEcKey())).execute().body();
        Script newRefundInputScript = ScriptBuilder.createMultiSigInputScript(paymentProtocol.signMultisigTransaction(externalTransaction.getOutput(0),
                newRefundTransaction, clientKey),refundTransaction2ServerSignature);
        newRefundTransaction.getInput(0).setScriptSig(newRefundInputScript);
        newRefundTransaction.getInput(0).verify(externalTransaction.getOutput(0));
        System.out.println("verified new refund transaction, we're good");

        kit.stopAsync();

        Thread.sleep(1000);
       /* try {
            for (int i = 0; i < 5; i++) {
                Coin halfAmount = Coin.valueOf(40000);
                TransactionOutput multisigOutput = tx.getOutput(0);

                final Transaction externalTransaction = paymentProtocol.generateFromMultisigTransaction(multisigOutput, serverUser.getEcKey(), clientUser.getEcKey(), halfAmount, new Address(Constants.PARAMS, MERCHANT_ADDRESS));

                Script merchantInputScript = ScriptBuilder.createMultiSigInputScript(paymentProtocol.signMultisigTransaction(multisigOutput, externalTransaction, clientKey), paymentProtocol.signMultisigTransaction(multisigOutput, externalTransaction, serverKey));
                externalTransaction.getInput(0).setScriptSig(merchantInputScript);
                externalTransaction.getInput(0).verify(multisigOutput);
                System.out.println("the fee" + i + ":" + externalTransaction.getFee());
                kit.peerGroup().broadcastTransaction(externalTransaction, 2);
                tx = externalTransaction;
                System.out.println("txid:" + tx.getHashAsString());
                Thread.sleep(10000);
            }
            return;
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }*/


    }

    @Test
    public void firstPackage() {
        ECKey key = new ECKey();
        Coin amount = Coin.valueOf(200);
        Address address = key.toAddress(Constants.PARAMS);

        ByteBuffer srcByteBuffer = ByteBuffer.allocate(8 + 20 + 33);
        srcByteBuffer.putLong(amount.getValue());
        srcByteBuffer.put(address.getHash160());
        srcByteBuffer.put(key.getPubKey());

        // transfer ...

        ByteBuffer dstByteBuffer = ByteBuffer.allocate(8 + 20 + 33);
        dstByteBuffer.put(srcByteBuffer.array(), 0, 8 + 20 + 33);
        dstByteBuffer.flip();

        final long value = dstByteBuffer.getLong();
        final byte[] hash160 = new byte[20];
        final byte[] publicKey = new byte[33];

        dstByteBuffer.get(hash160, 0, 20);
        dstByteBuffer.get(publicKey, 0, 33);

        Assert.assertEquals(amount, Coin.valueOf(value));
        Assert.assertEquals(address, new Address(Constants.PARAMS, hash160));
        Assert.assertEquals(ECKey.fromPublicOnly(key.getPubKey()), ECKey.fromPublicOnly(publicKey));
    }


    @Test
    public void secondPackage() {
        ECKey key = new ECKey();
        Sha256Hash exampleHash = Sha256Hash.of(key.getPubKey());
        ECKey.ECDSASignature signature = key.sign(exampleHash);
        byte[] signatureBytes = signature.encodeToDER();
        signatureBytes = signatureBytes;

        //new TransactionSignature.
    }
}