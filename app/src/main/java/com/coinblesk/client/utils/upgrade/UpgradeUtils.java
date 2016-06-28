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

package com.coinblesk.client.utils.upgrade;

import android.content.Context;
import android.util.Log;

import com.coinblesk.client.BuildConfig;
import com.coinblesk.client.config.Constants;
import com.coinblesk.client.utils.SharedPrefUtils;
import com.coinblesk.util.SerializeUtils;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import org.apache.commons.io.FileUtils;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.TestNet3Params;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;


/**
 * @author Andreas Albrecht
 */
public class UpgradeUtils {
    private static final String TAG = UpgradeUtils.class.getName();

    private static boolean checkDone = false;

    public void checkUpgrade(Context context) {
        if (checkDone) {
            // skip if already done
            return;
        }

        String appVersion = SharedPrefUtils.getAppVersion(context);
        if (appVersion == null) {
            // first start OR upgrade from initial app

            if(!migrateFrom_v1_0_262(context)) {
                // first start: no version yet
                Log.d(TAG, "No appVersion yet - first launch");
            } else {
                Log.d(TAG, "No appVersion yet - upgrade from v1.0.262 / 2of2 multisig (CeBIT)");
                /* special case: ECKeys already exist, but no version
                 * - migrate from early version v1.0.262 (CeBIT)
                 * - transfer funds to new address (2of2 multisig to cltv)
                 */
                // TODO: parameters!
                try {
                    upgradeFrom_v1_0_262(context, TestNet3Params.get());
                } catch (Exception e) {
                    Log.e(TAG, "Upgrade failed: ", e);
                }
            }

        } else if (appVersion.equals(BuildConfig.VERSION_NAME)) {
            Log.d(TAG, "appVersion '" + appVersion + "' equals current version - no action required");
        } else {
            // add upgrade instructions as needed.
        }

        SharedPrefUtils.setAppVersion(context, BuildConfig.VERSION_NAME);

        checkDone = true;
    }

    private boolean migrateFrom_v1_0_262(Context context) {
        // these directories are from the object store.
        return new File(context.getFilesDir(), "testnet_wallet_" + "_uuid_object_storage").exists() ||
                new File(context.getFilesDir(), "mainnet_wallet_" + "_uuid_object_storage").exists();
    }

    private void upgradeFrom_v1_0_262(Context context, NetworkParameters migrationParams) throws IOException {
        // TODO: implement upgrade: forward from 2-of-2 to cltv
        Log.d(TAG, "********* MIGRATION FROM v1.0.262 *********");

        final File rootDir = context.getFilesDir();
        final File storageDir = new File(rootDir, "testnet_wallet_" + "_uuid_object_storage");
        final File keyFile = new File(storageDir, "ECKeyWrapper.json");
        final File walletFile = new File(rootDir, "testnet_wallet_" + "_.wallet");

        if (keyFile.exists()) {

            // Keys: stored in ECKeyWrapper.json
            /* Key format (JSON):
              {
                "...uuid1...": {
                    "isPublicOnly": true,
                    "keyPayload": [...bytes (integers)...],
                    "name": "remote_server_public_key",
                    "uuid": "...uuid1..."
                },
                "...uuid2...": {
                    "isPublicOnly": false,
                    "keyPayload": [...bytes (integers)...],
                    "name": "remote_client_public_key",
                    "uuid": "...uuid2..."
                }
              }
            */

            Log.d(TAG, "Key file found: " + keyFile);
            String json = FileUtils.readFileToString(keyFile);
            Type type = new TypeToken<Map<String, ECKeyWrapper>>(){}.getType();
            // Note: do not use gson from serializeutils (key is not stored in base64).
            Map<String, ECKeyWrapper> keys = new Gson().fromJson(json, type);
            ECKey serverKey = null;
            ECKey clientKey = null;
            for (ECKeyWrapper key : keys.values()) {
                if (key.isPublicOnly && key.name.equals("remote_server_public_key")) {
                    serverKey = ECKey.fromPublicOnly(key.keyPayload);
                } else if (!key.isPublicOnly && key.name.equals("remote_client_public_key")) {
                    clientKey = ECKey.fromPrivate(key.keyPayload);
                } else {
                    Log.d(TAG, "Unknown key name: " + key.name);
                }
            }

            if (clientKey != null && serverKey != null) {
                Log.d(TAG, "Found client and server keys - store in shared preferences.");
                try {
                    SharedPrefUtils.setClientKey(context, migrationParams, clientKey);
                    SharedPrefUtils.setServerKey(context, migrationParams, serverKey);
                    Log.d(TAG, "Migrated keys:"
                            + " clientPubKey=" + clientKey.getPublicKeyAsHex()
                            + ", serverPubKey=" + serverKey.getPublicKeyAsHex());
                } catch (Exception e) {
                    Log.d(TAG, "Exception: ", e);
                    SharedPrefUtils.setClientKey(context, migrationParams, null);
                    SharedPrefUtils.setServerKey(context, migrationParams, null);
                }
            }
        } else {
            Log.d(TAG, "Key file not found: " + keyFile);
        }

        SharedPrefUtils.enableMultisig2of2ToCltvForwarder(context);

        Log.d(TAG, "********* MIGRATION FROM v1.0.262 FINISHED *********");
    }


    private static class ECKeyWrapper {
        byte[] keyPayload;
        boolean isPublicOnly;
        String name;
    }

}
