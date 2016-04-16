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
import android.widget.Toast;
import ch.papers.objectstorage.UuidObjectStorage;
import ch.papers.objectstorage.UuidObjectStorageException;
import com.coinblesk.client.R;
import com.coinblesk.client.helpers.UIUtils;
import com.google.zxing.client.android.Intents;
import com.google.zxing.integration.android.IntentIntegrator;
import org.bitcoinj.uri.BitcoinURI;
import org.bitcoinj.uri.BitcoinURIParseException;

public class AddressActivity extends AppCompatActivity
                            implements EditAddressFragment.AddressFragmentInteractionListener,
                                       AddressListAdapter.AddressItemClickListener {

    private final static String TAG = AddressActivity.class.getName();
    private static final int REQUEST_CODE = 0x0000c0de; // Only use bottom 16 bits

    private AddressList addressList;
    private AddressListAdapter addressListAdapter;

    /**
     * TODO
     * - allow adding from other coinblesk dialogs.
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_address);
        initToolbar();
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart");
        super.onStart();
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
        addressList = (AddressList) getSupportFragmentManager()
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
        if (requestCode == REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                final String contents = data.getStringExtra(Intents.Scan.RESULT);
                try {
                    BitcoinURI bitcoinURI = new BitcoinURI(contents);
                    String result = bitcoinURI.getAddress().toString();
                    showAddAddressDialog(result);
                } catch (BitcoinURIParseException e) {
                    Toast.makeText(this, R.string.send_address_parse_error, Toast.LENGTH_LONG).show();
                }

            }
        }
    }

    private void showAddAddressDialog() {
        showAddAddressDialog(null, null);
    }

    private void showAddAddressDialog(String address) {
        showAddAddressDialog(null, address);
    }

    private void showAddAddressDialog(AddressWrapper address) {
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
    public void onNewOrChangedAddress(AddressWrapper address) {
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
                AddressWrapper item = addressListAdapter.getItems().get(i);
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

            Snackbar.make(findViewById(android.R.id.content),
                    UIUtils.toFriendlySnackbarString(this, getString(R.string.addresses_msg_saved)),
                    Snackbar.LENGTH_LONG)
                    .show();
        }
    }

    private void addAddressToList(AddressWrapper address) {
        // add new item
        final int last = addressListAdapter.getItemCount();
        addressListAdapter.getItems().add(address);
        addressListAdapter.notifyItemInserted(last);
        storeAddress(address);
    }

    private void updateAddressInList(int itemIndex, AddressWrapper newAddress) {
        // update the existing item.
        AddressWrapper current = addressListAdapter.getItems().get(itemIndex);
        current.setAddress(newAddress.getAddress().trim());
        current.setAddressLabel(newAddress.getAddressLabel().trim());
        addressListAdapter.notifyItemChanged(itemIndex);
        storeAddress(current);
    }

    private void storeAddress(AddressWrapper address) {
        try {
            UuidObjectStorage.getInstance().addEntry(address, AddressWrapper.class);
            UuidObjectStorage.getInstance().commit();
        } catch (Exception e) {
            Toast.makeText(this, R.string.addresses_msg_saved_error, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onItemLongClick(AddressWrapper item, int itemPosition) {
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
    public void onItemClick(AddressWrapper item, int itemPosition) {
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
            Pair<Integer, AddressWrapper> data = getAddressWrapper(mode);
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

        private Pair<Integer, AddressWrapper> getAddressWrapper(ActionMode am) {
            if (am.getTag() != null) {
                @SuppressWarnings("unchecked")
                Pair<Integer, AddressWrapper> address = Pair.class.cast(am.getTag());
                return address;
            }
            return null;
        }
    };

    private void itemActionCopy(int itemPosition, AddressWrapper item) {
        Log.d(TAG, "Copy address at position " + itemPosition + ", " + item);
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Address", item.getAddress());
        clipboard.setPrimaryClip(clip);
        finishActionMode();
        Snackbar.make(findViewById(android.R.id.content),
                UIUtils.toFriendlySnackbarString(this, getString(R.string.snackbar_address_copied)),
                Snackbar.LENGTH_LONG)
                .show();
    }

    private void itemActionEdit(int itemPosition, AddressWrapper item) {
        Log.d(TAG, "Edit address at position " + itemPosition + ", " + item);
        showAddAddressDialog(item);
        // the adapter is notified about the change later in onNewOrChangedAddress
        // user is notified after the change
        finishActionMode();
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

        boolean removed = addressListAdapter.getItems().remove(item);
        if (removed) {
            addressListAdapter.notifyItemRemoved(itemPosition);
        }

        finishActionMode();
        Snackbar.make(findViewById(android.R.id.content),
                UIUtils.toFriendlySnackbarString(this, getString(R.string.addresses_msg_removed)),
                Snackbar.LENGTH_LONG)
                .show();
    }

    private void finishActionMode() {
        if (actionMode != null) {
            actionMode.finish();
        }
    }

}
