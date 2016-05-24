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

package com.coinblesk.payments.communications.peers.bluetooth.cltv;

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

import com.coinblesk.client.config.Constants;
import com.coinblesk.client.utils.ClientUtils;
import com.coinblesk.payments.WalletService;
import com.coinblesk.der.DERObject;
import com.coinblesk.der.DERParser;
import com.coinblesk.payments.communications.peers.AbstractClient;
import com.coinblesk.payments.communications.peers.steps.cltv.PaymentFinalizeStep;
import com.coinblesk.payments.communications.peers.steps.cltv.PaymentRequestReceiveStep;
import com.coinblesk.payments.communications.peers.steps.cltv.PaymentResponseSendStep;
import com.coinblesk.payments.communications.peers.steps.cltv.PaymentServerSignatureReceiveStep;

import org.bitcoinj.core.Transaction;
import org.bitcoinj.uri.BitcoinURI;

import java.util.Arrays;
import java.util.UUID;


/**
 * @author Andreas Albrecht
 * @author Alessandro De Carli
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BluetoothLEClient extends AbstractClient {
    private final static String TAG = BluetoothLEClient.class.getName();

    private final BluetoothAdapter bluetoothAdapter;

    public BluetoothLEClient(Context context, WalletService.WalletServiceBinder walletServiceBinder) {
        super(context, walletServiceBinder);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    public boolean isSupported() {
        return getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    @Override
    protected void onStart() {
        bluetoothAdapter.startLeScan(new UUID[]{Constants.BLUETOOTH_SERVICE_UUID}, leScanCallback);
    }

    @Override
    protected void onStop() {
        bluetoothAdapter.stopLeScan(leScanCallback);
    }

    private final BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            bluetoothAdapter.stopLeScan(this);
            device.connectGatt(getContext(), false, new ClientCallback());
        }
    };

    private class ClientCallback extends BluetoothGattCallback {

        private PaymentRequestReceiveStep paymentRequestReceive;
        private PaymentResponseSendStep paymentResponseSend;
        private PaymentServerSignatureReceiveStep paymentServerSignatures;

        private byte[] derRequestPayload;
        private byte[] derResponsePayload;
        private int byteCounter = 0;
        private int stepCounter = 0;
        private boolean isPaymentDone;

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            String newStateStr;
            switch (newState) {
                case BluetoothGatt.STATE_CONNECTING:
                    newStateStr = "CONNECTING";
                    break;
                case BluetoothGatt.STATE_CONNECTED:
                    newStateStr = "CONNECTED";
                    gatt.requestMtu(BluetoothLE.MAX_MTU);
                    break;
                case BluetoothGatt.STATE_DISCONNECTING:
                    newStateStr = "DISCONNECTING";
                    break;
                case BluetoothGatt.STATE_DISCONNECTED:
                    newStateStr = "DISCONNECTED";
                    gatt.close();
                    break;
                default:
                    newStateStr = "STATE_" + status;
                    break;
            }
            Log.d(TAG, String.format("%s - changed connection state to %s (%d)",
                    gatt.getDevice().getAddress(), newStateStr, status));
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            gatt.discoverServices();
            Log.d(TAG, String.format("onMtuChanged - mtu=%d, status=%d, %s", mtu, status, this));
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            Log.d(TAG, String.format("onServicesDiscovered - status=%d, deviceAddress=%s",
                    status, gatt.getDevice().getAddress()));
            stepCounter = 0;
            isPaymentDone = false;
            doReadCharacteristic(gatt);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            Log.d(TAG, String.format("%s - onCharacteristicRead - status=%d, length=%d bytes",
                    gatt.getDevice().getAddress(), status, characteristic.getValue().length));

            derRequestPayload = ClientUtils.concatBytes(derRequestPayload, characteristic.getValue());
            int responseLength = DERParser.extractPayloadEndIndex(derRequestPayload);

            if (derRequestPayload.length >= responseLength && derRequestPayload.length != 2) {
                derResponsePayload = new byte[0];
                switch (stepCounter++) {
                    case 0:
                        DERObject paymentRequest = DERParser.parseDER(derRequestPayload);
                        /* 1. RECEIVE PAYMENT REQUEST */
                        paymentRequestReceive = new PaymentRequestReceiveStep();
                        paymentRequestReceive.process(paymentRequest);

                        /* 2. AUTHORIZE REQUEST (by user) */
                        BitcoinURI paymentRequestURI = paymentRequestReceive.getBitcoinURI();
                        Log.d(TAG, "got request, authorizing user: " + paymentRequestURI);
                        boolean isAuthorized = getPaymentRequestDelegate().isPaymentRequestAuthorized(paymentRequestURI);
                        if (!isAuthorized) {
                            Log.d(TAG, "Payment not authorized.");
                            // TODO: we should notify server about this?
                            getPaymentRequestDelegate().onPaymentError("unauthorized");
                            return;
                        }

                        /* 3. SEND PAYMENT RESPONSE */
                        paymentResponseSend = new PaymentResponseSendStep(paymentRequestURI, getWalletServiceBinder());
                        DERObject paymentResponse = paymentResponseSend.process(DERObject.NULLOBJECT);
                        derResponsePayload = paymentResponse.serializeToDER();

                        break;
                    case 1:
                        /* 4. RECEIVE SIGANTURES */
                        DERObject serverSignatures = DERParser.parseDER(derRequestPayload);
                        paymentServerSignatures = new PaymentServerSignatureReceiveStep();
                        paymentServerSignatures.process(serverSignatures);

                        /* 5. FINALIZE PAYMENT (TX) */
                        PaymentFinalizeStep finalizeStep = new PaymentFinalizeStep(
                                paymentRequestReceive.getBitcoinURI(),
                                paymentResponseSend.getTransaction(),
                                paymentResponseSend.getClientTransactionSignatures(),
                                paymentServerSignatures.getServerTransactionSignatures(),
                                getWalletServiceBinder());
                        finalizeStep.process(DERObject.NULLOBJECT);

                        Transaction transaction = finalizeStep.getTransaction();
                        getWalletServiceBinder().commitAndBroadcastTransaction(transaction);

                        isPaymentDone = true;
                        derResponsePayload = DERObject.NULLOBJECT.serializeToDER(); // final ack.
                        break;
                }

                derRequestPayload = new byte[0];
                byteCounter = 0;
                writeNextFragment(gatt);

            } else {
                doReadCharacteristic(gatt);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            Log.d(TAG, String.format("%s - onCharacteristicWrite - status=%d, length=%d bytes",
                    gatt.getDevice().getAddress(), status, characteristic.getValue().length));

            if (byteCounter < derResponsePayload.length) {
                writeNextFragment(gatt);
            } else if (!isPaymentDone) {
                doReadCharacteristic(gatt);
            } else if (isPaymentDone) {
                // done and nothing to send.
                Log.d(TAG, "payment successful!");
                getPaymentRequestDelegate().onPaymentSuccess();
            } else {
                Log.e(TAG, "onCharacteristicWrite - unknown state - do nothing");
            }
        }

        private void writeNextFragment(BluetoothGatt gatt) {
            int toSend = Math.min(derResponsePayload.length, BluetoothLE.MAX_FRAGMENT_SIZE);
            int end = byteCounter + toSend;
            byte[] fragment = Arrays.copyOfRange(derResponsePayload, byteCounter, end);
            byteCounter += fragment.length;
            Log.d(TAG, String.format("writeNextFragment - fragment length=%d, sent=%d, total=%d bytes",
                    fragment.length, byteCounter, derResponsePayload.length));
            doWriteCharacteristic(gatt, fragment);
        }

        private boolean doReadCharacteristic(BluetoothGatt gatt) {
            BluetoothGattCharacteristic readCharacteristic = gatt
                    .getService(Constants.BLUETOOTH_SERVICE_UUID)
                    .getCharacteristic(Constants.BLUETOOTH_READ_CHARACTERISTIC_UUID);
            return gatt.readCharacteristic(readCharacteristic);
        }

        private boolean doWriteCharacteristic(BluetoothGatt gatt, byte[] value) {
            BluetoothGattCharacteristic writeCharacteristic = gatt
                    .getService(Constants.BLUETOOTH_SERVICE_UUID)
                    .getCharacteristic(Constants.BLUETOOTH_WRITE_CHARACTERISTIC_UUID);
            writeCharacteristic.setValue(value);
            return gatt.writeCharacteristic(writeCharacteristic);
        }
    }
}
