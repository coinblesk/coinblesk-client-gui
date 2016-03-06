package ch.papers.payments.communications.peers.bluetooth;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import java.util.Arrays;

import ch.papers.payments.Constants;
import ch.papers.payments.Utils;
import ch.papers.payments.WalletService;
import ch.papers.payments.communications.messages.DERObject;
import ch.papers.payments.communications.messages.DERParser;
import ch.papers.payments.communications.peers.AbstractClient;
import ch.papers.payments.communications.peers.steps.PaymentRefundSendStep;
import ch.papers.payments.communications.peers.steps.PaymentRequestReceiveStep;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 04/03/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BluetoothLEClient extends AbstractClient {
    private final static String TAG = BluetoothLEClient.class.getSimpleName();
    private final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private final static int MAX_MTU = 300;
    private final static int MAX_FRAGMENT_SIZE = MAX_MTU - 3;
    private final PaymentRequestReceiveStep paymentRequestReceiveStep;

    public BluetoothLEClient(Context context, WalletService.WalletServiceBinder walletServiceBinder) {
        super(context, walletServiceBinder);
        paymentRequestReceiveStep = new PaymentRequestReceiveStep(walletServiceBinder.getMultisigClientKey());
    }


    @Override
    public void onIsReadyForInstantPaymentChange() {
        if(this.isReadyForInstantPayment()){
            bluetoothAdapter.startLeScan(this.leScanCallback);
        } else {
            bluetoothAdapter.stopLeScan(this.leScanCallback);
        }
    }

    private final BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            bluetoothAdapter.stopLeScan(this);
            device.connectGatt(getContext(), false, new BluetoothGattCallback() {
                byte[] derRequestPayload;
                int stepCounter = 0;

                @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                    super.onConnectionStateChange(gatt, status, newState);

                    if (newState == BluetoothGatt.STATE_CONNECTED) {
                        gatt.requestMtu(MAX_MTU);
                    }

                    switch (newState) {
                        case BluetoothGatt.STATE_CONNECTING:
                            Log.d(TAG, gatt.getDevice().getAddress() + " changed connection state to connecting");
                            break;
                        case BluetoothGatt.STATE_CONNECTED:
                            Log.d(TAG, gatt.getDevice().getAddress() + " changed connection state to connected");
                            break;
                        case BluetoothGatt.STATE_DISCONNECTING:
                            Log.d(TAG, gatt.getDevice().getAddress() + " changed connection state to disconnecting");
                            break;
                        case BluetoothGatt.STATE_DISCONNECTED:
                            Log.d(TAG, gatt.getDevice().getAddress() + " changed connection state to disconnected");
                            break;
                        default:
                            Log.d(TAG, gatt.getDevice().getAddress() + " changed connection state to " + newState);
                            break;
                    }
                }

                @Override
                public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
                    super.onMtuChanged(gatt, mtu, status);
                    gatt.discoverServices();
                    Log.d(TAG, mtu + " mtu changed");
                }

                @Override
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    super.onServicesDiscovered(gatt, status);
                    Log.d(TAG, "discovered service:" + status);

                    BluetoothGattCharacteristic readCharacteristic = gatt.getService(Constants.SERVICE_UUID).getCharacteristic(Constants.READ_CHARACTERISTIC_UUID);
                    gatt.readCharacteristic(readCharacteristic);
                    this.stepCounter = 0;
                }

                @Override
                public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    super.onCharacteristicRead(gatt, characteristic, status);
                    Log.d(TAG, gatt.getDevice().getAddress() + " read characteristic:" + status);
                    Log.d(TAG, "read receiving bytes: " + characteristic.getValue().length);

                    this.derRequestPayload = Utils.concatBytes(derRequestPayload, characteristic.getValue());
                    int responseLength = DERParser.extractPayloadEndIndex(derRequestPayload);

                    if(derRequestPayload.length>=responseLength && derRequestPayload.length!=2){
                        byte[] derResponsePayload = new byte[0];
                        final DERObject requestDER = DERParser.parseDER(derRequestPayload);
                        switch (stepCounter){
                            case 0:
                                DERObject paymentRequestResponse = paymentRequestReceiveStep.process(requestDER);
                                if(getPaymentRequestAuthorizer().isPaymentRequestAuthorized(paymentRequestReceiveStep.getBitcoinURI())) {
                                    derResponsePayload =paymentRequestResponse.serializeToDER();
                                    stepCounter++;
                                }
                                break;
                            case 1:
                                PaymentRefundSendStep paymentRefundSendStep = new PaymentRefundSendStep(getWalletServiceBinder(),paymentRequestReceiveStep.getBitcoinURI());
                                derResponsePayload = paymentRefundSendStep.process(requestDER).serializeToDER();
                                stepCounter++;
                                break;
                        }

                        int byteCounter = 0;
                        byte[] fragment;
                        while(byteCounter<derResponsePayload.length){
                            fragment = Arrays.copyOfRange(derResponsePayload,byteCounter,Math.min(derResponsePayload.length,MAX_FRAGMENT_SIZE));
                            BluetoothGattCharacteristic writeCharacteristic = gatt.getService(Constants.SERVICE_UUID).getCharacteristic(Constants.WRITE_CHARACTERISTIC_UUID);
                            writeCharacteristic.setValue(fragment);
                            gatt.writeCharacteristic(writeCharacteristic);
                            byteCounter += fragment.length;
                        }
                        this.derRequestPayload = new byte[0];
                    } else {
                        BluetoothGattCharacteristic readCharacteristic = gatt.getService(Constants.SERVICE_UUID).getCharacteristic(Constants.READ_CHARACTERISTIC_UUID);
                        gatt.readCharacteristic(readCharacteristic);
                    }
                }

                @Override
                public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    super.onCharacteristicWrite(gatt, characteristic, status);
                    Log.d(TAG, gatt.getDevice().getAddress() + " write characteristic:" + status);
                    Log.d(TAG, "write receiving bytes: " + characteristic.getValue().length);
                    BluetoothGattCharacteristic readCharacteristic = gatt.getService(Constants.SERVICE_UUID).getCharacteristic(Constants.READ_CHARACTERISTIC_UUID);
                    gatt.readCharacteristic(readCharacteristic);
                }
            });

        }
    };

    @Override
    public boolean isSupported() {
        return this.getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }
}
