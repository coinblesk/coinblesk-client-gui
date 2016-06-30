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
import com.coinblesk.client.utils.ClientUtils;
import com.coinblesk.client.utils.SharedPrefUtils;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import org.apache.commons.io.FileUtils;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.MainNetParams;
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

    public void checkUpgrade(Context context, NetworkParameters params) {
        if (checkDone) {
            // skip if already done
            return;
        }

        String appVersion = SharedPrefUtils.getAppVersion(context);
        if (appVersion == null) {
            // first start - no app version yet.
            Log.d(TAG, "No appVersion yet - first launch");
        } else if (appVersion.equals(BuildConfig.VERSION_NAME)) {
            Log.d(TAG, "appVersion '" + appVersion + "' equals current version - no action required");
        } else {
            // add upgrade instructions as needed.
        }

        if(migrateFrom_v1_0_262(context)) {
            Log.d(TAG, "Migrate from v1.0.262 / 2of2 multisig (CeBIT) to CLTV");
            /* special case: migrate from objectstore and 2of2 multisig wallet
             * - migrate from early version v1.0.262 (CeBIT)
             * - enable transfer of funds to new address (2of2 multisig to cltv)
             */
            try {
                // migration is done for mainnet and testnet regardless of the current network setting of the app!
                NetworkParameters[] cltvMigrationParams = new NetworkParameters[]{MainNetParams.get(), TestNet3Params.get()};
                for (NetworkParameters migrationParams : cltvMigrationParams) {
                    doMigrateFrom_v1_0_262(context, migrationParams);
                }
            } catch (Exception e) {
                Log.e(TAG, "Migration failed: ", e);
            }
        }


        SharedPrefUtils.setAppVersion(context, BuildConfig.VERSION_NAME);
        checkDone = true;
    }

    private boolean migrateFrom_v1_0_262(Context context) {
        // these directories were used by the object store
        // if any of these exist, we migrate from an existing wallet.
        File objectStoreMain = new File(context.getFilesDir(), "mainnet_wallet_" + "_uuid_object_storage");
        File objectStoreTest = new File(context.getFilesDir(), "testnet_wallet_" + "_uuid_object_storage");
        return objectStoreMain.exists() || objectStoreTest.exists();
    }

    private void doMigrateFrom_v1_0_262(Context context, NetworkParameters migrationParams) throws IOException {
        Log.d(TAG, "********* MIGRATION FROM v1.0.262 - "+migrationParams.getId()+"*********");

        final File rootDir = context.getFilesDir();
        final File storageDir;
        final File archiveDir = new File(rootDir, "archive_" + System.currentTimeMillis());
        final File walletFile;
        final File chainFile;


        final File newWalletFile;
        final File newChainFile;

        if (ClientUtils.isMainNet(migrationParams)) {
            storageDir = new File(rootDir, "mainnet_wallet__uuid_object_storage");
            walletFile = new File(rootDir, "mainnet_wallet_.wallet");
            newWalletFile = new File(rootDir, Constants.WALLET_FILES_PREFIX_MAIN + ".wallet");
            chainFile = new File(rootDir, "mainnet_wallet_.spvchain");
            newChainFile = new File(rootDir, Constants.WALLET_FILES_PREFIX_MAIN + ".spvchain");
        } else if (ClientUtils.isTestNet3(migrationParams)) {
            storageDir = new File(rootDir, "testnet_wallet__uuid_object_storage");
            walletFile = new File(rootDir, "testnet_wallet_.wallet");
            newWalletFile = new File(rootDir, Constants.WALLET_FILES_PREFIX_TEST + ".wallet");
            chainFile = new File(rootDir, "testnet_wallet_.spvchain");
            newChainFile = new File(rootDir, Constants.WALLET_FILES_PREFIX_TEST + ".spvchain");
        } else {
            throw new RuntimeException("Network params not supported (unknown): " + migrationParams.toString());
        }

        final File keyFile = new File(storageDir, "ECKeyWrapper.json");

        if (keyFile.exists() && walletFile.exists()) {

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
            String keyFileJson = FileUtils.readFileToString(keyFile);
            Type type = new TypeToken<Map<String, ECKeyWrapper>>(){}.getType();
            // Note: do not use gson from serializeutils (key is not stored in base64).
            Map<String, ECKeyWrapper> keys = new Gson().fromJson(keyFileJson, type);
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

                    /********** Actual Migration Code **********/
                    SharedPrefUtils.setClientKey(context, migrationParams, clientKey);
                    SharedPrefUtils.setServerKey(context, migrationParams, serverKey);
                    Log.d(TAG, "Migrated keys:"
                            + " clientPubKey=" + clientKey.getPublicKeyAsHex()
                            + ", serverPubKey=" + serverKey.getPublicKeyAsHex());

                    // move wallet file
                    Log.d(TAG, "Migrate wallet file: " + walletFile.toString() + " -> " + newWalletFile.toString());
                    FileUtils.copyFile(walletFile, newWalletFile);
                    Log.d(TAG, "Migrate chain file: " + chainFile.toString() + " -> " + newChainFile.toString());
                    FileUtils.copyFile(chainFile, newChainFile);

                    SharedPrefUtils.enableMultisig2of2ToCltvForwarder(context);

                    // move everything to an archive file.
                    Log.d(TAG, "Move old files to archive dir: " + archiveDir.toString());
                    FileUtils.moveToDirectory(storageDir, archiveDir, true);
                    FileUtils.moveToDirectory(walletFile, archiveDir, true);
                    FileUtils.moveToDirectory(chainFile, archiveDir, true);

                } catch (Exception e) {
                    Log.d(TAG, "Exception: ", e);
                    // clear the changes made.
                    SharedPrefUtils.setClientKey(context, migrationParams, null);
                    SharedPrefUtils.setServerKey(context, migrationParams, null);
                    if (newWalletFile.exists()) {
                        newWalletFile.delete();
                    }
                }
            }
        } else {
            Log.d(TAG, "Key file or wallet file not found - no migration required - "
                    + String.format("keyFile: %s (exists: %s), walletFile: %s (exists: %s)",
                    keyFile.toString(), keyFile.exists(), walletFile.toString(), walletFile.exists()));
        }

        Log.d(TAG, "********* MIGRATION FROM v1.0.262 FINISHED *********");
    }


    private static class ECKeyWrapper {
        /* keys stored in the objectstore have the following structure */
        byte[] keyPayload;
        boolean isPublicOnly;
        String name;
    }

}
