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

    public static AddressList newInstance() {
        return new AddressList();
    }

    public AddressList() {
        adapter = new AddressListAdapter();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadAddresses();
    }

    private void loadAddresses() {
        Log.d(TAG, "Load addresses from storage.");
        UuidObjectStorage
                .getInstance()
                .getEntriesAsList(new OnResultListener<List<AddressItem>>() {
                    @Override
                    public void onSuccess(List<AddressItem> objects) {
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
                }, AddressItem.class);
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
