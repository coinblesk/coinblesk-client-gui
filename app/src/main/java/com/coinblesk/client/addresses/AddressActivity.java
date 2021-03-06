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

import android.app.Activity;
import android.app.DialogFragment;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.coinblesk.client.CoinbleskApp;
import com.coinblesk.client.R;
import com.coinblesk.client.config.Constants;
import com.coinblesk.client.models.AddressBookItem;
import com.coinblesk.client.utils.SharedPrefUtils;
import com.coinblesk.client.utils.UIUtils;
import com.google.zxing.client.android.Intents;
import com.google.zxing.integration.android.IntentIntegrator;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.uri.BitcoinURI;
import org.bitcoinj.uri.BitcoinURIParseException;

/**
 * @author Andreas Albrecht
 */
public class AddressActivity extends AppCompatActivity
                            implements EditAddressFragment.AddressFragmentInteractionListener,
                                       AddressListAdapter.AddressItemClickListener {

    private final static String TAG = AddressActivity.class.getName();

    private NetworkParameters params;
    private AddressList addressList;
    private AddressListAdapter addressListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        params = ((CoinbleskApp) getApplication())
                .getAppConfig()
                .getNetworkParameters();

        setContentView(R.layout.activity_address);
        initToolbar();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
        initAddressList();
    }

    private void initToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void initAddressList() {
        addressList = (AddressList) getFragmentManager()
                .findFragmentById(R.id.fragment_address_list);

        addressList.setItemClickListener(this);
        addressListAdapter = addressList.getAdapter();
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
            case R.id.action_add:
                showAddAddressDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.QR_ACTIVITY_RESULT_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                String scannedContent = data.getStringExtra(Intents.Scan.RESULT);
                if (scannedContent == null) {
                    Toast.makeText(this, R.string.send_address_parse_error, Toast.LENGTH_LONG).show();
                    return;
                }

                String scannedAddress = null;
                try {
                    // first try to parse as bitcoin URI
                    BitcoinURI bitcoinURI = new BitcoinURI(scannedContent);
                    scannedAddress = bitcoinURI.getAddress() != null ? bitcoinURI.getAddress().toBase58() : null;
                } catch (BitcoinURIParseException e) {
                    // not a bitcoin URI - ignore
                }

                if (scannedAddress != null) {
                    showAddAddressDialog(scannedAddress);
                    return;
                }

                try {
                    // scanned content is not an URI, try regular address (plain text)
                    scannedAddress = Address.fromBase58(null, scannedContent).toBase58();
                } catch (AddressFormatException e) {
                    // not a bitcoin address - ignore
                }

                if (scannedAddress != null) {
                    showAddAddressDialog(scannedAddress);
                    return;
                }

                // parsing failed
                Toast.makeText(this, R.string.send_address_parse_error, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void showAddAddressDialog() {
        showAddAddressDialog(null, null);
    }

    private void showAddAddressDialog(String address) {
        showAddAddressDialog(null, address);
    }

    private void showAddAddressDialog(AddressBookItem address) {
        showAddAddressDialog(address.getAddressLabel(), address.getAddress());
    }

    private void showAddAddressDialog(String label, String address) {
        DialogFragment frag = EditAddressFragment.newInstance(label, address);
        frag.show(getFragmentManager(), "fragment_edit_address");
    }

    private void scanAddress() {
        new IntentIntegrator(this).initiateScan();
    }

    @Override
    public void onNewOrChangedAddress(AddressBookItem address) {
        if (address == null) {
            return;
        }

        /**
         * The problem is that the API wants random access, i.e. a set cannot be used.
         * Thus, we prevent inserting if the address is already present by a simple scan
         * and consider only the address and not the label.
         */
        synchronized (addressListAdapter.getItems()) {
            final int numItems = addressListAdapter.getItemCount();
            int existingIndex = -1;
            for (int i = 0; i < numItems; ++i) {
                AddressBookItem item = addressListAdapter.getItems().get(i);
                if (item.getAddress().equals(address.getAddress())) {
                    existingIndex = i;
                    break;
                }
            }

            if (existingIndex >= 0) {
                Log.d(TAG, "Address already exists, update it. old="
                        + addressListAdapter.getItems().get(existingIndex)
                        + ", new=" + address);
                updateAddressInList(existingIndex, address);
            } else {
                Log.d(TAG, "New address=" + address);
                addAddressToList(address);
            }

            View container = findViewById(android.R.id.content);
            if (container != null) {
                Snackbar.make(container,
                        UIUtils.toFriendlySnackbarString(this, getString(R.string.addresses_msg_saved)),
                        Snackbar.LENGTH_LONG)
                        .show();
            }
        }
    }

    private void addAddressToList(AddressBookItem address) {
        // add new item
        final int last = addressListAdapter.getItemCount();
        addressListAdapter.getItems().add(address);
        addressListAdapter.notifyItemInserted(last);
        saveAddress(address);
    }

    private void updateAddressInList(int itemIndex, AddressBookItem newAddress) {
        // update the existing item (remove/add for persistence).
        AddressBookItem current = addressListAdapter.getItems().get(itemIndex);
        deleteAddress(current);

        current.setAddress(newAddress.getAddress().trim());
        current.setAddressLabel(newAddress.getAddressLabel().trim());
        addressListAdapter.notifyItemChanged(itemIndex);

        saveAddress(current);
    }

    private void saveAddress(AddressBookItem addressToAdd) {
        SharedPrefUtils.addAddressBookItem(this, params, addressToAdd);
    }

    private void deleteAddress(AddressBookItem addressToRemove) {
        SharedPrefUtils.removeAddressBookItem(this, params, addressToRemove);
    }

    @Override
    public boolean onItemLongClick(AddressBookItem item, int itemPosition) {
        if (item != null) {
            if (actionMode != null) {
                return false; // already open
            }

            actionMode = startSupportActionMode(actionModeCallback);
            actionMode.setTag(new Pair<>(itemPosition, item));
            actionMode.setTitle(item.getAddress());
            return true;
        }
        return false;
    }

    @Override
    public void onItemClick(AddressBookItem item, int itemPosition) {
        // ignore short clicks on items.
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
            Pair<Integer, AddressBookItem> data = getAddressItem(mode);
            if(data != null) {
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
                        break;
                }
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            actionMode = null;
        }

        private Pair<Integer, AddressBookItem> getAddressItem(ActionMode am) {
            if (am.getTag() != null) {
                @SuppressWarnings("unchecked")
                Pair<Integer, AddressBookItem> address = Pair.class.cast(am.getTag());
                return address;
            }
            return null;
        }
    };

    private void itemActionCopy(int itemPosition, AddressBookItem item) {
        Log.d(TAG, "Copy address at position " + itemPosition + ", " + item);
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Address", item.getAddress());
        clipboard.setPrimaryClip(clip);
        finishActionMode();

        View container = findViewById(android.R.id.content);
        if (container != null) {
            Snackbar.make(container,
                    UIUtils.toFriendlySnackbarString(this, getString(R.string.snackbar_address_copied)),
                    Snackbar.LENGTH_LONG)
                    .show();
        }
    }

    private void itemActionEdit(int itemPosition, AddressBookItem item) {
        Log.d(TAG, "Edit address at position " + itemPosition + ", " + item);
        showAddAddressDialog(item);
        // the adapter is notified about the change later in onNewOrChangedAddress
        // user is notified after the change
        finishActionMode();
    }

    private void itemActionDelete(int itemPosition, AddressBookItem item) {
        Log.d(TAG, "Delete address at position " + itemPosition + ", " + item);

        boolean removed = addressListAdapter.getItems().remove(item);
        if (removed) {
            addressListAdapter.notifyItemRemoved(itemPosition);
        }

        deleteAddress(item);

        finishActionMode();

        View container = findViewById(android.R.id.content);
        if (container != null) {
            Snackbar.make(container,
                    UIUtils.toFriendlySnackbarString(this, getString(R.string.addresses_msg_removed)),
                    Snackbar.LENGTH_LONG)
                    .show();
        }
    }

    private void finishActionMode() {
        if (actionMode != null) {
            actionMode.finish();
        }
    }

}
