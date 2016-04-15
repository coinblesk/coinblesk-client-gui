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

import com.coinblesk.payments.Constants;
import com.coinblesk.payments.Utils;
import com.coinblesk.payments.WalletService;
import com.coinblesk.payments.communications.messages.DERObject;
import com.coinblesk.payments.communications.messages.DERParser;
import com.coinblesk.payments.communications.peers.steps.PaymentFinalSignatureSendStep;
import com.coinblesk.payments.communications.peers.steps.PaymentRefundSendStep;
import com.coinblesk.payments.communications.peers.steps.PaymentRequestReceiveStep;

import org.bitcoinj.core.Transaction;
import org.bitcoinj.uri.BitcoinURI;

import java.util.Arrays;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 28/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
@TargetApi(Build.VERSION_CODES.KITKAT)
public class NFCClientService extends HostApduService {
    private final static String TAG = NFCClientService.class.getSimpleName();

    private int stepCounter = 0;
    private boolean isProcessing = false;

    private byte[] derRequestPayload = new byte[0];
    private byte[] derResponsePayload = new byte[0];

    private static final byte[] KEEPALIVE = {1, 2, 3, 4};
    private static final byte[] AID_ANDROID = {(byte) 0xF0, 0x0C, 0x01, 0x04, 0x0B, 0x01, 0x03};
    private static final byte[] AID_ANDROID_ACS = {(byte) 0xF0, 0x0C, 0x01, 0x04, 0x0B, 0x01, 0x04};
    private int maxFragmentSize = 245;

    private BitcoinURI bitcoinURI;
    private long timestamp;
    private Transaction tx;
    private Transaction refund;
    private boolean isClientStarted = false;


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Check if intent has extras
        if (intent.getExtras() != null) {
            this.isClientStarted = intent.getExtras().getBoolean(Constants.CLIENT_STARTED_KEY);
            derRequestPayload = new byte[0];
            derResponsePayload = new byte[0];
            stepCounter = 0;
            isProcessing = false;
            bitcoinURI = null;
            timestamp = 0;
            tx = null;
            refund = null;
        }

        Intent walletServiceIntent = new Intent(this, WalletService.class);
        this.bindService(walletServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        return START_NOT_STICKY;
    }


