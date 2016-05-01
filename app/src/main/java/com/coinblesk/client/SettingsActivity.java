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
 * Created by ckiller on 17/01/16.
 */

public class SettingsActivity extends AppCompatActivity {

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
            Preference pref = findPreference("pref_network_list");
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