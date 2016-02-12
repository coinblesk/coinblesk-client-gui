package com.uzh.ckiller.coinblesk_client_gui;

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

import com.uzh.ckiller.coinblesk_client_gui.helpers.ConnectionIconFormatter;
import com.uzh.ckiller.coinblesk_client_gui.helpers.CurrencyFormatter;
import com.uzh.ckiller.coinblesk_client_gui.helpers.IPreferenceStrings;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by ckiller on 10/01/16.
 */

public class BalanceCurrentFragment extends Fragment implements IPreferenceStrings {

    private CurrencyFormatter currencyFormatter;
    private ConnectionIconFormatter connectionIconFormatter;

    public static BalanceCurrentFragment newInstance(int page) {
        BalanceCurrentFragment fragment = new BalanceCurrentFragment();
        return fragment;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Get all the ImageViews
        View view = getView();
        final ImageView nfcIcon = (ImageView) view.findViewById(R.id.nfc_balance);
        final ImageView bluetoothIcon = (ImageView) view.findViewById(R.id.bluetooth_balance);
        final ImageView wifiIcon = (ImageView) view.findViewById(R.id.wifidirect_balance);

        connectionIconFormatter.setIconColor(nfcIcon, NFC_ACTIVATED);
        connectionIconFormatter.setIconColor(bluetoothIcon, BT_ACTIVATED);
        connectionIconFormatter.setIconColor(wifiIcon, WIFIDIRECT_ACTIVATED);

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currencyFormatter = new CurrencyFormatter(getContext());
        connectionIconFormatter = new ConnectionIconFormatter((getContext()));
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


