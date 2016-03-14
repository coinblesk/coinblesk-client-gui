package com.coinblesk.client;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.coinblesk.client.helpers.UIUtils;
import com.coinblesk.payments.Constants;
import com.coinblesk.payments.WalletService;

import org.bitcoinj.params.TestNet2Params;
import org.bitcoinj.params.TestNet3Params;

/**
 * Created by ckiller on 10/01/16.
 */

public class CurrentBalanceFragment extends Fragment {

    public static CurrentBalanceFragment newInstance(int page) {
        CurrentBalanceFragment fragment = new CurrentBalanceFragment();
        return fragment;
    }

    @Override
    public void onResume() {
        super.onResume();

        View view = getView();
        final ImageView nfcIcon = (ImageView) view.findViewById(R.id.nfc_balance);
        final ImageView bluetoothIcon = (ImageView) view.findViewById(R.id.bluetooth_balance);
        final ImageView wifiIcon = (ImageView) view.findViewById(R.id.wifidirect_balance);

        UIUtils.formatConnectionIcon(this.getContext(), nfcIcon, AppConstants.NFC_ACTIVATED);
        UIUtils.formatConnectionIcon(this.getContext(), bluetoothIcon, AppConstants.BT_ACTIVATED);
        UIUtils.formatConnectionIcon(this.getContext(), wifiIcon, AppConstants.WIFIDIRECT_ACTIVATED);

        if(Constants.PARAMS.equals(TestNet3Params.get()) || Constants.PARAMS.equals(TestNet2Params.get())){
            final TextView testnet = (TextView) view.findViewById(R.id.testnet_textview);
            testnet.setText("Connected to:" + Constants.PARAMS.getClass().getSimpleName());
            testnet.setTextColor(Color.parseColor("#ffff4444"));
            testnet.setVisibility(View.VISIBLE);
        }

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
        largeBalance.setTextSize(TypedValue.COMPLEX_UNIT_SP, UIUtils.getLargeTextSizeForBalance(this.getContext(), largeBalance.getText().length()));
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


