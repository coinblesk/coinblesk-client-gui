package com.coinblesk.client.wallet;

import android.content.*;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.*;
import android.widget.ProgressBar;
import com.coinblesk.client.R;
import com.coinblesk.client.helpers.UIUtils;
import com.coinblesk.client.ui.dialogs.QrDialogFragment;
import com.coinblesk.client.ui.dialogs.SendDialogFragment;
import com.coinblesk.client.ui.widget.RecyclerView;
import com.coinblesk.payments.Constants;
import com.coinblesk.payments.WalletService;
import com.coinblesk.payments.models.TimeLockedAddressWrapper;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.uri.BitcoinURI;
import org.bitcoinj.uri.BitcoinURIParseException;

import java.util.Collections;
import java.util.Map;


/**
 * @author Andreas Albrecht
 */
public class WalletAddressList extends Fragment implements WalletAddressListAdapter.ItemClickListener {

    private static final String TAG = WalletAddressList.class.getName();

    private WalletService.WalletServiceBinder walletService;

    private WalletAddressListAdapter adapter;
    private RecyclerView recyclerView;
    private AsyncTask<Void, Void, TimeLockedAddressWrapper> task;

    public static Fragment newInstance() {
        return new WalletAddressList();
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        Intent walletServiceIntent = new Intent(getContext(), WalletService.class);
        getActivity().bindService(walletServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        setHasOptionsMenu(true);
        adapter = new WalletAddressListAdapter();
        adapter.setItemClickListener(this);
    }

    @Override
    public void onStart() {
        super.onStart();

        IntentFilter walletEventFilter = new IntentFilter();
        walletEventFilter.addAction(Constants.WALLET_BALANCE_CHANGED_ACTION);
        walletEventFilter.addAction(Constants.WALLET_SCRIPTS_CHANGED_ACTION);
        LocalBroadcastManager
                .getInstance(getContext())
                .registerReceiver(walletEventReceiver, walletEventFilter);
    }

    @Override
    public void onStop() {
        super.onStop();
        LocalBroadcastManager
                .getInstance(getContext())
                .unregisterReceiver(walletEventReceiver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (task != null && task.getStatus() != AsyncTask.Status.FINISHED) {
            task.cancel(false);
        }
        task = null;
        getActivity().unbindService(serviceConnection);
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

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_wallet_addresses, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int id = item.getItemId();

        switch (id) {
            case R.id.menu_wallet_refresh:
                refreshAddresses();
                return true;
            case R.id.menu_wallet_refund:
                collectRefund();
                return true;
            case R.id.menu_wallet_addresses_create_time_locked_address:
                if (task == null) {
                    // create new task iff not already running.
                    task = createNewAddress();
                }
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private synchronized void refreshAddresses() {
        Map<Address, Coin> balances = walletService.getBalanceByAddress();
        adapter.setBalanceByAddress(balances);
        adapter.getItems().clear();
        adapter.getItems().addAll(walletService.getAddresses());
        Collections.sort(adapter.getItems(), new TimeLockedAddressWrapper.TimeCreatedComparator(false));
        adapter.notifyDataSetChanged();
        Log.d(TAG, "Update addresses, total addresses=" + adapter.getItems().size());
    }

    private void collectRefund() {
        final Coin refundBalance = walletService.getBalanceUnlocked();
        Log.d(TAG, "Collect refund - available amount (unlocked): " + refundBalance.getValue());
        if (refundBalance.isPositive()) {
            DialogFragment sendDialog = SendDialogFragment.newInstance(refundBalance);
            sendDialog.show(getFragmentManager(), "collect_refund_send_dialog");
        } else {
            showDialogAmountTooSmall(refundBalance);
        }
    }

    private void showDialogAmountTooSmall(Coin amount) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.AlertDialogAccent);
        AlertDialog dialog = builder
                .setNegativeButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setTitle(R.string.dialog_refund_too_small_title)
                .setMessage(getString(R.string.dialog_refund_too_small_message, amount.toFriendlyString()))
                .create();
        dialog.show();
    }

    private AsyncTask<Void, Void, TimeLockedAddressWrapper> createNewAddress() {
        Log.d(TAG, "Create new address.");
        return new CreateAddressTask().execute();
    }

    @Override
    public void onItemClick(TimeLockedAddressWrapper item, int position) {
        try {
            Address address = item.getTimeLockedAddress().getAddress(Constants.PARAMS);
            String uri = BitcoinURI.convertToBitcoinURI(address, null, null, null);
            BitcoinURI addressUri = new BitcoinURI(uri);
            QrDialogFragment
                    .newInstance(addressUri)
                    .show(getFragmentManager(), "address_qr_fragment");
        } catch (BitcoinURIParseException e) {
            Log.w(TAG, "Could not create bitcoin uri: ", e);
        }
    }

    private ProgressBar getProgressBar() {
        View progressBar = null;
        if (getActivity() != null && !getActivity().isDestroyed()) {
            progressBar = getActivity().findViewById(R.id.wallet_progressBar);
        }
        return (progressBar != null) ? (ProgressBar) progressBar : null;
    }

    private void startProgress() {
        ProgressBar progressBar = getProgressBar();
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
    }

    private void stopProgress() {
        ProgressBar progressBar = getProgressBar();
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
    }

    private class CreateAddressTask extends AsyncTask<Void, Void, TimeLockedAddressWrapper> {

        private Exception thrownException;

        @Override
        protected void onPreExecute () {
            // runs on UI thread
            startProgress();
        }

        @Override
        protected TimeLockedAddressWrapper doInBackground(Void... params) {
            // runs on background thread
            TimeLockedAddressWrapper newAddress;
            try {
                newAddress = walletService.createTimeLockedAddress();
            } catch (Exception e) {
                thrownException = e;
                Log.w(TAG, "Could not create new address: ", e);
                newAddress = null;
            }
            return newAddress;
        }

        @Override
        protected void onPostExecute (TimeLockedAddressWrapper result) {
            if (getActivity() == null || getActivity().isDestroyed()) {
                // may happen if user goes back before task completed.
                return;
            }

            // runs on UI thread
            stopProgress();
            task = null;

            if (thrownException != null || result == null) {
                String errorMsg = (thrownException != null) ? thrownException.getMessage() : "unknown";
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.AlertDialogAccent);
                builder
                        .setNegativeButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .setTitle(R.string.wallet_create_address_failed_title)
                        .setMessage(getString(R.string.wallet_create_address_failed_message, errorMsg))
                        .create()
                        .show();
            } else if (result != null) {
                Address address = result.getTimeLockedAddress().getAddress(Constants.PARAMS);
                long lockTime = result.getTimeLockedAddress().getLockTime();
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.AlertDialogAccent);
                builder
                        .setNegativeButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .setTitle(R.string.wallet_create_address_success_title)
                        .setMessage(getString(R.string.wallet_create_address_success_message,
                                address.toBase58(), UIUtils.lockedUntilText(lockTime)))
                        .create()
                        .show();
            }
        }

        @Override
        protected void onCancelled (TimeLockedAddressWrapper result) {
            stopProgress();
            task = null;
        }
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder binder) {
            walletService = (WalletService.WalletServiceBinder) binder;
            refreshAddresses();
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            walletService = null;
        }
    };

    private final BroadcastReceiver walletEventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshAddresses();
        }
    };

}