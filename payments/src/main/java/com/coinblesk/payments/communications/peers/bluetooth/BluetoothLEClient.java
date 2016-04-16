package com.coinblesk.payments.communications.peers.bluetooth;

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

import com.coinblesk.payments.Constants;
import com.coinblesk.payments.Utils;
import com.coinblesk.payments.WalletService;
import com.coinblesk.payments.communications.messages.DERObject;
import com.coinblesk.payments.communications.messages.DERParser;
import com.coinblesk.payments.communications.peers.AbstractClient;
import com.coinblesk.payments.communications.peers.steps.PaymentFinalSignatureSendStep;
import com.coinblesk.payments.communications.peers.steps.PaymentRefundSendStep;
import com.coinblesk.payments.communications.peers.steps.PaymentRequestReceiveStep;
import com.coinblesk.payments.models.RefundTransactionWrapper;

import org.bitcoinj.core.Transaction;

import java.util.Arrays;
import java.util.UUID;

import ch.papers.objectstorage.UuidObjectStorage;
import ch.papers.objectstorage.UuidObjectStorageException;
import ch.papers.objectstorage.listeners.OnResultListener;

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
        this.paymentRequestReceiveStep = new PaymentRequestReceiveStep(walletServiceBinder);
    }

    private final BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            bluetoothAdapter.stopLeScan(this);
            device.connectGatt(getContext(), false, new BluetoothGattCallback() {
                Transaction fullsignedTransaction;
                Transaction halfsignedRefundTransaction;
                byte[] derRequestPayload;
                byte[] derResponsePayload;
                int byteCounter = 0;
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
                    Log.d(TAG, mtu + " mtu changed" + this);
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

                    if (derRequestPayload.length >= responseLength && derRequestPayload.length != 2) {
                        derResponsePayload = new byte[0];
                        final DERObject requestDER = DERParser.parseDER(derRequestPayload);
                        switch (stepCounter++) {
                            case 0:
                                DERObject paymentRequestResponse = paymentRequestReceiveStep.process(requestDER);
                                if (getPaymentRequestDelegate().isPaymentRequestAuthorized(paymentRequestReceiveStep.getBitcoinURI())) {
                                    derResponsePayload = paymentRequestResponse.serializeToDER();
                                } else {
                                    getPaymentRequestDelegate().onPaymentError("unauthorized");
                                }
                                break;
                            case 1:
                                PaymentRefundSendStep paymentRefundSendStep = new PaymentRefundSendStep(getWalletServiceBinder(), paymentRequestReceiveStep.getBitcoinURI(), paymentRequestReceiveStep.getTimestamp());
                                derResponsePayload = paymentRefundSendStep.process(requestDER).serializeToDER();
                                fullsignedTransaction = paymentRefundSendStep.getFullSignedTransaction();
                                halfsignedRefundTransaction = paymentRefundSendStep.getHalfSignedRefundTransaction();
                                break;
                            case 2:
                                final PaymentFinalSignatureSendStep paymentFinalSignatureSendStep = new PaymentFinalSignatureSendStep(getWalletServiceBinder(), fullsignedTransaction, halfsignedRefundTransaction);
                                // payment was successful in every way, commit that tx
                                getWalletServiceBinder().commitAndBroadcastTransaction(fullsignedTransaction);
                                final Transaction fullsignedRefundTransaction = paymentFinalSignatureSendStep.getFullSignedRefundTransation();
                                UuidObjectStorage.getInstance().addEntry(new RefundTransactionWrapper(fullsignedRefundTransaction), new OnResultListener<RefundTransactionWrapper>() {
                                    @Override
                                    public void onSuccess(RefundTransactionWrapper refundTransactionWrapper) {
                                        try {

                                            UuidObjectStorage.getInstance().commit();
                                        } catch (UuidObjectStorageException e) {
                                            e.printStackTrace();
                                        }
                                    }

                                    @Override
                                    public void onError(String s) {
                                        Log.d(TAG, s);
                                    }
                                }, RefundTransactionWrapper.class);

                                derResponsePayload = paymentFinalSignatureSendStep.process(requestDER).serializeToDER();
                                break;
                        }
                        this.derRequestPayload = new byte[0];
                        this.byteCounter = 0;

                        writeNextFragment(gatt);

                        if (stepCounter == 3) {
                            getPaymentRequestDelegate().onPaymentSuccess();
                        }
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

                    if (byteCounter < derResponsePayload.length) {
                        writeNextFragment(gatt);
                    } else {
                        BluetoothGattCharacteristic readCharacteristic = gatt.getService(Constants.SERVICE_UUID).getCharacteristic(Constants.READ_CHARACTERISTIC_UUID);
                        gatt.readCharacteristic(readCharacteristic);
                    }
                }

                private void writeNextFragment(BluetoothGatt gatt) {
                    final byte[] fragment = Arrays.copyOfRange(derResponsePayload, byteCounter, byteCounter + Math.min(derResponsePayload.length, MAX_FRAGMENT_SIZE));
                    Log.d(TAG, "write characteristics:" + fragment.length + "/" + derResponsePayload.length);
                    BluetoothGattCharacteristic writeCharacteristic = gatt.getService(Constants.SERVICE_UUID).getCharacteristic(Constants.WRITE_CHARACTERISTIC_UUID);
                    writeCharacteristic.setValue(fragment);
                    gatt.writeCharacteristic(writeCharacteristic);
                    byteCounter += fragment.length;
                }
            });

        }
    };

    @Override
    public boolean isSupported() {
        return this.getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    @Override
    protected void onStart() {
        bluetoothAdapter.startLeScan(new UUID[]{Constants.SERVICE_UUID}, this.leScanCallback);
    }

    @Override
    protected void onStop() {
        bluetoothAdapter.stopLeScan(this.leScanCallback);
    }
}
