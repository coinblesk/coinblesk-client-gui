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
import com.coinblesk.client.models.AppVersion;
import com.coinblesk.client.models.ECKeyWrapper;
import com.coinblesk.client.utils.SharedPrefUtils;

import ch.papers.objectstorage.UuidObjectStorage;
import ch.papers.objectstorage.UuidObjectStorageException;
import ch.papers.objectstorage.filters.MatchAllFilter;

/**
 * @author Andreas Albrecht
 */
public class UpgradeUtils {
    private static final String TAG = UpgradeUtils.class.toString();
    private final UuidObjectStorage storage;

    private static boolean checkDone = false;

    public UpgradeUtils(UuidObjectStorage storage) {
        this.storage = storage;
    }

    public void checkUpgrade(Context context) {
        if (checkDone) {
            // skip if already done
            return;
        }

        AppVersion appVersion;
        try {
            appVersion = storage.getFirstMatchEntry(new MatchAllFilter(), AppVersion.class);
        } catch (UuidObjectStorageException e) {
            appVersion = null;
        }

        boolean ecKeysExist;
        try {
            ecKeysExist = storage.getEntries(ECKeyWrapper.class).size() == 2;
        } catch (UuidObjectStorageException e) {
            ecKeysExist = false;
        }

        if (appVersion == null && ecKeysExist) {
            /* special case: ECKeys already exist, but no version
             * - migrate from early version v1.0.262 (CeBIT)
             * - transfer funds to new address (2of2 multisig to cltv)
             */
            SharedPrefUtils.enableMultisig2of2ToCltvForwarder(context);
        } else if (appVersion == null) {
            /* first start: no version yet */
        }

        /* add more instructions as needed... */

        try {
            if (appVersion == null) {
                appVersion = new AppVersion();
            }
            appVersion.setVersion(BuildConfig.VERSION_NAME);

            storage.addEntry(appVersion, AppVersion.class);
            storage.commit();
        } catch (UuidObjectStorageException e) {
            Log.w(TAG, "Could not store AppVersion: ", e);
        }

        checkDone = true;
    }
}
