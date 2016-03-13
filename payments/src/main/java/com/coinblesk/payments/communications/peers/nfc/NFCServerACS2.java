package com.coinblesk.payments.communications.peers.nfc;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.util.Log;

import com.acs.smartcard.Reader;
import com.acs.smartcard.ReaderException;
import com.coinblesk.payments.Utils;
import com.coinblesk.payments.WalletService;
import com.coinblesk.payments.communications.messages.DERObject;
import com.coinblesk.payments.communications.messages.DERParser;
import com.coinblesk.payments.communications.peers.AbstractServer;
import com.coinblesk.payments.communications.peers.steps.PaymentAuthorizationReceiveStep;
import com.coinblesk.payments.communications.peers.steps.PaymentFinalSignatureReceiveStep;
import com.coinblesk.payments.communications.peers.steps.PaymentRefundReceiveStep;
import com.coinblesk.payments.communications.peers.steps.PaymentRequestSendStep;
import com.coinblesk.util.Pair;

import java.io.IOException;
import java.util.Arrays;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 28/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */

@TargetApi(Build.VERSION_CODES.KITKAT)
public class NFCServerACS2 extends AbstractServer {
    private final static String TAG = NFCServerACS2.class.getSimpleName();

    private static final byte[] CLA_INS_P1_P2 = {(byte) 0x00, (byte) 0xA4, (byte) 0x04, (byte) 0x00};
    private static final byte[] AID_ANDROID_ACS = {(byte) 0xF0, 0x0C, 0x01, 0x04, 0x0B, 0x01, 0x04};
    private static final byte[] KEEPALIVE = {1, 2, 3, 4};

    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";


    private Reader reader;
    private BroadcastReceiver broadcastReceiver;

    public NFCServerACS2(Context context, WalletService.WalletServiceBinder walletServiceBinder) {
        super(context, walletServiceBinder);

        if (isSupported()) {
            UsbManager manager = (UsbManager) getContext().getSystemService(Context.USB_SERVICE);
            PendingIntent permissionIntent = PendingIntent.getBroadcast(getContext(), 0, new Intent(ACTION_USB_PERMISSION), 0);
            Reader reader = new Reader(manager);
            UsbDevice externalDevice = externalReaderAttached(getContext(), manager, reader);
            manager.requestPermission(externalDevice, permissionIntent);
        }
    }

    @Override
    public boolean isSupported() {
        return hasClass("com.acs.smartcard.Reader") && isExternalReaderAttached(getContext());
    }

