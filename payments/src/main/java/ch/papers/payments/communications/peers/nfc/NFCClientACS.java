package ch.papers.payments.communications.peers.nfc;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import com.acs.smartcard.Reader;
import com.acs.smartcard.ReaderException;
import com.coinblesk.util.Pair;

import org.spongycastle.asn1.x509.Extension;

import java.io.IOException;
import java.util.Arrays;

import ch.papers.payments.Utils;
import ch.papers.payments.WalletService;
import ch.papers.payments.communications.messages.DERObject;
import ch.papers.payments.communications.messages.DERParser;
import ch.papers.payments.communications.peers.AbstractClient;
import ch.papers.payments.communications.peers.steps.PaymentFinalSignatureSendStep;
import ch.papers.payments.communications.peers.steps.PaymentRefundSendStep;
import ch.papers.payments.communications.peers.steps.PaymentRequestReceiveStep;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 28/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */

@TargetApi(Build.VERSION_CODES.KITKAT)
public class NFCClientACS extends AbstractClient {
    private final static String TAG = NFCClientACS.class.getSimpleName();

    private static final byte[] CLA_INS_P1_P2 = {(byte) 0x00, (byte) 0xA4, (byte) 0x04, (byte) 0x00};
    private static final byte[] AID_ANDROID = {(byte) 0xF0, 0x0C, 0x01, 0x04, 0x0B, 0x01, 0x03};

    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

    private final Activity activity;

    private final PaymentRequestReceiveStep paymentRequestReceiveStep;

    private Reader reader;
    private BroadcastReceiver broadcastReceiver;

    public NFCClientACS(Activity activity, WalletService.WalletServiceBinder walletServiceBinder) {
        super(activity, walletServiceBinder);
        this.activity = activity;
        this.paymentRequestReceiveStep = new PaymentRequestReceiveStep(walletServiceBinder);
        if(isSupported()) {
            UsbManager manager = (UsbManager) activity.getSystemService(Context.USB_SERVICE);
            PendingIntent permissionIntent = PendingIntent.getBroadcast(activity, 0, new Intent(ACTION_USB_PERMISSION), 0);
            Reader reader = new Reader(manager);
            UsbDevice externalDevice = externalReaderAttached(activity, manager, reader);
            manager.requestPermission(externalDevice, permissionIntent);
        }
    }

    @Override
    public boolean isSupported() {
        return hasClass("com.acs.smartcard.Reader") && isExternalReaderAttached(activity);
    }

    @Override
    public void start() {
        if(this.isRunning()) {
            Log.d(TAG, "Already turned on ACS");
        }

        try {
            Pair<ACSTransceiver, Reader> pair = createReaderAndTransceiver(getContext());
            this.reader = pair.element1();
            setOnStateChangedListener(reader, pair.element0(), new NFCClientACSCallback() {
                @Override
                public void tagDiscovered(ACSTransceiver transceiver) {
                    if(isReadyForInstantPayment()){
                        try {
                            DERObject paymentRequestInput = transceiveDER(transceiver, DERObject.NULLOBJECT, true);
                            DERObject authorizationResponseOutput = paymentRequestReceiveStep.process(paymentRequestInput);

                            Log.d(TAG, "got request, authorizing user");
                            DERObject refundSendInput = transceiveDER(transceiver, authorizationResponseOutput);
                            PaymentRefundSendStep paymentRefundSendStep = new PaymentRefundSendStep(getWalletServiceBinder(), paymentRequestReceiveStep.getBitcoinURI(), paymentRequestReceiveStep.getTimestamp());
                            DERObject refundSendOutput = paymentRefundSendStep.process(refundSendInput);


                            DERObject finalSendInput = transceiveDER(transceiver, refundSendOutput);
                            PaymentFinalSignatureSendStep paymentFinalSignatureSendStep = new PaymentFinalSignatureSendStep(getWalletServiceBinder(), paymentRequestReceiveStep.getBitcoinURI().getAddress(), paymentRefundSendStep.getFullSignedTransaction(), paymentRefundSendStep.getHalfSignedRefundTransaction());
                            DERObject sendFinalSignatureOutput = paymentFinalSignatureSendStep.process(finalSendInput);

                            transceiveDER(transceiver, sendFinalSignatureOutput);
                            getPaymentRequestAuthorizer().onPaymentSuccess();
                        } catch (Exception e){}
                    }
                }

                @Override
                public void tagFailed() {

                }

                @Override
                public void nfcTagLost() {

                }
            });
            IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_USB_PERMISSION);
            filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
            broadcastReceiver = createBroadcastReceiver(reader /*, callback*/);
            activity.registerReceiver(broadcastReceiver, filter);
        } catch (IOException e) {
            Log.e(TAG, "unable to create reader", e);
            return;
        }

