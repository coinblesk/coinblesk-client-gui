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

import android.app.DialogFragment;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;

import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.coinblesk.client.config.Constants;
import com.coinblesk.client.models.TransactionWrapper;
import com.coinblesk.client.ui.dialogs.CurrencyDialogFragment;
import com.coinblesk.client.ui.widgets.RecyclerView;
import com.coinblesk.client.utils.ClientUtils;
import com.coinblesk.client.utils.SharedPrefUtils;
import com.coinblesk.client.utils.UIUtils;
import com.coinblesk.payments.WalletService;
import com.coinblesk.util.CoinbleskException;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.utils.Fiat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import java8.util.concurrent.CompletableFuture;
import java8.util.function.Consumer;
import java8.util.function.Supplier;

/**
 * @author ckiller
 * @author Andreas Albrecht
 */

public class CurrentBalanceFragment extends Fragment {
    private static final String TAG = CurrentBalanceFragment.class.getSimpleName();
    private WalletService.WalletServiceBinder walletService;

    private RecyclerView recyclerView;
    private TransactionWrapperRecyclerViewAdapter transactionAdapter;

    private int walletProgressLastRefresh = 0;


    public static CurrentBalanceFragment newInstance() {
        return new CurrentBalanceFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        broadcaster.registerReceiver(exchangeRateChangeListener, new IntentFilter(Constants.EXCHANGE_RATE_CHANGED_ACTION));

        IntentFilter walletProgressFilter = new IntentFilter(Constants.WALLET_DOWNLOAD_PROGRESS_ACTION);
        walletProgressFilter.addAction(Constants.WALLET_DOWNLOAD_DONE_ACTION);
        broadcaster.registerReceiver(walletProgressBroadcastReceiver, walletProgressFilter);


        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.WALLET_CHANGED_ACTION);
        filter.addAction(Constants.WALLET_DOWNLOAD_DONE_ACTION);
        broadcaster.registerReceiver(walletChangedBroadcastReceiver, filter);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshConnectionIcons();
        refreshTestnetWarning();
        refreshBalance();
    }

    @Override
    public void onStop() {
        super.onStop();
        getActivity().unbindService(serviceConnection);
        LocalBroadcastManager broadcaster = LocalBroadcastManager.getInstance(getActivity());
        broadcaster.unregisterReceiver(walletBalanceChangeBroadcastReceiver);
        broadcaster.unregisterReceiver(walletProgressBroadcastReceiver);
        broadcaster.unregisterReceiver(exchangeRateChangeListener);
        broadcaster.unregisterReceiver(walletChangedBroadcastReceiver);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_balance, container, false);





        recyclerView = (RecyclerView) view.findViewById(R.id.txhistoryview);
        View empty = view.findViewById(R.id.txhistory_emptyview);
        recyclerView.setEmptyView(empty);
        transactionAdapter = new TransactionWrapperRecyclerViewAdapter(new ArrayList<TransactionWrapper>());
        recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));
        recyclerView.setAdapter(transactionAdapter);
        updateTransactions();




        ImageView switcher  = (ImageView) view.findViewById(R.id.balance_switch_image_view);
        switcher.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(SharedPrefUtils.isBitcoinPrimaryBalance(getActivity())) {
                    SharedPrefUtils.setFiatPrimaryBalance(getActivity());
                } else {
                    SharedPrefUtils.setBitcoinPrimaryBalance(getActivity());
                }
                refreshBalance();
            }
        });

        TextView t1 = (TextView) view.findViewById(R.id.balance_large);
        TextView t2 = (TextView) view.findViewById(R.id.balance_large_currency);
        TextView t3 = (TextView) view.findViewById(R.id.balance_small);
        TextView t4 = (TextView) view.findViewById(R.id.balance_small_currency);


        View.OnLongClickListener listener = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                DialogFragment fragment = CurrencyDialogFragment.newInstance();
                if (fragment != null) {
                    fragment.show(CurrentBalanceFragment.this.getFragmentManager(), TAG);
                }
                return true;
            }
        };

        t1.setOnLongClickListener(listener);
        t2.setOnLongClickListener(listener);
        t3.setOnLongClickListener(listener);
        t4.setOnLongClickListener(listener);
        return view;
    }

    private void refreshConnectionIcons() {
        final View view = getView();
        if (view != null) {
            UIUtils.refreshConnectionIconStatus(getActivity(), view);
        }
    }

    private void refreshTestnetWarning() {
        final View view = getView();
        if (walletService == null || view == null) {
            return;
        }

        final TextView testnetWarning = (TextView) view.findViewById(R.id.testnet_textview);
        final NetworkParameters params = walletService.getNetworkParameters();
        if (testnetWarning != null) {
            if(ClientUtils.isMainNet(params)) {
                testnetWarning.setVisibility(View.GONE);
            } else {
                testnetWarning.setText(getString(R.string.testnet_warning, params.getClass().getSimpleName()));
                testnetWarning.setVisibility(View.VISIBLE);
            }
        }
    }

    /* ------------------- PAYMENTS INTEGRATION STARTS HERE  ------------------- */
    private void refreshBalance() {
        if (walletService == null) {
            return;
        }
        //TODO: this may be called twice
        refreshConnectionIcons();
        final Coin coinBalance = walletService.getBalance();

        CompletableFuture<Coin> future = CompletableFuture.supplyAsync(new Supplier<Coin>() {
            @Override
            public Coin get() {
                try {
                    Coin virtualBalance = walletService.virtualBalance();
                    return coinBalance.add(virtualBalance);
                } catch (final Exception e) {
                    Snackbar snackbar = Snackbar.make(getView(), e.getMessage(), Snackbar.LENGTH_LONG);
                    snackbar.show();
                    e.printStackTrace();
                }
                return Coin.ZERO;
            }
        });

        future.thenAccept(new Consumer<Coin>() {
            @Override
            public void accept(Coin coin) {
                Fiat fiatBalance = walletService.getExchangeRate().coinToFiat(coin);
                refreshBalance(coin, fiatBalance);
            }
        });







    }

    private void refreshBalance(Coin coinBalance, Fiat fiatBalance) {
        final View root = getView();
        if (root == null || coinBalance == null || fiatBalance == null) {
            return;
        }

        boolean isBitcoinPrimary = SharedPrefUtils.getPrimaryBalance(getActivity()).equals("bitcoin");

        final TextView largeBalance = (TextView) getView().findViewById(R.id.balance_large);
        final TextView largeBalanceCurrency = (TextView) getView().findViewById(R.id.balance_large_currency);
        final TextView smallBalance = (TextView) getView().findViewById(R.id.balance_small);
        final TextView smallBalanceCurrency = (TextView) getView().findViewById(R.id.balance_small_currency);
        if (largeBalance != null && largeBalanceCurrency!= null && smallBalance!=null && smallBalanceCurrency!=null) {
            if(isBitcoinPrimary) {
                largeBalance.setText(UIUtils.scaleCoin(getActivity(), coinBalance));
                largeBalanceCurrency.setText(UIUtils.getMoneyFormat(getActivity()).code());
                smallBalance.setText(fiatBalance.toPlainString());
                smallBalanceCurrency.setText(fiatBalance.getCurrencyCode());
            } else {
                largeBalance.setText(fiatBalance.toPlainString());
                largeBalanceCurrency.setText(fiatBalance.getCurrencyCode());
                smallBalance.setText(UIUtils.scaleCoin(getActivity(), coinBalance));
                smallBalanceCurrency.setText(UIUtils.getMoneyFormat(getActivity()).code());
            }
        }

        Log.d(TAG, "refresh balance: coin=" + coinBalance.toFriendlyString() + ", fiat=" + fiatBalance.toFriendlyString());
    }

    private final BroadcastReceiver exchangeRateChangeListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String symbol = intent.getStringExtra(Constants.EXCHANGE_RATE_SYMBOL);
            if(symbol != null && !symbol.isEmpty()) {
                walletService.setCurrency(symbol);
            }
            refreshBalance();
        }
    };

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
            updateTransactions();
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            walletService = null;
        }
    };

    /* ------------------- PAYMENTS INTEGRATION STARTS HERE  ------------------- */
    private final BroadcastReceiver walletChangedBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (walletService == null) {
                return; // service not connected yet.
            }
            int currentWalletProgress = walletService.getDownloadProgress();
            if (!walletService.isDownloadDone()) {
                // during wallet sync, we prevent updating the tx history for every block
                // and only update once in a while every x%
                int progress = currentWalletProgress - walletProgressLastRefresh;
                if (progress >= 0 && progress < 5) {
                    return;
                }
            }
            walletProgressLastRefresh = currentWalletProgress;
            updateTransactions();
        }
    };

    private void updateTransactions() {
        if (walletService == null || transactionAdapter == null) {
            return; // service not connected yet.
        }

        List<TransactionWrapper> transactions = walletService.getTransactionsByTime();
        transactionAdapter.getItems().clear();
        transactionAdapter.getItems().addAll(transactions);
        transactionAdapter.notifyDataSetChanged();
        Log.d(getClass().getName(), "Transaction history updated.");
    }
}