    @Override
    public byte[] processCommandApdu(byte[] commandApdu, Bundle extras) {
        if (this.isClientStarted && walletServiceBinder != null ) {
            try {
                Log.d(TAG, "this is command apdu lenght: " + commandApdu.length);
                int derPayloadStartIndex = 0;
                if (this.selectAidApdu(commandApdu)) {
                    Log.d(TAG, "handshake");
                    derPayloadStartIndex = 6 + commandApdu[4];
                    derRequestPayload = new byte[0];
                    derResponsePayload = new byte[0];
                    stepCounter = 0;
                    isProcessing = false;
                    bitcoinURI = null;
                    timestamp = 0;
                    tx = null;
                    refund = null;

                    byte[] aid = Arrays.copyOfRange(commandApdu, 5, derPayloadStartIndex - 1);
                    if (Arrays.equals(aid, AID_ANDROID)) {
                        this.maxFragmentSize = 245;
                    } else if (Arrays.equals(aid, AID_ANDROID_ACS)) {
                        this.maxFragmentSize = 53;
                    }
                    return KEEPALIVE;
                }

                if (hasNextFragment()) {
                    Log.d(TAG, "get next fragment");
                    return getNextFragment();
                } else {
                    final byte[] payload = Arrays.copyOfRange(commandApdu, derPayloadStartIndex, commandApdu.length);
                    if (!Arrays.equals(payload, KEEPALIVE)) {
                        derRequestPayload = Utils.concatBytes(derRequestPayload, payload);
                        int responseLength = DERParser.extractPayloadEndIndex(derRequestPayload);
                        Log.d(TAG, "expecting response length:" + responseLength + ", actual response length:" + derRequestPayload.length);

                        if (derRequestPayload.length >= responseLength && !isProcessing) { // we have an unprocessed request
                            isProcessing = true;
                            derResponsePayload = new byte[0];

                            final Thread processingThread = new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        final byte[] requestPayload = derRequestPayload;
                                        derRequestPayload = new byte[0];

                                        switch (stepCounter) {
                                            case 0:
                                                PaymentRequestReceiveStep paymentRequestReceiveStep = new PaymentRequestReceiveStep(walletServiceBinder);
                                                DERObject obj = paymentRequestReceiveStep.process(DERParser.parseDER(requestPayload));
                                                if (obj == null) {
                                                    //not enough funds
                                                    LocalBroadcastManager.getInstance(NFCClientService.this).sendBroadcast(new Intent(Constants.WALLET_INSUFFICIENT_BALANCE_ACTION));
                                                    break;
                                                }
                                                derResponsePayload = obj.serializeToDER();
                                                bitcoinURI = paymentRequestReceiveStep.getBitcoinURI();
                                                timestamp = paymentRequestReceiveStep.getTimestamp();
                                                stepCounter++;
                                                Log.d(TAG, "got payload1: " + derResponsePayload.length);
                                                break;
                                            case 1:
                                                final PaymentRefundSendStep paymentRefundSendStep1 = new PaymentRefundSendStep(walletServiceBinder,
                                                        bitcoinURI, timestamp);
                                                derResponsePayload = paymentRefundSendStep1.process(DERParser.parseDER(requestPayload)).serializeToDER();
                                                tx = paymentRefundSendStep1.getFullSignedTransaction();
                                                refund = paymentRefundSendStep1.getHalfSignedRefundTransaction();
                                                Log.d(TAG, "got payload2: " + derResponsePayload.length);
                                                stepCounter++;
                                                break;
                                            case 2:
                                                PaymentFinalSignatureSendStep paymentFinalSignatureSendStep = new PaymentFinalSignatureSendStep(walletServiceBinder,
                                                        bitcoinURI.getAddress(), tx, refund);
                                                derResponsePayload = paymentFinalSignatureSendStep.process(DERParser.parseDER(requestPayload)).serializeToDER();
                                                stepCounter++;
                                                break;
                                            case 3:
                                                walletServiceBinder.commitTransaction(tx);
                                                LocalBroadcastManager.getInstance(NFCClientService.this).sendBroadcast(new Intent(Constants.INSTANT_PAYMENT_SUCCESSFUL_ACTION));
                                                break;
                                        }
                                    } catch (Exception e) {
                                        Log.w(TAG, "Exception in processing thread: ", e);
                                    }
                                }
                            }, "NFCClient.Processing");
                            processingThread.start();
                        }
                    }
                    Log.d(TAG, "return keep alive: ");
                    return KEEPALIVE;

                }
            } catch (Throwable t) {
                Log.e(TAG, "hostapud issue", t);
                return null;
            }
        } else {
            return KEEPALIVE;
        }
    }

    private boolean hasNextFragment() {
        return derResponsePayload.length > 0;
    }

    private byte[] getNextFragment() {

        byte[] fragment = Arrays.copyOfRange(derResponsePayload, 0, Math.min(derResponsePayload.length, maxFragmentSize));
        derResponsePayload = Arrays.copyOfRange(derResponsePayload, fragment.length, derResponsePayload.length);
        if (derResponsePayload.length == 0) {
            isProcessing = false;
        }
        Log.d(TAG, "sending next fragment:" + fragment.length);
        return fragment;
    }

    @Override
    public void onDeactivated(int reason) {

    }

    private boolean selectAidApdu(byte[] apdu) {
        return apdu.length >= 2 && apdu[0] == (byte) 0 && apdu[1] == (byte) 0xa4;
    }


    /* ------------------- PAYMENTS INTEGRATION STARTS HERE  ------------------- */
    private WalletService.WalletServiceBinder walletServiceBinder;

    @Override
    public void onDestroy() {
        this.unbindService(this.serviceConnection);
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder binder) {
            walletServiceBinder = (WalletService.WalletServiceBinder) binder;
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            walletServiceBinder = null;
        }
    };
    /* -------------------- PAYMENTS INTEGRATION ENDS HERE  -------------------- */
}
