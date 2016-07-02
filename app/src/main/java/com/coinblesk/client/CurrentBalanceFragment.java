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

import android.content.*;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.coinblesk.client.utils.ClientUtils;
import com.coinblesk.client.utils.SharedPrefUtils;
import com.coinblesk.client.utils.UIUtils;
import com.coinblesk.client.config.Constants;
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
    private WalletService.WalletServiceBinder walletServiceBinder;

    public static Fragment newInstance() {
        return new CurrentBalanceFragment();
    }

    @Override
    public void onResume() {
        super.onResume();

        final View view = getView();
        if (view != null) {

            UIUtils.refreshConnectionIconStatus(getContext(), view);

            final NetworkParameters params = Constants.PARAMS;
            if(!ClientUtils.isMainNet(params)) {
                final TextView warning = (TextView) view.findViewById(R.id.testnet_textview);
                if (warning != null) {
                    warning.setText(getString(R.string.testnet_warning, params.getClass().getSimpleName()));
                    warning.setTextColor(Color.parseColor("#ffff4444"));
                    warning.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_balance_current, container, false);
        return view;
    }

    /* ------------------- PAYMENTS INTEGRATION STARTS HERE  ------------------- */
    private void refreshBalance() {
        Coin coinBalance = walletServiceBinder.getBalance();
        Fiat fiatBalance = walletServiceBinder.getExchangeRate().coinToFiat(coinBalance);
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

    private BroadcastReceiver walletProgressBroadcastReceiver = new BroadcastReceiver() {
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

    @Override
    public void onStart() {
        super.onStart();
        Intent intent = new Intent(getActivity(), WalletService.class);
        getActivity().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop() {
        super.onStop();
        getActivity().unbindService(serviceConnection);
        final LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(getActivity());
        broadcastManager.unregisterReceiver(walletBalanceChangeBroadcastReceiver);
        broadcastManager.unregisterReceiver(walletProgressBroadcastReceiver);
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            walletServiceBinder = (WalletService.WalletServiceBinder) binder;
            final LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(getActivity());
            IntentFilter balanceFilter = new IntentFilter(Constants.WALLET_BALANCE_CHANGED_ACTION);
            balanceFilter.addAction(Constants.EXCHANGE_RATE_CHANGED_ACTION);
            broadcastManager.registerReceiver(walletBalanceChangeBroadcastReceiver, balanceFilter);

            IntentFilter walletProgressFilter = new IntentFilter(Constants.WALLET_DOWNLOAD_PROGRESS_ACTION);
            walletProgressFilter.addAction(Constants.WALLET_DOWNLOAD_DONE_ACTION);
            broadcastManager.registerReceiver(walletProgressBroadcastReceiver, walletProgressFilter);

            if (walletServiceBinder.isReady()) {
                refreshBalance();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            walletServiceBinder = null;
        }
    };
    /* -------------------- PAYMENTS INTEGRATION ENDS HERE  -------------------- */
}