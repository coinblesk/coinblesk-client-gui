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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import java.util.HashSet;
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
    public void onResume() {
        super.onResume();

        Log.i("onResume", "we are inside the balance current fragment");

        View view = getView();
        final ImageView nfcConn = (ImageView) view.findViewById(R.id.nfc_balance);
        final ImageView bluetoothConn = (ImageView) view.findViewById(R.id.bluetooth_balance);
        final ImageView wifiConn = (ImageView) view.findViewById(R.id.wifidirect_balance);

        SharedPreferences preferences = getActivity().getSharedPreferences("pref_payment_settings", Context.MODE_PRIVATE);

        if (preferences == null) {
            Log.i("preferences", "we are null");
        }



        float inactive = new Float(0.25);
        nfcConn.setAlpha(inactive);

        float activated = new Float(0.75);
        bluetoothConn.setAlpha(activated);
        bluetoothConn.setColorFilter(getResources().getColor(R.color.colorAccent));

        wifiConn.setAlpha(activated);
        wifiConn.setColorFilter(getResources().getColor(R.color.colorAccent));


        try {

            if (preferences.getString(NFC_ACTIVATED, null).equals(NFC_ACTIVATED)) {
                nfcConn.setAlpha(inactive);
//                    nfcConn.setImageAlpha(100);
                Log.i("NFC", "NFC ACTIVATED");
            }

        } catch (NullPointerException e) {
            Log.i(e.toString(), "...");
        }


/*
        Set<String> connectionSettings = preferences.getStringSet(CONNECTION_SETTINGS, new HashSet<String>());


        if (connectionSettings == null) {
            Log.i("connectionSettings", "we are null");
        }
*/


      /*  if (connectionSettings != null) {
            for (String s : connectionSettings) {
                if (s.equals(NFC_ACTIVATED)) {
                    nfcConn.setAlpha(value);
//                    nfcConn.setImageAlpha(100);
                    Log.i("NFC", "NFC ACTIVATED");
                }

                if (s.equals(BLUETOOTH_ACTIVATED)) {
                    bluetoothConn.setAlpha(value);
//                    bluetoothConn.setImageAlpha(100);
                    Log.i("BT", "BT ACTIVATED");
                }
                if (s.equals(WIFIDIRECT_ACTIVATED)) {
                    wifiConn.setAlpha(value);
//                    wifiConn.setImageAlpha(100);
                    Log.i("WIFI", "WIFI ACTIVATED");


                }
            }*/
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
}


