package com.uzh.ckiller.coinblesk_client_gui;


/**
 * Created by ckiller on 12/01/16.
 */

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

/**
 * Created by ckiller on 10/01/16.
 */

public class BalanceCurrentFragment extends Fragment {

    private CurrencyFormatter currencyFormatter;

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

        // TODO Get the SharedPreferences on Connection Settings
        // change android:alpha to 0.25 if not activated
        // change android:alpha to 0.75 if activated

        final ImageView nfcConn = (ImageView) view.findViewById(R.id.nfc_balance);
        final ImageView bluetoothConn = (ImageView) view.findViewById(R.id.bluetooth_balance);
        final ImageView wifiConn = (ImageView) view.findViewById(R.id.wifidirect_balance);

/*

        public static final String PREF_FILE_NAME = "PrefFile";
        SharedPreferences preferences = getActivity().getSharedPreferences(PREF_FILE_NAME, MODE_PRIVATE);

*/

        //TODO Get the actual Balance instead of dummy data
        largeBalance.setText(currencyFormatter.formatLarge("2.4431", "BTC"));
        smallBalance.setText(currencyFormatter.formatSmall("655.01", "CHF"));

        return view;
    }
}
