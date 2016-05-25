package com.coinblesk.client.ui.dialogs;

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

import com.coinblesk.client.AppConstants;
import com.coinblesk.client.R;
import com.coinblesk.client.addresses.AddressItem;
import com.coinblesk.client.addresses.AddressList;
import com.coinblesk.client.addresses.AddressListAdapter;
import com.coinblesk.client.utils.UIUtils;
import com.coinblesk.client.config.Constants;
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

    private final static String TAG = SendDialogFragment.class.getName();

    private static final String ARGS_KEY_AMOUNT = "ARGS_KEY_AMOUNT";
    private static final String ARGS_KEY_ADDRESS = "ARGS_KEY_ADDRESS";

    private EditText addressEditText;
    private EditText amountEditText;

    private SendDialogListener listener;

    public static DialogFragment newInstance(Coin amount) {
        DialogFragment fragment = new SendDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putLong(ARGS_KEY_AMOUNT, amount.value);
        fragment.setArguments(arguments);
        return fragment;
    }

    public static DialogFragment newInstance(Address address, Coin amount) {
        DialogFragment fragment = new SendDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putLong(ARGS_KEY_AMOUNT, amount.value);
        arguments.putString(ARGS_KEY_ADDRESS, address.toString());
        fragment.setArguments(arguments);
        return fragment;
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_send_dialog, container);
        addressEditText = (EditText) view.findViewById(R.id.address_edit_text);
        final String addressStr = getArguments().getString(ARGS_KEY_ADDRESS, "");
        try {
            Address address = Address.fromBase58(Constants.PARAMS, addressStr);
            addressEditText.setText(address.toString());
        } catch (AddressFormatException e) {
            Log.w(TAG, "Could not parse address: " + addressStr);
        }

        final Coin amount = Coin.valueOf(getArguments().getLong(ARGS_KEY_AMOUNT, 0));
        amountEditText = (EditText) view.findViewById(R.id.amount_edit_text);
        amountEditText.setText(UIUtils.scaleCoinForDialogs(amount, getContext()));

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
            public void onItemClick(AddressItem item, int itemPosition) {
                if (item != null) {
                    addressEditText.setText(item.getAddress());
                }
                addressListDialog.dismiss();
            }

            @Override
            public boolean onItemLongClick(AddressItem item, int itemPosition) {
                // long click is ignored
                return false;
            }
        });
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AppConstants.QR_ACTIVITY_RESULT_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                final String scanContent = data.getStringExtra(Intents.Scan.RESULT);
                try {
                    BitcoinURI bitcoinURI = new BitcoinURI(scanContent);
                    addressEditText.setText(bitcoinURI.getAddress().toString());
                } catch (BitcoinURIParseException e) {
                    Log.w(TAG, "Could not parse scanned content: '" + scanContent + "'", e);
                    addressEditText.setText(scanContent);
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
            Coin amount = Coin.valueOf(getArguments().getLong(ARGS_KEY_AMOUNT, 0));
            Address sendTo = Address.fromBase58(Constants.PARAMS, addressEditText.getText().toString().trim());
            if (listener != null) {
                listener.sendCoins(sendTo, amount);
            }
            dismiss();
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
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof SendDialogListener) {
            listener = (SendDialogListener) context;
        } else {
            Log.w(TAG, "onAttach - context does not implement SendDialogListener interface: " + context.getClass().getName());
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    @Override
    public void onStart() {
        super.onStart();
        Intent intent = new Intent(getActivity(), WalletService.class);
        getActivity().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop() {
        super.onStop();
        getActivity().unbindService(serviceConnection);
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(walletCoinsSentReceiver);
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            walletServiceBinder = (WalletService.WalletServiceBinder) binder;

            // TODO: is this broadcast receiver still required / used?
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

    public interface SendDialogListener {
        /**
         * Called when "send" is clicked.
         * @param address
         * @param amount
         */
        void sendCoins(Address address, Coin amount);
    }
}

