package com.uzh.ckiller.coinblesk_client_gui;


/**
 * Created by ckiller on 12/01/16.
 */

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.media.IMediaBrowserServiceCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by ckiller on 10/01/16.
 */

public class BalanceCurrentFragment extends Fragment {

    private CurrencyFormatter currencyFormatter;
    public static final Float ICON_TRANSPARENT = new Float(0.25);
    public static final Float ICON_VISIBLE = new Float(0.8);

    public static final String NFC_ACTIVATED = "nfc-checked";
    public static final String BT_ACTIVATED = "bt-checked";
    public static final String WIFIDIRECT_ACTIVATED = "wifi-checked";
    public static final String CONNECTION_SETTINGS = "pref_connection_settings";


    public static BalanceCurrentFragment newInstance(int page) {
        BalanceCurrentFragment fragment = new BalanceCurrentFragment();
        return fragment;
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.i("onResume", "we are inside the balance current fragment");

        // Get all the ImageViews
        View view = getView();
        final ImageView mNfcIcon = (ImageView) view.findViewById(R.id.nfc_balance);
        final ImageView mBluetoothIcon = (ImageView) view.findViewById(R.id.bluetooth_balance);
        final ImageView mWifiIcon = (ImageView) view.findViewById(R.id.wifidirect_balance);

        // Get shared Preferences
        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(this.getContext());
        Set<String> connectionSettings = preferences.getStringSet(CONNECTION_SETTINGS, new HashSet<String>());

        // Set the Icon Color and Visibility
        if (connectionSettings != null) {
            for (String s : connectionSettings) {
                switch (s) {
                    case NFC_ACTIVATED:
                        this.formatImageView(mNfcIcon, NFC_ACTIVATED);
                        break;
                    case BT_ACTIVATED:
                        this.formatImageView(mBluetoothIcon, BT_ACTIVATED);
                        break;
                    case WIFIDIRECT_ACTIVATED:
                        this.formatImageView(mWifiIcon, WIFIDIRECT_ACTIVATED);
                        break;
                }

            }

        }
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currencyFormatter = new CurrencyFormatter(getContext());

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_balance_current, container, false);
        final TextView smallBalance = (TextView) view.findViewById(R.id.balance_small);
        final TextView largeBalance = (TextView) view.findViewById(R.id.balance_large);

        //TODO Get the actual Balance instead of dummy data
        largeBalance.setText(currencyFormatter.formatLarge("2.4431", "BTC"));
        smallBalance.setText(currencyFormatter.formatSmall("655.01", "CHF"));

        return view;

    }

    private void formatImageView(ImageView imageView, String status) {
        switch (status) {
            case NFC_ACTIVATED:
            case BT_ACTIVATED:
            case WIFIDIRECT_ACTIVATED:
                imageView.setAlpha(ICON_VISIBLE);
                imageView.setColorFilter(getResources().getColor(R.color.colorAccent));
                break;
            default:
                break;
        }

    }
}