        this.setRunning(true);
    }

    @Override
    public void stop() {
        if(!this.isRunning()) {
            Log.d(TAG, "Already turned off ACS");
        }
        Log.d(TAG, "Turn off ACS");
        if (reader != null && reader.isOpened()) {
               reader.close();
               reader = null;
               Log.d(TAG, "Reader closed");
        }
        activity.unregisterReceiver(broadcastReceiver);
        this.setRunning(false);
    }

    @Override
    public void onIsReadyForInstantPaymentChange() {
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
                fragment = createSelectAidApdu(AID_ANDROID);
            }

            fragment = Utils.concatBytes(fragment, Arrays.copyOfRange(derPayload, fragmentByte, Math.min(derPayload.length, fragmentByte + 53)));
            derResponse = Utils.concatBytes(derResponse, acsTransceiver.write(fragment));
            fragmentByte += fragment.length;
            Log.d(TAG, "fragment size:" + fragment.length);
        }

        int responseLength = DERParser.extractPayloadEndIndex(derResponse);
        Log.d(TAG, "expected response lenght:" + responseLength);
        Log.d(TAG, "actual response lenght:" + derResponse.length);

        while (derResponse.length < responseLength) {
            derResponse = Utils.concatBytes(derResponse, acsTransceiver.write(DERObject.NULLOBJECT.serializeToDER()));
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
        try  {
            Class.forName(className);
            return true;
        }  catch (final ClassNotFoundException e) {
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
                Log.d(TAG, "statechange from: "+prevState+" to: "+ currState);

                if (currState == Reader.CARD_PRESENT) {
                    try {
                        transceiver.initCard(slotNum);
                        if(!disabledBuzzer) {
                            transceiver.disableBuzzer();
                            disabledBuzzer = true;
                        }
                        callback.tagDiscovered(transceiver);
                    } catch (ReaderException e) {
                        Log.e(TAG, "Could not connnect reader (ReaderException): ", e);
                        callback.tagFailed();
                    }
                } else if(currState == Reader.CARD_ABSENT) {
                    callback.nfcTagLost();
                }
            }
        });
    }

    public static Pair<ACSTransceiver, Reader> createReaderAndTransceiver(final Context context /*final ReaderOpenCallback callback,*/ ) throws IOException {
        UsbManager manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        Reader reader = new Reader(manager);
        UsbDevice externalDevice = externalReaderAttached(context, manager, reader);
        if (externalDevice == null) {
            throw new IOException("External device is not set");
        }

        int pid = externalDevice.getProductId();
        int vid = externalDevice.getVendorId();

        Log.d(TAG, "pid=" + pid + ", vid="+ vid);

        final int maxLen;
        final boolean acr122u;
        if(pid == 8704 && vid == 1839) {
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
        } else if(pid == 8730 && vid == 1839) {
            /**
             * The ACR1251U can handle larger message, go for the same amount as the android devices, 245
             */
            maxLen = 53;
            acr122u = false;
        } else {
            throw new IOException("unknow device with pid "+pid+":"+vid);
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

    public static class ACSTransceiver {

        final private Reader reader;
        final private int maxLen;
        final private boolean acr122u;

        private ACSTransceiver(Reader reader, final int maxLen, boolean acr122u) {
            this.reader = reader;
            this.maxLen = maxLen;
            this.acr122u = acr122u;
        }

        private void disableBuzzer() throws ReaderException {
            if(!acr122u) {
                return;
            }
            // Disable the standard buzzer when a tag is detected (Section 6.7). It sounds
            // immediately after placing a tag resulting in people lifting the tag off before
            // we've had a chance to read the ID.
            byte[] sendBuffer = {(byte) 0xFF, (byte) 0x00, (byte) 0x52, (byte) 0x00, (byte) 0x00};
            byte[] recvBuffer = new byte[8];
            int length = reader.transmit(0, sendBuffer, sendBuffer.length, recvBuffer, recvBuffer.length);
            if (length != 8) {
                throw new RuntimeException("unexcpeted number of bytes");
            }
        }

		/*private String firwware() throws ReaderException {
			byte[] sendBuffer={(byte)0xFF, (byte)0x00, (byte)0x48, (byte)0x00, (byte)0x00};
			byte[] recvBuffer=new byte[10];
			int length = reader.transmit(0, sendBuffer, sendBuffer.length, recvBuffer, recvBuffer.length);
			if(length != 10) {
				nfcInit.tagFailed(NfcEvent.INIT_FAILED.name());
			}
			return new String(recvBuffer);
		}*/

        private void initCard(final int slotNum) throws ReaderException {
            reader.power(slotNum, Reader.CARD_WARM_RESET);
            reader.setProtocol(slotNum, Reader.PROTOCOL_T0 | Reader.PROTOCOL_T1);
        }

        public byte[] write(byte[] input) throws Exception {
            if (!reader.isOpened()) {
                Log.d(TAG, "could not write message, reader is not or no longe open");
                throw new IOException("NFCTRANSCEIVER_NOT_CONNECTED");
            }

            if (input.length > maxLen) {
                throw new IOException("The message length exceeds the maximum capacity of " + maxLen + " bytes.");
            }

            final byte[] recvBuffer = new byte[maxLen];
            final int length;
            try {
                Log.d(TAG, "write bytes: " + Arrays.toString(input));
                length = reader.transmit(0, input, input.length, recvBuffer, recvBuffer.length);
            } catch (ReaderException e) {
                Log.d(TAG, "could not write message - ReaderException", e);
                throw new IOException("UNEXPECTED_ERROR");
            }

            if (length <= 0) {
                Log.d(TAG, "could not write message - return value is 0");
                //most likely due to tag lost
                return null;
            }

            byte[] received = new byte[length];
            System.arraycopy(recvBuffer, 0, received, 0, length);
            return received;
        }
    }
}
