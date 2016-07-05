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
package com.coinblesk.client;

import android.app.Application;
import android.util.Log;

import com.coinblesk.client.config.AppConfig;
import com.coinblesk.client.utils.SharedPrefUtils;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;

/**
 * @author Andreas Albrecht
 */
public class CoinbleskApp extends Application {
    private static final String TAG = CoinbleskApp.class.getName();

    private volatile AppConfig appConfig;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

        refreshAppConfig();
    }

    private void refreshAppConfig() {
        if (SharedPrefUtils.isNetworkMainnet(this)) {
            appConfig = AppConfig.MainNetConfig.get();
        } else if (SharedPrefUtils.isNetworkTestnet(this)) {
            appConfig = AppConfig.TestNetConfig.get();
        } else {
            throw new RuntimeException("Unsupported network: " + SharedPrefUtils.getNetwork(this));
        }
        Log.i(TAG, "Refresh app config: " + appConfig);
    }

    public AppConfig getAppConfig() {
        return appConfig;
    }
}
