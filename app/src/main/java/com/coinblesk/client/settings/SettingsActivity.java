/*
 * Copyright 2016 The Coinblesk team and the CSG Group at University of Zurich
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.coinblesk.client.settings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.coinblesk.client.CoinbleskApp;
import com.coinblesk.client.R;
import com.coinblesk.client.utils.ClientUtils;
import com.coinblesk.client.utils.SharedPrefUtils;
import com.coinblesk.payments.WalletService;

import org.bitcoinj.core.NetworkParameters;

/**
 * @author ckiller
 * @author Andreas Albrecht
 */

public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = SettingsActivity.class.getName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }


        if (getFragmentManager().findFragmentById(R.id.fragment_settings) == null) {
            getFragmentManager()
                    .beginTransaction()
                    .add(R.id.fragment_settings, new CoinbleskPrefs())
                    .commit();
        }

    }

    public static class CoinbleskPrefs extends PreferenceFragment {
        private WalletService.WalletServiceBinder walletService;

        @Override
        public void onStart() {
            super.onStart();
            Intent walletServiceIntent = new Intent(getActivity(), WalletService.class);
            getActivity().bindService(walletServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            getActivity().unbindService(serviceConnection);
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            getPreferenceManager().setSharedPreferencesName(SharedPrefUtils.getSharedPreferencesName());
            getPreferenceManager().setSharedPreferencesMode(SharedPrefUtils.getSharedPreferencesMode());

            refreshPreferenceScreen();

            initRestartAfterNetworkChange();
            initOnFiatCurrencyChanged();
        }

        private void refreshPreferenceScreen() {
            setPreferenceScreen(null);
            addPreferencesFromResource(R.xml.settings_pref);
        }

        private void initOnFiatCurrencyChanged() {
            final String key = getString(R.string.pref_currency_list);
            Preference pref = findPreference(key);
            pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (walletService != null) {
                        walletService.setCurrency((String) newValue);
                    }
                    return true;
                }
            });
        }

        private void initRestartAfterNetworkChange() {
            final String key = getResources().getString(R.string.pref_network_list);
            Preference pref = findPreference(key);
            pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, final Object newValue) {
                    final String network = newValue.toString();

                    if (network.equals(getString(R.string.pref_network_localtestnet))) {
                        showServerUrlDialog(network);
                        // pref is accepted after user entered server url and confirms with OK
                        return false;
                    }

                    refreshAppConfigAndRestartWallet(network);
                    return true;
                }
            });
        }

        private void refreshAppConfigAndRestartWallet(final String network) {
            ((CoinbleskApp) getActivity().getApplication()).refreshAppConfig(network);
            restartWalletService();
        }

        private void showServerUrlDialog(final String network) {
            final View serverUrlView = getActivity().getLayoutInflater().inflate(R.layout.dialog_server_url_input, null);
            Dialog dialog = new AlertDialog.Builder(getActivity(), R.style.AlertDialogAccent)
                    .setTitle("Server URL")
                    .setMessage("Enter the URL of a Coinblesk server.")
                    .setView(serverUrlView)
                    .setNegativeButton(R.string.cancel, null)
                    .setCancelable(false)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // since we returned false below, preference is set now.
                            String serverUrl = ((EditText) serverUrlView
                                    .findViewById(R.id.server_url))
                                    .getText()
                                    .toString();

                            if (!serverUrl.endsWith("/")) {
                                // retrofit requires url ending with "/"
                                serverUrl = serverUrl.concat("/");
                            }

                            if (ClientUtils.isValidHttpUrl(serverUrl)) {
                                SharedPrefUtils.setLocalTestNetServerUrl(getActivity(), serverUrl);
                                SharedPrefUtils.setNetwork(getActivity(), network);
                                refreshAppConfigAndRestartWallet(network);
                                refreshPreferenceScreen();
                            } else {
                                new AlertDialog.Builder(getActivity(), R.style.AlertDialogAccent)
                                        .setTitle(R.string.dialog_invalid_url_title)
                                        .setMessage(R.string.dialog_invalid_url_message)
                                        .setNeutralButton(R.string.ok, null)
                                        .create()
                                        .show();
                            }

                            dialog.dismiss();
                        }
                    })
                    .create();
            dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialog) {
                    String currentServerUrl = SharedPrefUtils.getLocalTestNetServerUrl(getActivity());
                    ((EditText)serverUrlView.findViewById(R.id.server_url)).setText(currentServerUrl);
                }
            });
            dialog.show();
        }

        private void restartWalletService() {
            getActivity().unbindService(serviceConnection);
            walletService = null;
            getActivity().stopService(new Intent(getActivity(), WalletService.class));
            getActivity().startService(new Intent(getActivity(), WalletService.class));
            getActivity().bindService(new Intent(getActivity(), WalletService.class), serviceConnection, Context.BIND_AUTO_CREATE);
        }

        private void stopWalletService() {
            Intent intent = new Intent(getActivity(), WalletService.class);
            getActivity().stopService(intent);
        }

        private final ServiceConnection serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                walletService = (WalletService.WalletServiceBinder) service;
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                walletService = null;
            }
        };
    }

}