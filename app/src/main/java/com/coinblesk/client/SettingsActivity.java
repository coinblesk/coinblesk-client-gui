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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;

import com.coinblesk.payments.WalletService;

/**
 * @author ckiller
 * @author Andreas Albrecht
 */

public class SettingsActivity extends AppCompatActivity {

    public static class PreferenceKeys {
        public static final String CONNECTION_SETTINGS = "pref_connection_settings";
        public static final String NETWORK_SETTINGS = "pref_network_list";
        public static final String FIAT_CURRENCY = "pref_currency_list";
    }

    private static final String TAG = SettingsActivity.class.getName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);


        if (getFragmentManager().findFragmentById(R.id.fragment_settings) == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.fragment_settings,
                            new Prefs()).commit();
        }
    }

    public static class Prefs extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings_pref);

            initRestartAfterNetworkChange();
        }

        private void initRestartAfterNetworkChange() {
            Preference pref = findPreference(PreferenceKeys.NETWORK_SETTINGS);
            pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, final Object newValue) {
                    stopWalletService();
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle(R.string.restart)
                            .setMessage(R.string.pref_network_changed_restart)
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Log.i(TAG, "Restart wallet due to network change: " + newValue);
                                    dialog.dismiss();
                                    getActivity().finishAffinity();
                                }
                            })
                            .setCancelable(false)
                            .create()
                            .show();
                    return true;
                }
            });
        }

        private void stopWalletService() {
            Intent intent = new Intent(getActivity(), WalletService.class);
            getActivity().stopService(intent);
        }

    }
}