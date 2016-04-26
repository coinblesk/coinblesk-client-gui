package com.coinblesk.client.wallet;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.coinblesk.client.R;
import com.coinblesk.client.ui.widget.RecyclerView;
import com.coinblesk.payments.WalletService;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;

import java.util.Map;

/**
 * Created by albrecht on 22.04.16.
 */
public class WalletAddressList extends Fragment {
    private static final String TAG = WalletAddressList.class.getName();

    private final WalletAddressListAdapter adapter;
    private RecyclerView recyclerView;
    private Map<Address, Coin> balances;

    public static Fragment newInstance() {
        return new WalletAddressList();
    }

    public WalletAddressList() {
        adapter = new WalletAddressListAdapter();
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        Intent walletServiceIntent = new Intent(getContext(), WalletService.class);
        getActivity().bindService(walletServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().unbindService(serviceConnection);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.wallet_address_list, container, false);
        initView(v);
        return v;
    }

    private void initView(View v) {
        recyclerView = (RecyclerView) v.findViewById(R.id.wallet_address_list);
        recyclerView.setHasFixedSize(true);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);

        View empty = v.findViewById(R.id.wallet_address_list_empty);
        recyclerView.setEmptyView(empty);

        recyclerView.setAdapter(adapter);
    }

    private WalletService.WalletServiceBinder walletServiceBinder;

    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            walletServiceBinder = (WalletService.WalletServiceBinder) binder;


            balances = walletServiceBinder.getBalanceByAddress();
            adapter.setBalanceByAddress(balances);
            adapter.getItems().addAll(walletServiceBinder.getAddresses());
            adapter.notifyDataSetChanged();

        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            walletServiceBinder = null;
        }
    };

}
