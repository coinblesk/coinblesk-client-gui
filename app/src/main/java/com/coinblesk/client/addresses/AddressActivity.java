package com.coinblesk.client.addresses;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;


import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.Toolbar;
import android.support.v7.view.ActionMode;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import ch.papers.objectstorage.UuidObjectStorage;
import ch.papers.objectstorage.UuidObjectStorageException;
import ch.papers.objectstorage.listeners.OnResultListener;

import com.coinblesk.client.R;

import com.coinblesk.client.helpers.UIUtils;
import com.coinblesk.client.ui.widget.RecyclerView;
import com.google.zxing.client.android.Intents;
import com.google.zxing.integration.android.IntentIntegrator;
import org.bitcoinj.uri.BitcoinURI;
import org.bitcoinj.uri.BitcoinURIParseException;

import java.util.Collections;
import java.util.List;

public class AddressActivity extends AppCompatActivity
                            implements EditAddressFragment.AddressFragmentInteractionListener,
                                        AddressListAdapter.AddressClickHandler {

    private final static String TAG = AddressActivity.class.getName();
    private static final int REQUEST_CODE = 0x0000c0de; // Only use bottom 16 bits

    private RecyclerView recyclerView;
    private AddressListAdapter adapter;
    private RecyclerView.LayoutManager layoutManager;

    /**
     * TODO
     * - in editing mode: only title can be edited (such that we never accidentally change address).
     * - maybe title mandatory?
     * - fix action mode vs. toolbar
     * - close action mode if item is deleted.
     * - highlight selected item
     * - allow adding from clipboard / manually / other coinblesk dialogs.
     * - rename title to label
     * - address list empty bug if first item is added.
     */


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        setContentView(R.layout.activity_address);

        initToolbar();
        initAddressList();
    }

    private void initToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            // getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
    }

    private void initAddressList() {
        recyclerView = (RecyclerView) findViewById(R.id.addresslist);

        recyclerView.setHasFixedSize(true);

        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        adapter = new AddressListAdapter(null, this);
        recyclerView.setAdapter(adapter);

        View empty = findViewById(R.id.addresses_empty);
        recyclerView.setEmptyView(empty);

        loadAddresses();
    }

    private void loadAddresses() {
        UuidObjectStorage
                .getInstance()
                .getEntriesAsList(new OnResultListener<List<AddressWrapper>>() {
                    @Override
                    public void onSuccess(List<AddressWrapper> objects) {
                        if (objects != null && !objects.isEmpty()) {
                            Collections.sort(objects);
                            adapter.getItems().addAll(objects);
                            adapter.notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void onError(String s) {
                        String msg = "";
                        if (s != null) { msg = String.format(" (%s)", s); }
                        Toast.makeText(AddressActivity.this,
                                String.format("Could not load addresses %s", msg),
                                Toast.LENGTH_LONG)
                                .show();
                    }
                }, AddressWrapper.class);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.address_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_qrcode:
                scanAddress();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                final String contents = data.getStringExtra(Intents.Scan.RESULT);
                try {
                    BitcoinURI bitcoinURI = new BitcoinURI(contents);
                    String result = bitcoinURI.getAddress().toString();
                    showAddAddressDialog(result);
                } catch (BitcoinURIParseException e) {
                    Toast.makeText(this, "Not a valid Bitcoin address.", Toast.LENGTH_LONG).show();
                }

            }
        }
    }

    private void showAddAddressDialog(String address) {
        showAddAddressDialog("", address);
    }

    private void showAddAddressDialog(AddressWrapper address) {
        showAddAddressDialog(address.getAddressTitle(), address.getAddress());
    }

    private void showAddAddressDialog(String title, String address) {
        FragmentManager fm = getSupportFragmentManager();
        DialogFragment frag = EditAddressFragment.newInstance(title, address);
        frag.show(fm, "fragment_edit_address");
    }

    private void scanAddress() {
        new IntentIntegrator(this).initiateScan();
    }

    @Override
    public void onNewOrChangedAddress(AddressWrapper address) {
        if (address == null) {
            return;
        }

        /**
         * The problem is that the API wants random access, i.e. a set cannot be used.
         * Thus, we prevent inserting if the address is already present by a simple scan
         * and consider only the address and not the title.
         */
        synchronized (adapter.getItems()) {
            final int numItems = adapter.getItemCount();
            int existingIndex = -1;
            for (int i = 0; i < numItems; ++i) {
                AddressWrapper item = adapter.getItems().get(i);
                if (item.getAddress().equals(address.getAddress())) {
                    existingIndex = i;
                    break;
                }
            }

            if (existingIndex >= 0) {
                Log.d(TAG, "Address already exists, update it. old="
                        + adapter.getItems().get(existingIndex)
                        + ", new=" + address);
                updateAddressInList(existingIndex, address);
            } else {
                Log.d(TAG, "New address=" + address);
                addAddressToList(address);
            }
        }
    }

    private void addAddressToList(AddressWrapper address) {
        // add new item
        final int last = adapter.getItemCount();
        adapter.getItems().add(address);
        adapter.notifyItemInserted(last);
        storeAddress(address);
    }

    private void updateAddressInList(int itemIndex, AddressWrapper newAddress) {
        // update the existing item.
        AddressWrapper current = adapter.getItems().get(itemIndex);
        current.setAddress(newAddress.getAddress().trim());
        current.setAddressTitle(newAddress.getAddressTitle().trim());
        adapter.notifyItemChanged(itemIndex);
        storeAddress(current);
    }

    private void storeAddress(AddressWrapper address) {
        try {
            UuidObjectStorage.getInstance().addEntry(address, AddressWrapper.class);
            UuidObjectStorage.getInstance().commit();
        } catch (Exception e) {
            Toast.makeText(this, "Could not store address.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onItemLongClick(int itemPosition) {
        int numItems = adapter.getItemCount();
        if (itemPosition >= 0 && itemPosition < numItems) {
            AddressWrapper selected = adapter.getItems().get(itemPosition);

            if (actionMode != null) {
                return false; // already open but not closed yet.
            }

            actionMode = startSupportActionMode(actionModeCallback);
            actionMode.setTag(new Pair<Integer, AddressWrapper>(itemPosition, selected));
            //view.setSelected(true);
            return true;
        }
        return false;
    }


    private ActionMode actionMode;
    private ActionMode.Callback actionModeCallback = new ActionMode.Callback() {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.address_item_menu, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false; // Return false if nothing is done
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {

            Pair<Integer, AddressWrapper> data = (Pair<Integer, AddressWrapper>) mode.getTag();
            switch (item.getItemId()) {
                case R.id.action_copy:
                    itemActionCopy(data.first, data.second);
                    return true;
                case R.id.action_edit:
                    itemActionEdit(data.first, data.second);
                    return true;
                case R.id.action_delete:
                    itemActionDelete(data.first, data.second);
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            actionMode = null;
        }
    };

    private void itemActionCopy(int itemPosition, AddressWrapper item) {
        Log.d(TAG, "Copy address at position " + itemPosition + ", " + item);
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Address", item.getAddress());
        clipboard.setPrimaryClip(clip);
        Snackbar.make(recyclerView,
                UIUtils.toFriendlySnackbarString(this, getString(R.string.snackbar_address_copied)),
                Snackbar.LENGTH_LONG)
                .show();
    }

    private void itemActionEdit(int itemPosition, AddressWrapper item) {
        Log.d(TAG, "Edit address at position " + itemPosition + ", " + item);
        showAddAddressDialog(item);
        // the adapter is notified about the change later in onNewOrChangedAddress
    }

    private void itemActionDelete(int itemPosition, AddressWrapper item) {
        Log.d(TAG, "Delete address at position " + itemPosition + ", " + item);
        try {
            UuidObjectStorage
                    .getInstance()
                    .deleteEntry(item, AddressWrapper.class);
            UuidObjectStorage.getInstance().commit();
        } catch (UuidObjectStorageException e) {
            Log.w("Delete Failed.", e);
        }

        boolean removed = adapter.getItems().remove(item);
        if (removed) {
            adapter.notifyItemRemoved(itemPosition);
        }
    }

}
