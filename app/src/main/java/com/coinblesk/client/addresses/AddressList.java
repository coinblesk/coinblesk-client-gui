package com.coinblesk.client.addresses;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import ch.papers.objectstorage.UuidObjectStorage;
import ch.papers.objectstorage.listeners.OnResultListener;
import com.coinblesk.client.R;
import com.coinblesk.client.ui.widget.RecyclerView;

import java.util.Collections;
import java.util.List;

/**
 * Created by albrecht on 16.04.16.
 */
public class AddressList extends DialogFragment {

    private static final String TAG = AddressList.class.getName();

    private AddressListAdapter adapter;
    private RecyclerView recyclerView;

    public AddressList newInstance() {
        return new AddressList();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapter = new AddressListAdapter();
        loadAddresses();
    }

    private void loadAddresses() {
        Log.d(TAG, "Load addresses from storage.");
        UuidObjectStorage
                .getInstance()
                .getEntriesAsList(new OnResultListener<List<AddressWrapper>>() {
                    @Override
                    public void onSuccess(List<AddressWrapper> objects) {
                        if (objects != null && !objects.isEmpty()) {
                            Collections.sort(objects);
                            adapter.getItems().addAll(objects);
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    adapter.notifyDataSetChanged();
                                }
                            });
                        }
                    }

                    @Override
                    public void onError(String s) {
                        String msg = (s != null) ? String.format(" (%s)", s) : "";
                        Toast.makeText(getActivity(),
                                getString(R.string.addresses_msg_load_error, msg),
                                Toast.LENGTH_LONG)
                                .show();
                    }
                }, AddressWrapper.class);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        View v = inflater.inflate(R.layout.address_list, container, false);

        recyclerView = (RecyclerView) v.findViewById(R.id.address_list);
        recyclerView.setHasFixedSize(true);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);

        View empty = v.findViewById(R.id.addresses_empty);
        recyclerView.setEmptyView(empty);

        recyclerView.setAdapter(adapter);

        return v;
    }


    public void setItemClickListener(AddressListAdapter.AddressItemClickListener itemClickListener) {
        adapter.setItemClickListener(itemClickListener);
    }

    public AddressListAdapter getAdapter() {
        return adapter;
    }
}
