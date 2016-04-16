package com.coinblesk.client.ui.dialogs;

//import android.support.v7.app.AlertDialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.*;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import com.coinblesk.client.R;
import com.coinblesk.client.addresses.AddressList;
import com.coinblesk.client.addresses.AddressListAdapter;
import com.coinblesk.client.addresses.AddressWrapper;
import com.coinblesk.client.helpers.UIUtils;
import com.coinblesk.payments.Constants;
import com.coinblesk.payments.WalletService;
import com.google.zxing.client.android.Intents;
import com.google.zxing.integration.android.IntentIntegrator;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.WrongNetworkException;
import org.bitcoinj.uri.BitcoinURI;
import org.bitcoinj.uri.BitcoinURIParseException;

/**
 * Created by ckiller
 */

public class SendDialogFragment extends DialogFragment
                                            implements View.OnClickListener {

    public static final int REQUEST_CODE = 0x0000c0de; // Only use bottom 16 bits

    public static final String AMOUNT_KEY = "AMOUNT_KEY";
    public static final String ADDRESS_KEY = "ADDRESS_KEY";

    private final static String TAG = SendDialogFragment.class.getName();

    private EditText addressEditText;
    private EditText amountEditText;

    public static DialogFragment newInstance(Coin amount) {
        DialogFragment fragment = new SendDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putLong(AMOUNT_KEY, amount.value);
        fragment.setArguments(arguments);
        return fragment;
    }

    public static DialogFragment newInstance(Address address, Coin amount) {
        DialogFragment fragment = new SendDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putLong(AMOUNT_KEY, amount.value);
        arguments.putString(ADDRESS_KEY, address.toString());
        fragment.setArguments(arguments);
        return fragment;
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_send_dialog, container);
        this.addressEditText = (EditText) view.findViewById(R.id.address_edit_text);
        final String addressStr = getArguments().getString(ADDRESS_KEY, "");
        try {
            Address address = new Address(Constants.PARAMS, addressStr);
            this.addressEditText.setText(address.toString());
        } catch (AddressFormatException e) {
            Log.w(TAG, "Could not parse address: " + addressStr);
        }

        Coin amount = Coin.valueOf(this.getArguments().getLong(AMOUNT_KEY, 0));
        this.amountEditText = (EditText) view.findViewById(R.id.amount_edit_text);
        this.amountEditText.setText(UIUtils.scaleCoinForDialogs(amount, getContext()));

        view.findViewById(R.id.fragment_send_dialog_cancel).setOnClickListener(this);
        view.findViewById(R.id.fragment_send_dialog_qr_scan).setOnClickListener(this);
        view.findViewById(R.id.fragment_send_dialog_send).setOnClickListener(this);
        view.findViewById(R.id.fragment_send_dialog_address).setOnClickListener(this);

        return view;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setTitle(R.string.fragment_send_dialog_title);
        return dialog;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fragment_send_dialog_send:
                sendCoins();
                break;
            case R.id.fragment_send_dialog_cancel:
                getDialog().cancel();
                break;
            case R.id.fragment_send_dialog_qr_scan:
                IntentIntegrator.forSupportFragment(SendDialogFragment.this).initiateScan();
                break;
            case R.id.fragment_send_dialog_address:
                openAddressList();
                break;
        }
    }

    private void openAddressList() {
        final AddressList addressListDialog = AddressList.newInstance();
        addressListDialog.show(getFragmentManager(), "address_list_dialog");
        addressListDialog.setItemClickListener(new AddressListAdapter.AddressItemClickListener() {
            @Override
            public void onItemClick(AddressWrapper item, int itemPosition) {
                if (item != null) {
                    addressEditText.setText(item.getAddress());
                }
                addressListDialog.dismiss();
            }

            @Override
            public boolean onItemLongClick(AddressWrapper item, int itemPosition) {
                // long click is ignored
                return false;
            }
        });
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                final String contents = data.getStringExtra(Intents.Scan.RESULT);
                try {
                    BitcoinURI bitcoinURI = new BitcoinURI(contents);
                    this.addressEditText.setText(bitcoinURI.getAddress().toString());
                } catch (BitcoinURIParseException e) {
                    this.addressEditText.setText(contents);
                }
            }
        }
    }


    /* ------------------- PAYMENTS INTEGRATION STARTS HERE  ------------------- */
    private final BroadcastReceiver walletCoinsSentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            dismiss();
        }
    };

    private void sendCoins() {
        try {
            Coin amount = Coin.valueOf(getArguments().getLong(AMOUNT_KEY, 0));
            Address sendTo = new Address(Constants.PARAMS, addressEditText.getText().toString());
            walletServiceBinder.sendCoins(sendTo, amount);
        } catch (WrongNetworkException e) {
            Toast.makeText(getContext(),
                    getString(R.string.send_address_wrong_network, Constants.PARAMS.getId()),
                    Toast.LENGTH_SHORT)
                    .show();
        } catch (AddressFormatException e) {
            Toast.makeText(getContext(), R.string.send_address_parse_error, Toast.LENGTH_SHORT).show();
        }
    }

    private WalletService.WalletServiceBinder walletServiceBinder;

    @Override
    public void onStart() {
        super.onStart();
        Intent intent = new Intent(this.getActivity(), WalletService.class);
        this.getActivity().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop() {
        super.onStop();
        this.getActivity().unbindService(serviceConnection);
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(walletCoinsSentReceiver);
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            walletServiceBinder = (WalletService.WalletServiceBinder) binder;

            IntentFilter filter = new IntentFilter(Constants.WALLET_COINS_SENT_ACTION);
            filter.addAction(Constants.INSTANT_PAYMENT_SUCCESSFUL_ACTION);
            filter.addAction(Constants.INSTANT_PAYMENT_FAILED_ACTION);
            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(walletCoinsSentReceiver, filter);
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            walletServiceBinder = null;
        }
    };

    /* -------------------- PAYMENTS INTEGRATION ENDS HERE  -------------------- */

}

