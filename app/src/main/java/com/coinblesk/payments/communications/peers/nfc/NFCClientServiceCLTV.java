package com.coinblesk.payments.communications.peers.nfc;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.nfc.cardemulation.HostApduService;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.coinblesk.client.config.Constants;
import com.coinblesk.client.utils.ClientUtils;
import com.coinblesk.client.utils.SharedPrefUtils;
import com.coinblesk.der.DERObject;
import com.coinblesk.der.DERParser;
import com.coinblesk.payments.WalletService;
import com.coinblesk.payments.communications.PaymentError;
import com.coinblesk.payments.communications.PaymentException;
import com.coinblesk.payments.communications.steps.cltv.PaymentFinalizeStep;
import com.coinblesk.payments.communications.steps.cltv.PaymentRequestReceiveStep;
import com.coinblesk.payments.communications.steps.cltv.PaymentResponseSendCompactStep;
import com.coinblesk.payments.communications.steps.cltv.PaymentServerSignatureReceiveStep;
import com.coinblesk.util.Pair;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.uri.BitcoinURI;
import org.bitcoinj.uri.BitcoinURIParseException;

import java.util.Arrays;
import java.util.List;

/**
 * @author Alessandro De Carli
 * @author Andreas Albrecht
 */
@TargetApi(Build.VERSION_CODES.KITKAT)
public class NFCClientServiceCLTV extends HostApduService {
    private final static String TAG = NFCClientServiceCLTV.class.getName();
    private final static Object LOCK = new Object();

    private long startTime;

    private boolean isProcessing = false;

    private byte[] derRequestPayload = new byte[0];
    private byte[] derResponsePayload = new byte[0];

    private int maxFragmentSize = NFCUtils.DEFAULT_MAX_FRAGMENT_SIZE;

    private BitcoinURI bitcoinURI;
    private Transaction transaction;
    private List<TransactionSignature> clientTxSignatures;
    private List<TransactionSignature> serverTxSignatures;


    private String approveAddress = null;
    private String approveAmount = null;


    private boolean bound = false;

