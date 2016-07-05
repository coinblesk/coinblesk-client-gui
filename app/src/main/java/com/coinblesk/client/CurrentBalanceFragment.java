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

package com.coinblesk.client;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.coinblesk.client.config.Constants;
import com.coinblesk.client.utils.ClientUtils;
import com.coinblesk.client.utils.UIUtils;
import com.coinblesk.payments.WalletService;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.utils.Fiat;

/**
 * @author ckiller
 * @author Andreas Albrecht
 */

public class CurrentBalanceFragment extends Fragment {
    private static final String TAG = CurrentBalanceFragment.class.getSimpleName();
    private WalletService.WalletServiceBinder walletService;

    public static Fragment newInstance() {
        return new CurrentBalanceFragment();
    }

    @Override
    public void onStart() {
        super.onStart();
        Intent intent = new Intent(getActivity(), WalletService.class);
        getActivity().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        LocalBroadcastManager broadcaster = LocalBroadcastManager.getInstance(getActivity());
        IntentFilter balanceFilter = new IntentFilter(Constants.WALLET_BALANCE_CHANGED_ACTION);
        balanceFilter.addAction(Constants.EXCHANGE_RATE_CHANGED_ACTION);
        broadcaster.registerReceiver(walletBalanceChangeBroadcastReceiver, balanceFilter);

        IntentFilter walletProgressFilter = new IntentFilter(Constants.WALLET_DOWNLOAD_PROGRESS_ACTION);
        walletProgressFilter.addAction(Constants.WALLET_DOWNLOAD_DONE_ACTION);
        broadcaster.registerReceiver(walletProgressBroadcastReceiver, walletProgressFilter);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshConnectionIcons();
        refreshTestnetWarning();
    }

    @Override
    public void onStop() {
        super.onStop();
        getActivity().unbindService(serviceConnection);
        LocalBroadcastManager broadcaster = LocalBroadcastManager.getInstance(getActivity());
        broadcaster.unregisterReceiver(walletBalanceChangeBroadcastReceiver);
        broadcaster.unregisterReceiver(walletProgressBroadcastReceiver);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_balance_current, container, false);
        return view;
    }

    private void refreshConnectionIcons() {
        final View view = getView();
        if (view != null) {
            UIUtils.refreshConnectionIconStatus(getContext(), view);
        }
    }

    private void refreshTestnetWarning() {
        final View view = getView();
        if (walletService == null || view == null) {
            return;
        }

        final TextView testnetWarning = (TextView) view.findViewById(R.id.testnet_textview);
        final NetworkParameters params = walletService.networkParameters();
        if (testnetWarning != null) {
            if(ClientUtils.isMainNet(params)) {
                testnetWarning.setVisibility(View.GONE);
            } else {
                testnetWarning.setText(getString(R.string.testnet_warning, params.getClass().getSimpleName()));
                testnetWarning.setTextColor(getResources().getColor(R.color.cpb_red));
                testnetWarning.setVisibility(View.VISIBLE);
            }
        }
    }

    /* ------------------- PAYMENTS INTEGRATION STARTS HERE  ------------------- */
    private void refreshBalance() {
        if (walletService == null) {
            return;
        }

        Coin coinBalance = walletService.getBalance();
        Fiat fiatBalance = walletService.getExchangeRate().coinToFiat(coinBalance);
        refreshBalance(coinBalance, fiatBalance);
    }

    private void refreshBalance(Coin coinBalance, Fiat fiatBalance) {
        final View root = getView();
        if (root == null || coinBalance == null || fiatBalance == null) {
            return;
        }

        final TextView largeBalance = (TextView) getView().findViewById(R.id.balance_large);
        if (largeBalance != null) {
            largeBalance.setText(UIUtils.getLargeBalance(getContext(), coinBalance, fiatBalance));
            int len = largeBalance.getText().length();
            largeBalance.setTextSize(TypedValue.COMPLEX_UNIT_SP, UIUtils.getLargeTextSizeForBalance(getContext(), len));
        }

        final TextView smallBalance = (TextView) getView().findViewById(R.id.balance_small);
        if (smallBalance != null) {
            smallBalance.setText(UIUtils.getSmallBalance(getContext(), coinBalance, fiatBalance));
        }

        Log.d(TAG, "refresh balance: coin=" + coinBalance.toFriendlyString() + ", fiat=" + fiatBalance.toFriendlyString());
    }

    private final BroadcastReceiver walletBalanceChangeBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Object coinObj = intent.getSerializableExtra("coinBalance");
            Coin coinBalance = null;
            if (coinObj != null && coinObj instanceof Coin) {
                coinBalance = (Coin) coinObj;
            }
            Object fiatObj = intent.getSerializableExtra("fiatBalance");
            Fiat fiatBalance = null;
            if (fiatObj != null && fiatObj instanceof  Fiat) {
                fiatBalance = (Fiat) fiatObj;
            }
            refreshBalance(coinBalance, fiatBalance);
        }
    };

    private final BroadcastReceiver walletProgressBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(getView() == null) return;

            ProgressBar progressBar = (ProgressBar) getView().findViewById(R.id.walletSyncProgressBar);
            switch (intent.getAction()) {
                case Constants.WALLET_DOWNLOAD_PROGRESS_ACTION:
                    int progress = intent.getExtras().getInt("progress", 100);
                    if (progress < 100) {
                        progressBar.setVisibility(View.VISIBLE);
                        progressBar.setProgress(progress);
                    } else {
                        progressBar.setVisibility(View.GONE);
                    }
                    break;
                case Constants.WALLET_DOWNLOAD_DONE_ACTION:
                    progressBar.setVisibility(View.GONE);
                    break;
            }
        }
    };

    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            walletService = (WalletService.WalletServiceBinder) binder;

            if (walletService.isReady()) {
                refreshBalance();
            }

            refreshTestnetWarning();
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            walletService = null;
        }
    };

    /* -------------------- PAYMENTS INTEGRATION ENDS HERE  -------------------- */
}