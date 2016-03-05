package com.uzh.ckiller.coinblesk_client_gui;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.DialogPreference;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.uzh.ckiller.coinblesk_client_gui.helpers.IPreferenceStrings;
import com.uzh.ckiller.coinblesk_client_gui.helpers.UIUtils;

import org.bitcoinj.utils.ExchangeRate;
import org.bitcoinj.utils.Fiat;

import ch.papers.payments.Constants;
import ch.papers.payments.WalletService;

/**
 * Created by ckiller on 10/01/16.
 */

public class CurrentBalanceFragment extends Fragment implements IPreferenceStrings {

    public static CurrentBalanceFragment newInstance(int page) {
        CurrentBalanceFragment fragment = new CurrentBalanceFragment();
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

        UIUtils.formatConnectionIcon(this.getContext(), nfcIcon, NFC_ACTIVATED);
        UIUtils.formatConnectionIcon(this.getContext(), bluetoothIcon, BT_ACTIVATED);
        UIUtils.formatConnectionIcon(this.getContext(), wifiIcon, WIFIDIRECT_ACTIVATED);

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_balance_current, container, false);
        return view;
    }

    /* ------------------- PAYMENTS INTEGRATION STARTS HERE  ------------------- */
    private void setBalance() {
        final TextView smallBalance = (TextView) getView().findViewById(R.id.balance_small);
        final TextView largeBalance = (TextView) getView().findViewById(R.id.balance_large);

        // New UIUtils methods using Preferences and BtcFormat
        largeBalance.setText(UIUtils.getLargeBalance(this.getContext(), walletServiceBinder));
        largeBalance.setTextSize(TypedValue.COMPLEX_UNIT_SP, UIUtils.getLargeTextSize(this.getContext(), largeBalance.getText().length()));
        smallBalance.setText(UIUtils.getSmallBalance(this.getContext(), walletServiceBinder));

        // Old method
//        largeBalance.setText(UIUtils.toLargeSpannable(this.getContext(), walletServiceBinder.getBalance().toPlainString(), "BTC"));
//        smallBalance.setText(UIUtils.toSmallSpannable(walletServiceBinder.getBalanceFiat().toPlainString(), walletServiceBinder.getBalanceFiat().getCurrencyCode()));

    }

    private final BroadcastReceiver walletBalanceChangeBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            setBalance();
        }
    };

    private WalletService.WalletServiceBinder walletServiceBinder;

    @Override
    public void onStart() {
        super.onStart();
        Intent intent = new Intent(this.getActivity(), WalletService.class);
        this.getActivity().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop() {
        super.onStop();
        this.getActivity().unbindService(serviceConnection);
        LocalBroadcastManager.getInstance(CurrentBalanceFragment.this.getActivity()).unregisterReceiver(walletBalanceChangeBroadcastReceiver);
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder binder) {
            walletServiceBinder = (WalletService.WalletServiceBinder) binder;
            walletServiceBinder.setExchangeRate(new ExchangeRate(Fiat.parseFiat("CHF", "430")));
            IntentFilter filter = new IntentFilter(Constants.WALLET_BALANCE_CHANGED_ACTION);
            filter.addAction(Constants.WALLET_TRANSACTIONS_CHANGED_ACTION);
            LocalBroadcastManager.getInstance(CurrentBalanceFragment.this.getActivity()).registerReceiver(walletBalanceChangeBroadcastReceiver, filter);
            setBalance();
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            walletServiceBinder = null;
        }
    };
    /* -------------------- PAYMENTS INTEGRATION ENDS HERE  -------------------- */

}


