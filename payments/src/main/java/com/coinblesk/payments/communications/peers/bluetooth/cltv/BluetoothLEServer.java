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

import com.coinblesk.client.config.Constants;
import com.coinblesk.client.utils.ClientUtils;
import com.coinblesk.der.DERObject;
import com.coinblesk.der.DERParser;
import com.coinblesk.payments.WalletService;
import com.coinblesk.payments.communications.PaymentException;
import com.coinblesk.payments.communications.peers.AbstractServer;
import com.coinblesk.payments.communications.steps.cltv.PaymentRequestSendStep;
import com.coinblesk.payments.communications.steps.cltv.PaymentResponseReceiveStep;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Andreas Albrecht
 * @author Alessandro De Carli
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class BluetoothLEServer extends AbstractServer {
    private final static String TAG = BluetoothLEServer.class.getName();

    private final BluetoothAdapter bluetoothAdapter;
    private BluetoothGattServer bluetoothGattServer;
    private List<AdvertiseCallback> bluetoothLeAdvertiseCallbacks;
    private final Map<String, PaymentState> connectedDevices;

    public BluetoothLEServer(Context context, WalletService.WalletServiceBinder walletServiceBinder) {
        super(context, walletServiceBinder);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        connectedDevices = new ConcurrentHashMap<>();
    }

    @Override
    public boolean isSupported() {
        // see http://stackoverflow.com/questions/26482611/chipsets-devices-supporting-android-5-ble-peripheral-mode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) &&
                    bluetoothAdapter != null &&
                    bluetoothAdapter.isMultipleAdvertisementSupported() &&
                    bluetoothAdapter.isOffloadedFilteringSupported() &&
                    bluetoothAdapter.isOffloadedScanBatchingSupported();
        }

        return false;
    }

    @Override
    public void onStart() {
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getContext().getSystemService(Context.BLUETOOTH_SERVICE);

        synchronized (bluetoothManager) {

            bluetoothGattServer = bluetoothManager.openGattServer(getContext(), new ServerCallback());

            final BluetoothGattCharacteristic writeCharacteristic1 = new BluetoothGattCharacteristic(
                    Constants.BLUETOOTH_WRITE_CHARACTERISTIC_UUID,
                    BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                    BluetoothGattCharacteristic.PERMISSION_WRITE);
            final BluetoothGattCharacteristic readCharacteristic1 = new BluetoothGattCharacteristic(
                    Constants.BLUETOOTH_READ_CHARACTERISTIC_UUID,
                    BluetoothGattCharacteristic.PROPERTY_READ,
                    BluetoothGattCharacteristic.PERMISSION_READ);

            final BluetoothGattCharacteristic writeCharacteristic2 = new BluetoothGattCharacteristic(
                    Constants.BLUETOOTH_WRITE_CHARACTERISTIC_UUID,
                    BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                    BluetoothGattCharacteristic.PERMISSION_WRITE);
            final BluetoothGattCharacteristic readCharacteristic2 = new BluetoothGattCharacteristic(
                    Constants.BLUETOOTH_READ_CHARACTERISTIC_UUID,
                    BluetoothGattCharacteristic.PROPERTY_READ,
                    BluetoothGattCharacteristic.PERMISSION_READ);

            final BluetoothGattService bluetoothGattService = new BluetoothGattService(
                    Constants.BLUETOOTH_SERVICE_UUID,
                    BluetoothGattService.SERVICE_TYPE_PRIMARY);
            bluetoothGattService.addCharacteristic(writeCharacteristic1);
            bluetoothGattService.addCharacteristic(readCharacteristic1);

            final UUID pubKeyUuid = UUID.nameUUIDFromBytes(getWalletServiceBinder().getMultisigClientKey().getPubKey());
            Log.d(TAG, "pub key: " + pubKeyUuid);
            final BluetoothGattService bluetoothPubKeyGattService = new BluetoothGattService(
                    pubKeyUuid, BluetoothGattService.SERVICE_TYPE_PRIMARY);
            if (!bluetoothPubKeyGattService.addCharacteristic(writeCharacteristic2)) {
                Log.d(TAG, "could not add write characteristic: " + writeCharacteristic2);
            }
            if (!bluetoothPubKeyGattService.addCharacteristic(readCharacteristic2)) {
                Log.d(TAG, "could not add read characteristic: " + readCharacteristic2);
            }


            //bluetoothGattService.addService(bluetoothPubKeyGattService);

            if (!bluetoothGattServer.addService(bluetoothGattService)) {
                Log.d(TAG, "could not add service1: " + bluetoothGattService);
            }

            //hack, throws null pointer when adding a second service, but only on the nexus 9
            while(true) {
                try {
                    if (!bluetoothGattServer.addService(bluetoothPubKeyGattService)) {
                        Log.d(TAG, "could not add service2: " + bluetoothGattService);
                    } else {
                        break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Log.d(TAG, "Bluetooth LE Default Service UUID: " + Constants.BLUETOOTH_SERVICE_UUID.toString());
            Log.d(TAG, "Bluetooth LE PubKey Service UUID: " + pubKeyUuid.toString());
            startAdvertisingService(Constants.BLUETOOTH_SERVICE_UUID);
            startAdvertisingService(pubKeyUuid);
        }
    }

    private void startAdvertisingService(final UUID uuid) {
        if (bluetoothLeAdvertiseCallbacks == null) {
            bluetoothLeAdvertiseCallbacks = new ArrayList<>();
        }

        AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                Log.d(TAG, "BLE advertising - uuid: " + uuid.toString()
                        + " - onStartSuccess - settings: " + settingsInEffect.toString());
            }

            @Override
            public void onStartFailure(int errorCode) {
                Log.w(TAG, "BLE advertising - uuid: " + uuid.toString()
                        + " - onStartFailure - errorCode=" + errorCode);
            }
        };
        bluetoothLeAdvertiseCallbacks.add(advertiseCallback);

        bluetoothAdapter.getBluetoothLeAdvertiser().startAdvertising(
                new AdvertiseSettings.Builder()
                        .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                        .setConnectable(true)
                        .setTimeout(0)
                        .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                        .build(),
                new AdvertiseData.Builder()
                        .setIncludeDeviceName(true)
                        .addServiceUuid(new ParcelUuid(uuid))
                        .build(),
                advertiseCallback
        );
    }

    @Override
    public void onStop() {
        if (bluetoothGattServer != null) {
            bluetoothGattServer.clearServices();
            bluetoothGattServer.close();
            bluetoothGattServer = null;
        }

        if (bluetoothLeAdvertiseCallbacks != null) {
            if (bluetoothAdapter != null) {
                for (AdvertiseCallback callback : bluetoothLeAdvertiseCallbacks) {
                    bluetoothAdapter.getBluetoothLeAdvertiser().stopAdvertising(callback);
                }
            }
            bluetoothLeAdvertiseCallbacks.clear();
        }

        connectedDevices.clear();
    }

    private class PaymentState {
        byte[] derRequestPayload = new byte[0];
        byte[] derResponsePayload = DERObject.NULLOBJECT.serializeToDER();
        int stepCounter = 0;
        long startTime;

        private byte[] getNextFragment() {
            int fragLen = Math.min(derResponsePayload.length, BluetoothLE.MAX_FRAGMENT_SIZE);
            byte[] fragment = Arrays.copyOfRange(derResponsePayload, 0, fragLen);
            derResponsePayload = Arrays.copyOfRange(derResponsePayload, fragment.length, derResponsePayload.length);
            return fragment;
        }
    }

    private class ServerCallback extends  BluetoothGattServerCallback {

        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);

            String newStateStr;
            switch (newState) {
                case BluetoothGatt.STATE_CONNECTING:
                    newStateStr = "CONNECTING";
                    break;
                case BluetoothGatt.STATE_CONNECTED:
                    newStateStr = "CONNECTED";
                    paymentRequestSend(device);
                    break;
                case BluetoothGatt.STATE_DISCONNECTING:
                    newStateStr = "DISCONNECTING";
                    break;
                case BluetoothGatt.STATE_DISCONNECTED:
                    newStateStr = "DISCONNECTED";
                    PaymentState removed = connectedDevices.remove(device.getAddress());
                    if (removed != null) {
                        Log.d(TAG, "Removed " + device.getAddress() +
                                " from payment states (next step would have been: " + removed.stepCounter + ")");
                    }
                    break;
                default:
                    newStateStr = "STATE_" + status;
                    break;
            }
            Log.d(TAG, String.format("%s - changed connection state to %s (%d)",
                    device.getAddress(), newStateStr, status));
        }

        private void paymentRequestSend(BluetoothDevice device) {
            if (!hasPaymentRequestUri()) {
                return;
            }

            PaymentState paymentState = new PaymentState();
            paymentState.startTime = System.currentTimeMillis();

            connectedDevices.put(device.getAddress(), paymentState);

            Log.d(TAG, String.format("Send payment request to device=%s, startTime(ms)=%d",
                    device.getAddress(), paymentState.startTime));

            PaymentRequestSendStep paymentRequestSend = new PaymentRequestSendStep(
                    getPaymentRequestUri(), getWalletServiceBinder().getMultisigClientKey());
            try {
                DERObject paymentRequest = paymentRequestSend.process(DERObject.NULLOBJECT);
                paymentState.derResponsePayload = paymentRequest.serializeToDER();
            } catch (PaymentException e) {
                // TODO handle!
                Log.e(TAG, "Exception: ", e);
            }
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
                                                    BluetoothGattCharacteristic characteristic) {

            PaymentState paymentState = connectedDevices.get(device.getAddress());
            byte[] fragment = paymentState.getNextFragment();
            Log.d(TAG, String.format("%s - onCharacteristicReadRequest - fragment length=%d bytes, duration=%d ms",
                    device.getAddress(), fragment.length, (System.currentTimeMillis()-paymentState.startTime)));

            bluetoothGattServer.sendResponse(
                    device,
                    requestId,
                    BluetoothGatt.GATT_SUCCESS,
                    0, /* offset */
                    fragment);

            if (paymentState.derResponsePayload.length == 0) {
                paymentState.derResponsePayload = DERObject.NULLOBJECT.serializeToDER();
            }
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId,
                                                            BluetoothGattCharacteristic characteristic,
                                                            boolean preparedWrite,
                                                            boolean responseNeeded,
                                                            int offset,
                                                            byte[] value) {
            try {
                PaymentState paymentState = connectedDevices.get(device.getAddress());
                long duration;

                Log.d(TAG, String.format("%s - onCharacteristicWriteRequest - length=%d bytes, duration=%d ms",
                        device.getAddress(), value.length, (System.currentTimeMillis()-paymentState.startTime)));

                paymentState.derRequestPayload = ClientUtils.concatBytes(paymentState.derRequestPayload, value);
                int responseLength = DERParser.extractPayloadEndIndex(paymentState.derRequestPayload);

                if (paymentState.derRequestPayload.length >= responseLength) {
                    final byte[] requestPayload = paymentState.derRequestPayload;
                    paymentState.derRequestPayload = new byte[0];
                    switch (paymentState.stepCounter++) {

                        case 0:
                            duration = System.currentTimeMillis() - paymentState.startTime;
                            Log.d(TAG, String.format("%s - got payment response (%d ms since startTime)",
                                    device.getAddress(), duration));

                            DERObject paymentResponse = DERParser.parseDER(requestPayload);
                            PaymentResponseReceiveStep paymentResponseReceive = new PaymentResponseReceiveStep(
                                    getPaymentRequestUri(), getWalletServiceBinder());
                            DERObject serverSignatures = paymentResponseReceive.process(paymentResponse);
                            paymentState.derResponsePayload = serverSignatures.serializeToDER();

                            duration = System.currentTimeMillis() - paymentState.startTime;
                            Log.d(TAG, String.format("%s - send server response (%d ms since startTime)",
                                    device.getAddress(), duration));
                            break;

                        case 1:
                            duration = System.currentTimeMillis() - paymentState.startTime;
                            Log.d(TAG,  String.format("%s - payment finished - total duration: %d",
                                    device.getAddress(), duration));

                            getPaymentRequestDelegate().onPaymentSuccess();
                            break;
                    }
                }
            } catch (PaymentException e) {
                // TODO: handle exception
                Log.w(TAG, "Exception: ", e);
            }
        }
    }
}