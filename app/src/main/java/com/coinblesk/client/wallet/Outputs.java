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

package com.coinblesk.client.wallet;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.coinblesk.client.R;
import com.coinblesk.client.TransactionDetailActivity;
import com.coinblesk.client.config.Constants;
import com.coinblesk.client.ui.widgets.RecyclerView;
import com.coinblesk.payments.WalletService;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;

import java.util.Collections;
import java.util.Comparator;

/**
 * @author Andreas Albrecht
 */
public class Outputs extends Fragment implements OutputsAdapter.OutputItemClickListener {
    private final static String TAG = Outputs.class.getName();

    private WalletService.WalletServiceBinder walletServiceBinder;

    private OutputsAdapter adapter;
    private RecyclerView recyclerView;

    public static Outputs newInstance() {
        return new Outputs();
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        Intent walletServiceIntent = new Intent(getActivity(), WalletService.class);
        getActivity().bindService(walletServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        adapter = new OutputsAdapter(null, this);

        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(getActivity());
        IntentFilter coinsSentOrReceivedFilter = new IntentFilter();
        coinsSentOrReceivedFilter.addAction(Constants.WALLET_COINS_SENT_ACTION);
        coinsSentOrReceivedFilter.addAction(Constants.WALLET_COINS_RECEIVED_ACTION);
        broadcastManager.registerReceiver(onCoinsSentOrReceived, coinsSentOrReceivedFilter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().unbindService(serviceConnection);
    }

    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.wallet_output_list, container, false);
        initView(v);
        return v;
    }

    private void initView(View v) {
        recyclerView = (RecyclerView) v.findViewById(R.id.wallet_output_list);
        recyclerView.setHasFixedSize(true);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);

        View empty = v.findViewById(R.id.wallet_output_list_empty);
        recyclerView.setEmptyView(empty);

        recyclerView.setAdapter(adapter);
    }

    private synchronized  void refreshOutputs() {
        adapter.getItems().clear();
        adapter.getItems().addAll(walletServiceBinder.getUnspentInstantOutputs());
        Collections.sort(adapter.getItems(), new Comparator<TransactionOutput>() {
            @Override
            public int compare(TransactionOutput lhs, TransactionOutput rhs) {
                Sha256Hash lhsHash = lhs.getParentTransactionHash();
                Sha256Hash rhsHash = lhs.getParentTransactionHash();
                if (lhsHash != null && rhsHash != null) {
                    int hashCmp = lhsHash.toString().compareTo(rhsHash.toString());
                    if (hashCmp != 0) return hashCmp;
                    else return Long.compare(lhs.getIndex(), rhs.getIndex());
                }
                else if (lhsHash != null) return -1;
                else if (rhsHash != null) return 1;
                else return 0;
            }
        });
        adapter.notifyDataSetChanged();
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            walletServiceBinder = (WalletService.WalletServiceBinder) binder;
            refreshOutputs();
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            walletServiceBinder = null;
        }
    };

    @Override
    public void onItemClick(TransactionOutput item, int itemPosition) {
        Log.d(TAG, "Output: " + itemPosition + " - " + item.toString());

        Transaction parentTx = item.getParentTransaction();
        if (parentTx != null) {
            TransactionDetailActivity.openTransaction(getActivity(), parentTx.getHashAsString());
        }
    }


    private final BroadcastReceiver onCoinsSentOrReceived = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshOutputs();
        }
    };
}