    private BroadcastReceiver approvedReceiver =  new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "got approved, set local variables");
            approveAddress = intent.getStringExtra(Constants.PAYMENT_REQUEST_ADDRESS);
            approveAmount = intent.getStringExtra(Constants.PAYMENT_REQUEST_AMOUNT);
            isProcessing = false;
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        LocalBroadcastManager.getInstance(this).registerReceiver(
                approvedReceiver, new IntentFilter(Constants.PAYMENT_REQUEST_APPROVED));

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(approvedReceiver);
        Log.d(TAG, "onDestroy");
        if(bound) {
            unbindService(serviceConnection);
            bound = false;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent walletServiceIntent = new Intent(this, WalletService.class);
        bound = bindService(walletServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        Log.d(TAG, "onStartCommand");
        return START_NOT_STICKY;
    }

    @Override
    public void onDeactivated(int reason) {
        String reasonStr;
        switch (reason) {
            case DEACTIVATION_LINK_LOSS:
                reasonStr = "DEACTIVATION_LINK_LOSS";
                break;
            case DEACTIVATION_DESELECTED:
                reasonStr = "DEACTIVATION_DESELECTED";
                break;
            default:
                reasonStr = "unknown";
        }
        Log.d(TAG, "onDeactivated - reason=" + reasonStr + " (" + reason + ")");
    }

    @Override
    public byte[] processCommandApdu(byte[] commandApdu, Bundle extras) {
        Log.d(TAG, "processCommandApdu - commandApdu.length=" + commandApdu.length);
        if (startTime == 0) {
            startTime = System.currentTimeMillis();
        }


        try {

            /* HANDSHAKE */
            if (NFCUtils.selectAidApdu(commandApdu)) {
                startAppIfNotRunning();
                int derPayloadStartIndex = 6 + commandApdu[4];
                Log.d(TAG, "processCommandApdu - handshake (derPayloadStartIndex="+derPayloadStartIndex+")");
                executeHandshake(commandApdu, derPayloadStartIndex);
                return NFCUtils.KEEPALIVE;
            }

            if(clientTxSignatures == null && approveAmount != null && bitcoinURI != null && !isProcessing) {
                isProcessing = true;
                derResponsePayload = new byte[0];
                Thread processCommand = new Thread(new ProcessCommandRequest(), "NFCClient.ProcessCommand");
                processCommand.start();
                Log.d(TAG, "process for signig started");

            }

            /* NEXT FRAGMENT */
            if (hasNextFragment()) {
                Pair<byte[], byte[]> pair = getNextFragment(derResponsePayload, maxFragmentSize);
                byte[] fragment = pair.element0();
                derResponsePayload = pair.element1();
                if (derResponsePayload.length == 0) {
                    isProcessing = false;
                }
                Log.d(TAG, "has next fragment ready, return payload with length=" + fragment.length);
                return fragment;
            }

            /* PROCESS PAYLOAD */
            if (NFCUtils.isKeepAlive(commandApdu)) {
                Log.d(TAG, "got keep alive, sending it back");
                return NFCUtils.KEEPALIVE;
            }

            /* HANDLE REQUEST (not keepalive) */
            derRequestPayload = ClientUtils.concatBytes(derRequestPayload, commandApdu);
            final int responseLength = DERParser.extractPayloadEndIndex(derRequestPayload);
            Log.d(TAG, "processCommandApdu -  requestPayload - expected length=" + responseLength
                     + ", actual length=" + derRequestPayload.length);

            if ((derRequestPayload.length >= responseLength) && !isProcessing) {
                // we have an unprocessed request
                isProcessing = true;
                derResponsePayload = new byte[0];

                if(clientTxSignatures == null) {
                    Thread processCommand = new Thread(new ProcessCommandRequest(), "NFCClient.ProcessCommand");
                    processCommand.start();
                }
                else {
                    handlePaymentFinalize(derRequestPayload);
                    return DERObject.NULLOBJECT.serializeToDER();
                }
            }

            Log.d(TAG, "processCommandApdu - Keep alive");
            return NFCUtils.KEEPALIVE;
        } catch (Throwable t) {
            Log.e(TAG, "processCommandApdu - catched throwable: ", t);
            return null;
        }
    }

    private void startAppIfNotRunning() {
        Intent intent2 = new Intent(Constants.START_COINBLESK);
        intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent2);
    }

    private void reset() {
        derRequestPayload = new byte[0];
        derResponsePayload = new byte[0];
        isProcessing = false;
        startTime = 0;

        bitcoinURI = null;
        transaction = null;
        clientTxSignatures = null;
        serverTxSignatures = null;
    }

    private void executeHandshake(byte[] commandApdu, int derPayloadStartIndex) {
        reset();
        byte[] aid = Arrays.copyOfRange(commandApdu, 5, derPayloadStartIndex - 1);
        setMaxFragmentSizeByAid(aid);
    }

    private void setMaxFragmentSizeByAid(byte[] aid) {
        maxFragmentSize = NFCUtils.maxFragmentSizeByAid(aid);
        Log.d(TAG, "Set maxFragmentSize=" + maxFragmentSize);
    }

    private boolean hasNextFragment() {
        return derResponsePayload.length > 0;
    }

    private static Pair<byte[], byte[]> getNextFragment(byte[] derResponsePayload, int maxFragmentSize) {
        int payloadEnd = Math.min(derResponsePayload.length, maxFragmentSize);
        byte[] fragment = Arrays.copyOfRange(derResponsePayload, 0, payloadEnd);
        derResponsePayload = Arrays.copyOfRange(derResponsePayload, fragment.length, derResponsePayload.length);
        return new Pair<>(fragment, derResponsePayload);
    }

    private void handlePaymentFinalize(byte[] payload) throws PaymentException {
        PaymentServerSignatureReceiveStep step = new PaymentServerSignatureReceiveStep(walletServiceBinder);
        DERObject input = DERParser.parseDER(payload);
        step.process(input);

        serverTxSignatures = step.getServerTransactionSignatures();
        PaymentFinalizeStep finalizeStep = new PaymentFinalizeStep(
                bitcoinURI,
                transaction,
                clientTxSignatures,
                serverTxSignatures,
                walletServiceBinder);
        finalizeStep.process(null);
        transaction = finalizeStep.getTransaction();
        derResponsePayload = DERObject.NULLOBJECT.serializeToDER();

        approveAddress = null;
        approveAmount = null;
        walletServiceBinder.maybeCommitAndBroadcastTransaction(transaction);
        broadcastInstantPaymentSuccess();
        reset();
        long duration = System.currentTimeMillis() - startTime;
        Log.d(TAG, "Payment completed: total duration " + duration + " ms");
    }

    private void waitForWalletReady() {
        synchronized (LOCK) {
            while (walletServiceBinder == null) {
                //wait until ready
                try {
                    LOCK.wait();
                } catch (InterruptedException e) {
                    Log.e(TAG, "interrupted", e);
                    return;
                }
            }
        }
    }

    private class ProcessCommandRequest implements Runnable {


        public ProcessCommandRequest() {

        }

        @Override
        public void run() {
            long processStartTime = System.currentTimeMillis();
            waitForWalletReady();
            final byte[] requestPayload = derRequestPayload;
            try {
                derRequestPayload = new byte[0];
                 Log.d(TAG, "handle PAYMENT_REQUEST_RECEIVE");
                 handlePaymentRequestReceive(requestPayload, bitcoinURI!=null);
            } catch (PaymentException pe) {
                approveAddress = null;
                approveAmount = null;
                if(pe.getErrorCode() == PaymentError.INSUFFICIENT_FUNDS) {
                    broadcastInsufficientBalance();
                } else {
                    broadcastError();
                }
                long duration = System.currentTimeMillis() - processStartTime;
                Log.d(TAG, "Payment not completed: " + duration + "ms");
                pe.printStackTrace();
            }
            catch (Exception e) {
                Log.w(TAG, "Exception in processing thread: ", e);
            } finally {
                long duration = System.currentTimeMillis() - processStartTime;
                long totalDuration = System.currentTimeMillis() - startTime;
                Log.d(TAG, String.format(
                                "ProcessCommand - %d ms, %d since startTime): bitcoinURI=%s, " +
                                "request length=%d bytes, response length=%d bytes",
                                duration, totalDuration, bitcoinURI,
                                requestPayload.length, derResponsePayload.length));
            }
        }

        private void handlePaymentRequestReceive(byte[] requestPayload, boolean resume) throws PaymentException, BitcoinURIParseException {
            if(!resume) {
                NetworkParameters params = walletServiceBinder.getNetworkParameters();
                PaymentRequestReceiveStep request = new PaymentRequestReceiveStep(params);
                DERObject input = DERParser.parseDER(requestPayload);
                request.process(input);
                bitcoinURI = request.getBitcoinURI();
            }


            boolean isPaymentAutoAccepted = SharedPrefUtils.isPaymentAutoAcceptEnabled(NFCClientServiceCLTV.this) && SharedPrefUtils.getPaymentAutoAcceptValue(NFCClientServiceCLTV.this).isGreaterThan(bitcoinURI.getAmount());
            boolean isPaymentApproved = bitcoinURI.getAddress().toString().equals(approveAddress) && bitcoinURI.getAmount().toString().equals(approveAmount);
            if(isPaymentAutoAccepted || isPaymentApproved) {
                Log.d(TAG, "payment approved - isAutoAccepted: " + isPaymentAutoAccepted + ", isApproved: " + isPaymentApproved);
                PaymentResponseSendCompactStep response = new PaymentResponseSendCompactStep(bitcoinURI, walletServiceBinder);
                DERObject result = response.process(DERObject.NULLOBJECT);
                derResponsePayload = result.serializeToDER();
                transaction = response.getTransaction();
                clientTxSignatures = response.getClientTransactionSignatures();
            } else {
                Log.d(TAG, "Not yet approved");
                approveAddress = null;
                approveAmount = null;
                Intent intent = new Intent(Constants.PAYMENT_REQUEST);
                intent.putExtra(Constants.PAYMENT_REQUEST_ADDRESS, bitcoinURI.getAddress().toString());
                intent.putExtra(Constants.PAYMENT_REQUEST_AMOUNT, bitcoinURI.getAmount().toString());
                LocalBroadcastManager
                        .getInstance(NFCClientServiceCLTV.this)
                        .sendBroadcast(intent);
            }
        }
    }


    /* ------------------- PAYMENTS INTEGRATION STARTS HERE  ------------------- */
    private WalletService.WalletServiceBinder walletServiceBinder;

    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            synchronized (LOCK) {
                walletServiceBinder = (WalletService.WalletServiceBinder) binder;
                LOCK.notifyAll();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            walletServiceBinder = null;
        }
    };
    /* -------------------- PAYMENTS INTEGRATION ENDS HERE  -------------------- */

    private void broadcastInsufficientBalance() {
        Intent insufficientBalance = new Intent(Constants.WALLET_INSUFFICIENT_BALANCE_ACTION);
        LocalBroadcastManager
                .getInstance(NFCClientServiceCLTV.this)
                .sendBroadcast(insufficientBalance);
    }

    private void broadcastError() {
        Intent intent = new Intent(Constants.WALLET_ERROR_ACTION);
        LocalBroadcastManager
                .getInstance(NFCClientServiceCLTV.this)
                .sendBroadcast(intent);
    }

    private void broadcastInstantPaymentSuccess() {
        Intent instantPaymentSuccess = new Intent(Constants.INSTANT_PAYMENT_SUCCESSFUL_ACTION);
        LocalBroadcastManager
                .getInstance(this)
                .sendBroadcast(instantPaymentSuccess);
    }
}
