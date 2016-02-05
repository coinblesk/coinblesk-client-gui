package com.uzh.ckiller.coinblesk_client_gui;


/**
 * Created by ckiller on 12/01/16.
 */

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import java.util.Set;

/**
 * Created by ckiller on 10/01/16.
 */

public class BalanceCurrentFragment extends Fragment {

    private CurrencyFormatter currencyFormatter;
    public static final String NFC_ACTIVATED = "nfc-checked";
    public static final String BLUETOOTH_ACTIVATED = "btc-checked";
    public static final String WIFIDIRECT_ACTIVATED = "wifi-checked";
    public static final String CONNECTION_SETTINGS = "pref_connection_settings";


    public static BalanceCurrentFragment newInstance(int page) {
        BalanceCurrentFragment fragment = new BalanceCurrentFragment();
        return fragment;
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

        // TODO Move this code to the approporiate section / fragment method?
        final ImageView nfcConn = (ImageView) view.findViewById(R.id.nfc_balance);
        final ImageView bluetoothConn = (ImageView) view.findViewById(R.id.bluetooth_balance);
        final ImageView wifiConn = (ImageView) view.findViewById(R.id.wifidirect_balance);

        SharedPreferences preferences = getActivity().getSharedPreferences(CONNECTION_SETTINGS,Context.MODE_PRIVATE);
        Set<String> connectionSettings = preferences.getStringSet(CONNECTION_SETTINGS, null);
        // defValues
        if (connectionSettings != null) {
            for (String s : connectionSettings) {
                if (s.equals(NFC_ACTIVATED)) {
                    nfcConn.setImageAlpha(100);
                    System.out.println("[NFC]HELLO WE ARE INSIDE HERE HEHEHEH");
                }

                if (s.equals(BLUETOOTH_ACTIVATED)) {
                    bluetoothConn.setImageAlpha(100);
                    System.out.println("[BT]HELLO WE ARE INSIDE HERE HEHEHEH");

                }
                if (s.equals(WIFIDIRECT_ACTIVATED)) {
                    wifiConn.setImageAlpha(100);
                    System.out.println("[WIFI]HELLO WE ARE INSIDE HERE HEHEHEH");

                }
            }
        }


        //TODO Get the actual Balance instead of dummy data
        largeBalance.setText(currencyFormatter.formatLarge("2.4431", "BTC"));
        smallBalance.setText(currencyFormatter.formatSmall("655.01", "CHF"));

        return view;

    }
}


