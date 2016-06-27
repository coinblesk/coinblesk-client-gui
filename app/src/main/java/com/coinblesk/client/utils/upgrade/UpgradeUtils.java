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
import com.coinblesk.client.utils.SharedPrefUtils;


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
            boolean ecKeysExist = false;
            // TODO: migrate private key --> shared preferences.
            /*
            try {
                ecKeysExist = storage.getEntries(ECKeyWrapper.class).size() == 2;
            } catch (UuidObjectStorageException e) {
                ecKeysExist = false;
            }
            */

            if (!ecKeysExist) {
                // first start: no version yet
                Log.d(TAG, "No appVersion yet - first launch");
            } else {
                Log.d(TAG, "No appVersion yet - upgrade from v1.0.262 / 2of2 multisig (CeBIT)");
                /* special case: ECKeys already exist, but no version
                 * - migrate from early version v1.0.262 (CeBIT)
                 * - transfer funds to new address (2of2 multisig to cltv)
                 */
                upgradeFrom_v1_0_262();
            }

        } else if (appVersion.equals(BuildConfig.VERSION_NAME)) {
            Log.d(TAG, "appVersion '" + appVersion + "' equals current version - no action required");
        } else {
            // add upgrade instructions as needed.
        }

        SharedPrefUtils.setAppVersion(context, BuildConfig.VERSION_NAME);

        checkDone = true;
    }

    private void upgradeFrom_v1_0_262() {
        // TODO: implement upgrade: forward from 2-of-2 to cltv
        //SharedPrefUtils.enableMultisig2of2ToCltvForwarder(context);
    }
}
