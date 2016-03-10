package ch.papers.payments.communications.peers.bluetooth;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.ParcelUuid;
import android.util.Log;

import org.bitcoinj.core.ECKey;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ch.papers.payments.Constants;
import ch.papers.payments.Utils;
import ch.papers.payments.communications.messages.DERObject;
import ch.papers.payments.communications.messages.DERParser;
import ch.papers.payments.communications.peers.AbstractServer;
import ch.papers.payments.communications.peers.steps.PaymentAuthorizationReceiveStep;
import ch.papers.payments.communications.peers.steps.PaymentFinalSignatureReceiveStep;
import ch.papers.payments.communications.peers.steps.PaymentRefundReceiveStep;
import ch.papers.payments.communications.peers.steps.PaymentRequestSendStep;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 04/03/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class BluetoothLEServer extends AbstractServer {
    private final static String TAG = BluetoothLEServer.class.getSimpleName();

    private final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private BluetoothGattServer bluetoothGattServer;

    public BluetoothLEServer(Context context) {
        super(context);
    }

    @Override
    public boolean isSupported() {
        // see http://stackoverflow.com/questions/26482611/chipsets-devices-supporting-android-5-ble-peripheral-mode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return this.getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) &&
                    bluetoothAdapter.isMultipleAdvertisementSupported() &&
                    bluetoothAdapter.isOffloadedFilteringSupported() &&
                    bluetoothAdapter.isOffloadedScanBatchingSupported();
        } else {
            return false;
        }
    }

    @Override
    public void start() {
        if(!this.isRunning() && this.isSupported()) {
            this.setRunning(true);

            final BluetoothManager bluetoothManager =
                    (BluetoothManager) this.getContext().getSystemService(Context.BLUETOOTH_SERVICE);

            this.bluetoothGattServer = bluetoothManager.openGattServer(this.getContext(), new BluetoothGattServerCallback() {
                class PaymentState {
                    byte[] derRequestPayload = new byte[0];
                    byte[] derResponsePayload = DERObject.NULLOBJECT.serializeToDER();
                    ECKey clientKey = null;
                    int stepCounter = 0;
                }

                private Map<String, PaymentState> connectedDevices = new ConcurrentHashMap<String, PaymentState>();

                @Override
                public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
                    super.onConnectionStateChange(device, status, newState);

                    switch (newState){
                        case BluetoothGatt.STATE_CONNECTING:
                            Log.d(TAG, device.getAddress() + " changed connection state to connecting");
                            break;
                        case BluetoothGatt.STATE_CONNECTED:
                            Log.d(TAG, device.getAddress() + " changed connection state to connected");
                            if(hasPaymentRequestUri()){
                                PaymentState paymentState = new PaymentState();
                                this.connectedDevices.put(device.getAddress(),paymentState);
                                paymentState.derResponsePayload = new PaymentRequestSendStep(getPaymentRequestUri()).process(DERObject.NULLOBJECT).serializeToDER();
                            }
                            break;
                        case BluetoothGatt.STATE_DISCONNECTING:
                            Log.d(TAG, device.getAddress() + " changed connection state to disconnecting");
                            break;
                        case BluetoothGatt.STATE_DISCONNECTED:
                            this.connectedDevices.remove(device.getAddress());
                            Log.d(TAG, device.getAddress() + " changed connection state to disconnected");
                            break;
                        default:
                            Log.d(TAG, device.getAddress() + " changed connection state to "+newState);
                            break;
                    }
                }

                @Override
                public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
                    Log.d(TAG, device.getAddress() + " requested characteristic read");
                    PaymentState paymentState = this.connectedDevices.get(device.getAddress());
                    bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, paymentState.derResponsePayload);
                    paymentState.derResponsePayload = DERObject.NULLOBJECT.serializeToDER();
                }

                @Override
                public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
                    Log.d(TAG, device.getAddress() + " requested characteristic write with " + value.length + " payload");
                    PaymentState paymentState = this.connectedDevices.get(device.getAddress());
                    paymentState.derRequestPayload = Utils.concatBytes(paymentState.derRequestPayload, value);
                    int responseLength = DERParser.extractPayloadEndIndex(paymentState.derRequestPayload);
                    if(paymentState.derRequestPayload.length>=responseLength){
                        final byte[] requestPayload = paymentState.derRequestPayload;
                        paymentState.derRequestPayload = new byte[0];
                        switch(paymentState.stepCounter++){
                            case 0:
                                final PaymentAuthorizationReceiveStep paymentAuthorizationReceiveStep = new PaymentAuthorizationReceiveStep(getPaymentRequestUri());
                                paymentState.derResponsePayload = paymentAuthorizationReceiveStep.process(DERParser.parseDER(requestPayload)).serializeToDER();
                                paymentState.clientKey = paymentAuthorizationReceiveStep.getClientPublicKey();
                                break;
                            case 1:
                                final PaymentRefundReceiveStep paymentRefundReceiveStep = new PaymentRefundReceiveStep(paymentState.clientKey);
                                paymentState.derResponsePayload = paymentRefundReceiveStep.process(DERParser.parseDER(requestPayload)).serializeToDER();
                                break;
                            case 2:
                                final PaymentFinalSignatureReceiveStep paymentFinalSignatureReceiveStep = new PaymentFinalSignatureReceiveStep(paymentState.clientKey, getPaymentRequestUri().getAddress());
                                paymentState.derResponsePayload = paymentFinalSignatureReceiveStep.process(DERParser.parseDER(requestPayload)).serializeToDER();
                                getPaymentRequestAuthorizer().onPaymentSuccess();
                                break;

                        }
                        Log.d(TAG, "sending response now");
                    }
                }
            });



            final BluetoothGattCharacteristic writeCharacteristic = new BluetoothGattCharacteristic(Constants.WRITE_CHARACTERISTIC_UUID, BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE, BluetoothGattCharacteristic.PERMISSION_WRITE);
            final BluetoothGattCharacteristic readCharacteristic = new BluetoothGattCharacteristic(Constants.READ_CHARACTERISTIC_UUID, BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ);
            final BluetoothGattService bluetoothGattService = new BluetoothGattService(Constants.SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);
            bluetoothGattService.addCharacteristic(writeCharacteristic);
            bluetoothGattService.addCharacteristic(readCharacteristic);


            bluetoothGattServer.addService(bluetoothGattService);
            bluetoothAdapter.getBluetoothLeAdvertiser().startAdvertising(new AdvertiseSettings.Builder().setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                            .setConnectable(true).setTimeout(0)
                            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_ULTRA_LOW).build(),
                    new AdvertiseData.Builder().setIncludeDeviceName(true).addServiceUuid(ParcelUuid.fromString(Constants.SERVICE_UUID.toString())).build(), new AdvertiseCallback() {
                    });
        }
    }

    @Override
    public void stop() {
        this.bluetoothGattServer.clearServices();
        this.bluetoothGattServer.close();
        this.setRunning(false);

    }

    @Override
    public void onChangePaymentRequest() {
    }
}
