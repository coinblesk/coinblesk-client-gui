package com.coinblesk.payments.communications.peers.nfc;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.nfc.cardemulation.HostApduService;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.coinblesk.client.config.Constants;
import com.coinblesk.client.utils.ClientUtils;
import com.coinblesk.der.DERObject;
import com.coinblesk.der.DERParser;
import com.coinblesk.payments.WalletService;
import com.coinblesk.payments.communications.PaymentException;
import com.coinblesk.payments.communications.steps.cltv.PaymentFinalizeStep;
import com.coinblesk.payments.communications.steps.cltv.PaymentRequestReceiveStep;
import com.coinblesk.payments.communications.steps.cltv.PaymentResponseSendCompactStep;
import com.coinblesk.payments.communications.steps.cltv.PaymentServerSignatureReceiveStep;

import org.bitcoinj.core.Transaction;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.uri.BitcoinURI;

import java.util.Arrays;
import java.util.List;

/**
 * @author Alessandro De Carli
 * @author Andreas Albrecht
 */
@TargetApi(Build.VERSION_CODES.KITKAT)
public class NFCClientServiceCLTV extends HostApduService {
    private final static String TAG = NFCClientServiceCLTV.class.getName();

    private long startTime;

    private ClientSteps nextStep;
    private boolean isProcessing = false;
    private boolean isClientStarted = false;

    private byte[] derRequestPayload = new byte[0];
    private byte[] derResponsePayload = new byte[0];

    private int maxFragmentSize = NFCUtils.DEFAULT_MAX_FRAGMENT_SIZE;

    private BitcoinURI bitcoinURI;
    private Transaction transaction;
    private List<TransactionSignature> clientTxSignatures;
    private List<TransactionSignature> serverTxSignatures;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Check if intent has extras
        if (intent.getExtras() != null) {
            isClientStarted = intent.getExtras().getBoolean(Constants.CLIENT_STARTED_KEY, false);
            reset();
        }

        Intent walletServiceIntent = new Intent(this, WalletService.class);
        bindService(walletServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        Log.d(TAG, "onStartCommand");
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        unbindService(serviceConnection);
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
        //Log.d(TAG, "processCommandApdu - commandApdu.length=" + commandApdu.length);
        if (startTime == 0) {
            startTime = System.currentTimeMillis();
        }
        if (!isClientStarted || walletServiceBinder == null ) {
            return NFCUtils.KEEPALIVE;
        }
        try {

            int derPayloadStartIndex = 0;

            /* HANDSHAKE */
            if (NFCUtils.selectAidApdu(commandApdu)) {
                derPayloadStartIndex = 6 + commandApdu[4];
                Log.d(TAG, "processCommandApdu - handshake (derPayloadStartIndex="+derPayloadStartIndex+")");
                executeHandshake(commandApdu, derPayloadStartIndex);
                return NFCUtils.KEEPALIVE;
            }

            /* NEXT FRAGMENT */
            if (hasNextFragment()) {
                byte[] fragment = getNextFragment();
                Log.d(TAG, "has next fragment ready, return payload with length=" + fragment.length);
                return fragment;
            }

            /* PROCESS PAYLOAD */
            final byte[] payload = Arrays.copyOfRange(commandApdu, derPayloadStartIndex, commandApdu.length);
            if (NFCUtils.isKeepAlive(payload)) {
                return NFCUtils.KEEPALIVE;
            }

            /* HANDLE REQUEST (not keepalive) */
            derRequestPayload = ClientUtils.concatBytes(derRequestPayload, payload);
            final int responseLength = DERParser.extractPayloadEndIndex(derRequestPayload);
            Log.d(TAG, "processCommandApdu -  requestPayload - expected length=" + responseLength
                     + ", actual length=" + derRequestPayload.length);

            if ((derRequestPayload.length >= responseLength) && !isProcessing) {
                // we have an unprocessed request
                isProcessing = true;
                derResponsePayload = new byte[0];
                Thread processCommand = new Thread(new ProcessCommand(), "NFCClient.ProcessCommand");
                processCommand.start();
            }

            Log.d(TAG, "processCommandApdu - Keep alive");
            return NFCUtils.KEEPALIVE;
        } catch (Throwable t) {
            Log.e(TAG, "processCommandApdu - catched throwable: ", t);
            return null;
        }
    }

