/*
 * Copyright 2016 The Coinblesk team and the CSG Group at University of Zurich
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package com.coinblesk.client.addresses;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.coinblesk.client.CoinbleskApp;
import com.coinblesk.client.R;
import com.coinblesk.client.models.AddressBookItem;
import com.coinblesk.client.ui.widgets.RecyclerView;
import com.coinblesk.client.utils.SharedPrefUtils;

import org.bitcoinj.core.NetworkParameters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Andreas Albrecht
 */
public class AddressList extends DialogFragment {

    private static final String TAG = AddressList.class.getName();

    private AddressListAdapter adapter;
    private RecyclerView recyclerView;
    private NetworkParameters params;

    public static AddressList newInstance() {
        return new AddressList();
    }

    public AddressList() {
        adapter = new AddressListAdapter();
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
        params = ((CoinbleskApp) getActivity().getApplication()).getNetworkParameters();
        loadAddresses();
    }

    private void loadAddresses() {
        Log.d(TAG, "Load addresses from storage.");
        List<AddressBookItem> loadedItems;
        try {
            loadedItems = SharedPrefUtils.getAddressBookItems(getContext(), params);
            Collections.sort(loadedItems);
        } catch (Exception e) {
            Log.w(TAG, "Could not load addresses: ", e);
            loadedItems = new ArrayList<>(0);
            String msg = (e.getMessage() != null) ? String.format(" (%s)", e.getMessage()) : "";
            Toast.makeText(getActivity(),
                    getString(R.string.addresses_msg_load_error, msg),
                    Toast.LENGTH_LONG)
                    .show();
        }

        adapter.getItems().clear();
        adapter.getItems().addAll(loadedItems);
        adapter.notifyDataSetChanged();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        if (getShowsDialog()) {
            // fragment is shown as a dialog -> UI is initialized in the onCreateDialog
            return super.onCreateView(inflater, container, savedInstanceState);
        } else {
            View v = inflater.inflate(R.layout.address_list, container, false);
            initView(v);
            return v;
        }
    }

    private void initView(View v) {
        recyclerView = (RecyclerView) v.findViewById(R.id.address_list);
        recyclerView.setHasFixedSize(true);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);

        View empty = v.findViewById(R.id.addresses_empty);
        recyclerView.setEmptyView(empty);

        recyclerView.setAdapter(adapter);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle state) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.AlertDialogAccent);
        View view = getActivity().getLayoutInflater().inflate(R.layout.address_list, null);
        builder.setView(view)
                .setTitle(R.string.addresses)
                .setCancelable(true)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

        initView(view);
        return builder.create();
    }

    public void setItemClickListener(AddressListAdapter.AddressItemClickListener itemClickListener) {
        adapter.setItemClickListener(itemClickListener);
    }

    public AddressListAdapter getAdapter() {
        return adapter;
    }
}