    @Override
    public void start() {
        if (this.isRunning()) {
            Log.d(TAG, "Already turned on ACS");
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        broadcastReceiver = createBroadcastReceiver(reader /*, callback*/);
        getContext().registerReceiver(broadcastReceiver, filter);

        this.setRunning(true);
    }

    @Override
    public void stop() {
/*        try {
            if (!this.isRunning()) {
                Log.d(TAG, "Already turned off ACS");
            }
            Log.d(TAG, "Turn off ACS");
            if (reader != null && reader.isOpened()) {
                reader.close();
                reader = null;
                Log.d(TAG, "Reader closed");
            }
            getContext().unregisterReceiver(broadcastReceiver);
            this.setRunning(false);
        } catch (Exception e) {
        }*/
    }

    @Override
    public void onChangePaymentRequest() {
        if (this.hasPaymentRequestUri()) {
            Log.d(TAG,"got new payment request url:"+getPaymentRequestUri());
            try {
                Pair<ACSTransceiver, Reader> pair = createReaderAndTransceiver(getContext());
                this.reader = pair.element1();
                setOnStateChangedListener(reader, pair.element0(), new NFCClientACSCallback() {
                    @Override
                    public void tagDiscovered(ACSTransceiver transceiver) {
                        try {
                            final PaymentRequestSendStep paymentRequestSendStep = new PaymentRequestSendStep(getPaymentRequestUri());
                            DERObject authorizationResponseInput = transceiveDER(transceiver, paymentRequestSendStep.process(DERObject.NULLOBJECT), true);

                            final PaymentAuthorizationReceiveStep paymentAuthorizationReceiveStep = new PaymentAuthorizationReceiveStep(getPaymentRequestUri());
                            DERObject paymentRefundReceiveInput = transceiveDER(transceiver, paymentAuthorizationReceiveStep.process(authorizationResponseInput));

                            final PaymentRefundReceiveStep paymentRefundReceiveStep = new PaymentRefundReceiveStep(paymentAuthorizationReceiveStep.getClientPublicKey());
                            DERObject paymentFinalSignatureReceiveInput = transceiveDER(transceiver, paymentRefundReceiveStep.process(paymentRefundReceiveInput));

                            final PaymentFinalSignatureReceiveStep paymentFinalSignatureReceiveStep = new PaymentFinalSignatureReceiveStep(paymentAuthorizationReceiveStep.getClientPublicKey(), getPaymentRequestUri().getAddress());
                            paymentFinalSignatureReceiveStep.process(paymentFinalSignatureReceiveInput);


                            getWalletServiceBinder().commitTransaction(paymentFinalSignatureReceiveStep.getFullSignedTransaction());
                            getPaymentRequestAuthorizer().onPaymentSuccess();
                        } catch (Exception e) {
                        }
                    }


                    @Override
                    public void tagFailed() {

                    }

                    @Override
                    public void nfcTagLost() {

                    }
                });

            } catch (IOException e){}
        } else {
        }
    }

    public DERObject transceiveDER(ACSTransceiver acsTransceiver, DERObject input, boolean needsSelectAidApdu) throws Exception {
        Log.d(TAG, "start transceive");
        byte[] derPayload = input.serializeToDER();
        byte[] derResponse = new byte[0];
        int fragmentByte = 0;

        Log.d(TAG, "have to send bytes:" + derPayload.length);
        while (fragmentByte < derPayload.length) {
            byte[] fragment = new byte[0];
            if (needsSelectAidApdu) {
                acsTransceiver.write(createSelectAidApdu(AID_ANDROID_ACS));
                needsSelectAidApdu = false;
            }

            fragment = Utils.concatBytes(fragment, Arrays.copyOfRange(derPayload, fragmentByte, Math.min(derPayload.length, fragmentByte + 53)));


            Log.d(TAG, "about to send fragment size:" + fragment.length);
            derResponse = acsTransceiver.write(fragment);
            Log.d(TAG, "my client received payload" + Arrays.toString(derResponse));
            fragmentByte += fragment.length;
        }

        while (Arrays.equals(derResponse, KEEPALIVE)) {
            derResponse = acsTransceiver.write(KEEPALIVE);
        }

        int responseLength = DERParser.extractPayloadEndIndex(derResponse);
        Log.d(TAG, "expected response lenght:" + responseLength);
        Log.d(TAG, "actual response lenght:" + derResponse.length);

        while (derResponse.length < responseLength) {
            derResponse = Utils.concatBytes(derResponse, acsTransceiver.write(KEEPALIVE));
            Log.d(TAG, "had to ask for next bytes:" + derResponse.length);
        }
        Log.d(TAG, "end transceive");
        return DERParser.parseDER(derResponse);
    }

    private DERObject transceiveDER(ACSTransceiver acsTransceiver, DERObject input) throws Exception {
        return this.transceiveDER(acsTransceiver, input, false);
    }

    private byte[] createSelectAidApdu(byte[] aid) {
        byte[] result = new byte[6 + aid.length];
        System.arraycopy(CLA_INS_P1_P2, 0, result, 0, CLA_INS_P1_P2.length);
        result[4] = (byte) aid.length;
        System.arraycopy(aid, 0, result, 5, aid.length);
        result[result.length - 1] = 0;
        return result;
    }


    private static boolean hasClass(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (final ClassNotFoundException e) {
            return false;
        }
    }


    private static boolean isExternalReaderAttached(Context context) {
        UsbManager manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        Reader reader = new Reader(manager);
        return externalReaderAttached(context, manager, reader) != null;
    }

    private static UsbDevice externalReaderAttached(Context context, UsbManager manager, Reader reader) {
        for (UsbDevice device : manager.getDeviceList().values()) {
            if (reader.isSupported(device)) {
                return device;
            }
        }
        return null;
    }

    private static void setOnStateChangedListener(final Reader reader, final ACSTransceiver transceiver, final NFCClientACSCallback callback) {

        Log.d(TAG, "set listener");

        reader.setOnStateChangeListener(new Reader.OnStateChangeListener() {
            private boolean disabledBuzzer = false;

            public void onStateChange(int slotNum, int prevState, int currState) {
                Log.d(TAG, "statechange from: " + prevState + " to: " + currState);

                if (currState == Reader.CARD_PRESENT) {
                    try {
                        transceiver.initCard(slotNum);
                        /*if(!disabledBuzzer) {
                            transceiver.disableBuzzer();
                            disabledBuzzer = true;
                        }*/
                        callback.tagDiscovered(transceiver);
                    } catch (ReaderException e) {
                        Log.e(TAG, "Could not connnect reader (ReaderException): ", e);
                        callback.tagFailed();
                    }
                } else if (currState == Reader.CARD_ABSENT) {
                    callback.nfcTagLost();
                }
            }
        });
    }

    public static Pair<ACSTransceiver, Reader> createReaderAndTransceiver(final Context context /*final ReaderOpenCallback callback,*/) throws IOException {
        UsbManager manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        Reader reader = new Reader(manager);
        UsbDevice externalDevice = externalReaderAttached(context, manager, reader);
        if (externalDevice == null) {
            throw new IOException("External device is not set");
        }

        int pid = externalDevice.getProductId();
        int vid = externalDevice.getVendorId();

        Log.d(TAG, "pid=" + pid + ", vid=" + vid);

        final int maxLen;
        final boolean acr122u;
        if (pid == 8704 && vid == 1839) {
            /*
             * 64 is the maximum due to a sequence bug in the ACR122u
			 * http://musclecard.996296
			 * .n3.nabble.com/ACR122U-response-frames-contain-wrong
			 * -sequence-numbers-td5002.html If larger than 64, then I get a
			 * com.acs.smartcard.CommunicationErrorException: The sequence number (4) is
			 * invalid.
			 *
			 * The same problem arises sometimes even with the length of 54.
			 */
            maxLen = 53;
            acr122u = true;
        } else if (pid == 8730 && vid == 1839) {
            /**
             * The ACR1251U can handle larger message, go for the same amount as the android devices, 245
             */
            maxLen = 53;
            acr122u = false;
        } else {
            throw new IOException("unknow device with pid " + pid + ":" + vid);
        }

        //ask user for permission
        Log.d(TAG, "ask user for permission");
        ACSTransceiver transceiver = new ACSTransceiver(reader, maxLen, acr122u);
        try {
            reader.open(externalDevice);
            //callback.readerOpen(reader, externalDevice, transceiver);
        } catch (IllegalArgumentException e) {
            Log.d(TAG, "could not access device, no permissions given?", e);
        }

        return new Pair<ACSTransceiver, Reader>(transceiver, reader);
    }

    private static BroadcastReceiver createBroadcastReceiver(final Reader reader /*final ReaderOpenCallback callback,*/) {
        return new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Log.d(TAG, "action: " + action);

                if (ACTION_USB_PERMISSION.equals(action)) {
                    Log.d(TAG, "try to create reader");

                    synchronized (this) {
                        UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            if (device != null) {
                                try {
                                    Log.d(TAG, "reader open");
                                    reader.open(device);
                                    //callback.readerOpen(reader, device, transceiver);
                                } catch (Exception e) {
                                    Log.e(TAG, "reader open failed", e);
                                }
                            }
                        }
                    }
                } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                    synchronized (this) {
                        UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                        if (device != null && device.equals(reader.getDevice())) {
                            reader.close();
                        }
                        Log.d(TAG, "reader detached");
                    }
                }
            }
        };
    }


}