    private void reset() {
        derRequestPayload = new byte[0];
        derResponsePayload = new byte[0];
        nextStep = ClientSteps.PAYMENT_REQUEST_RECEIVE;
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

    private byte[] getNextFragment() {
        int payloadEnd = Math.min(derResponsePayload.length, maxFragmentSize);
        byte[] fragment = Arrays.copyOfRange(derResponsePayload, 0, payloadEnd);
        derResponsePayload = Arrays.copyOfRange(derResponsePayload, fragment.length, derResponsePayload.length);
        if (derResponsePayload.length == 0) {
            isProcessing = false;
        }
        return fragment;
    }

    private class ProcessCommand implements Runnable {

        @Override
        public void run() {
            long startTime = System.currentTimeMillis();
            ClientSteps currentStep = nextStep;
            final byte[] requestPayload = derRequestPayload;
            try {
                derRequestPayload = new byte[0];
                switch (currentStep) {
                    case PAYMENT_REQUEST_RECEIVE:
                        Log.d(TAG, "handle PAYMENT_REQUEST_RECEIVE");
                        handlePaymentRequestReceive(requestPayload);
                        nextStep = ClientSteps.SIGNATURES_RECEIVE;
                        break;
                    case SIGNATURES_RECEIVE:
                        Log.d(TAG, "handle SIGNATURES_RECEIVE");
                        handlePaymentFinalize(requestPayload);
                        nextStep = ClientSteps.PAYMENT_COMPLETED;
                        break;
                    case PAYMENT_COMPLETED:
                        Log.d(TAG, "handle PAYMENT_COMPLETED");
                        handlePaymentCompleted();
                        nextStep = ClientSteps.NULL;
                        break;
                    default:
                        Log.w(TAG, "ProcessCommand does not know how to handle current step: " + currentStep);
                }
            } catch (Exception e) {
                Log.w(TAG, "Exception in processing thread: ", e);
            } finally {
                long duration = System.currentTimeMillis() - startTime;
                Log.d(TAG, String.format(
                                "ProcessCommand - %s (%d ms): bitcoinURI=%s, " +
                                "request length=%d bytes, response length=%d bytes",
                                currentStep.toString(), duration, bitcoinURI,
                                requestPayload.length, derResponsePayload.length));
            }
        }
    }

    private void handlePaymentCompleted() {
        walletServiceBinder.commitAndBroadcastTransaction(transaction);
        broadcastInstantPaymentSuccess();
        long duration = System.currentTimeMillis() - startTime;
        Log.d(TAG, "Payment completed: " + duration + "ms");
    }

    private void handlePaymentRequestReceive(byte[] requestPayload) throws PaymentException {
        PaymentRequestReceiveStep request = new PaymentRequestReceiveStep();
        DERObject input = DERParser.parseDER(requestPayload);
        request.process(input);
        bitcoinURI = request.getBitcoinURI();

        PaymentResponseSendCompactStep response = new PaymentResponseSendCompactStep(bitcoinURI, walletServiceBinder);
        DERObject result = response.process(DERObject.NULLOBJECT);
        derResponsePayload = result.serializeToDER();
        transaction = response.getTransaction();
        clientTxSignatures = response.getClientTransactionSignatures();
    }

    private void handlePaymentFinalize(byte[] payload) throws PaymentException {
        PaymentServerSignatureReceiveStep step = new PaymentServerSignatureReceiveStep();
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
    }

    /* ------------------- PAYMENTS INTEGRATION STARTS HERE  ------------------- */
    private WalletService.WalletServiceBinder walletServiceBinder;

    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            walletServiceBinder = (WalletService.WalletServiceBinder) binder;
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

    private void broadcastInstantPaymentSuccess() {
        Intent instantPaymentSuccess = new Intent(Constants.INSTANT_PAYMENT_SUCCESSFUL_ACTION);
        LocalBroadcastManager
                .getInstance(this)
                .sendBroadcast(instantPaymentSuccess);
    }

    private enum ClientSteps {
        NULL(-1),
        PAYMENT_REQUEST_RECEIVE(0),
        PAYMENT_REFUND_SEND(1),
        PAYMENT_FINAL_SIGNATURE_OUTPOINTS_SEND(2),
        PAYMENT_COMPLETED(3),
        SIGNATURES_RECEIVE(4);

        private final int step;
        ClientSteps(int step) {
            this.step = step;
        }
    }
}
