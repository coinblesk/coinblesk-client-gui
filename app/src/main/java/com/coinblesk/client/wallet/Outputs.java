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

/**
 * Created by albrecht on 26.04.16.
 */
public class Outputs extends Fragment {
    private final static String TAG = Outputs.class.getName();

    private WalletService.WalletServiceBinder walletServiceBinder;

    //private OutputAdapter adapter;
    private RecyclerView recyclerView;

    public static Outputs newInstance() {
        return new Outputs();
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

    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.wallet_output_list, container, false);
        initView(v);
        return v;
    }

    private void initView(View v) {
        recyclerView = (RecyclerView) v.findViewById(R.id.wallet_output_list);
        recyclerView.setHasFixedSize(true);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);

        View empty = v.findViewById(R.id.wallet_output_list_empty);
        recyclerView.setEmptyView(empty);

        recyclerView.setAdapter(null); // TODO:
    }

    private void updateOutputs() {
        // get outputs and add to adapter.
        //adapter.notifyDataSetChanged();
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            walletServiceBinder = (WalletService.WalletServiceBinder) binder;
            updateOutputs();
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            walletServiceBinder = null;
        }
    };
}
