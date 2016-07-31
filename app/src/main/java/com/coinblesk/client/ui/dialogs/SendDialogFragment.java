/*
 * Copyright 2016 The Coinblesk team and the CSG Group at University of Zurich
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.coinblesk.client.ui.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.coinblesk.client.CoinbleskApp;
import com.coinblesk.client.R;
import com.coinblesk.client.models.AddressBookItem;
import com.coinblesk.client.addresses.AddressList;
import com.coinblesk.client.addresses.AddressListAdapter;
import com.coinblesk.client.config.Constants;
import com.coinblesk.client.utils.UIUtils;
import com.coinblesk.payments.WalletService;
import com.google.zxing.client.android.Intents;
import com.google.zxing.integration.android.IntentIntegrator;
import com.xeiam.xchange.Exchange;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.WrongNetworkException;
import org.bitcoinj.uri.BitcoinURI;
import org.bitcoinj.uri.BitcoinURIParseException;
import org.bitcoinj.utils.ExchangeRate;
import org.bitcoinj.utils.Fiat;

/**
 * @author ckiller
 * @author Andreas albrecht
 */
public class SendDialogFragment extends DialogFragment
                                            implements View.OnClickListener {

    private final static String TAG = SendDialogFragment.class.getName();

    private static final String ARGS_KEY_AMOUNT = "ARGS_KEY_AMOUNT";
    private static final String ARGS_KEY_ADDRESS = "ARGS_KEY_ADDRESS";

    private WalletService.WalletServiceBinder walletService;

    private EditText addressEditText;
    private EditText amountEditText;
    private EditText feeEditText;

    private NetworkParameters params;

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        params = ((CoinbleskApp) getActivity().getApplication())
                .getAppConfig()
                .getNetworkParameters();

        Intent walletServiceIntent = new Intent(getActivity(), WalletService.class);
        getActivity().bindService(walletServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy() {
        getActivity().unbindService(serviceConnection);
        super.onDestroy();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof SendDialogListener) {
            listener = (SendDialogListener) context;
        } else {
            Log.w(TAG, "onAttach - context does not implement SendDialogListener interface: "
                    + context.getClass().getName());
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_send_dialog, container);
        addressEditText = (EditText) view.findViewById(R.id.address_edit_text);
        final String addressStr = getArguments().getString(ARGS_KEY_ADDRESS, null);
        try {
            if (addressStr != null) {
                // validation
                Address address = Address.fromBase58(params, addressStr);
                addressEditText.setText(address.toBase58());
            } else {
                addressEditText.setText("");
            }
        } catch (AddressFormatException e) {
            Log.w(TAG, "Could not parse address: " + addressStr);
            addressEditText.setText("");
        }

        addressEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                refreshAmountAndFee();
            }
        });

        final Coin amount = Coin.valueOf(getArguments().getLong(ARGS_KEY_AMOUNT, 0));
        amountEditText = (EditText) view.findViewById(R.id.amount_edit_text);
        amountEditText.setText(UIUtils.coinFiatSpannable(getActivity(), amount, (ExchangeRate)null, true, 0.66f));

        feeEditText = (EditText) view.findViewById(R.id.fee_edit_text);
        feeEditText.setText(R.string.unknown);

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
                IntentIntegrator.forFragment(SendDialogFragment.this).initiateScan();
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
            public void onItemClick(AddressBookItem item, int itemPosition) {
                if (item != null) {
                    addressEditText.setText(item.getAddress());
                }
                addressListDialog.dismiss();
            }

            @Override
            public boolean onItemLongClick(AddressBookItem item, int itemPosition) {
                // long click is ignored
                return false;
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.QR_ACTIVITY_RESULT_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                final String scanContent = data.getStringExtra(Intents.Scan.RESULT);
                try {
                    BitcoinURI bitcoinURI = new BitcoinURI(scanContent);
                    if (bitcoinURI.getAddress() != null) {
                        addressEditText.setText(bitcoinURI.getAddress().toString());
                        return;
                    }
                } catch (BitcoinURIParseException e) {
                    Log.w(TAG, "Could not parse scanned content: '" + scanContent + "'", e);
                }
                // set to scanned content if no address detected
                addressEditText.setText(scanContent);
            }
        }
    }

    private void refreshAmountAndFee() {
        ExchangeRate exchangeRate = walletService != null ? walletService.getExchangeRate() : null;

        final Coin amount = Coin.valueOf(getArguments().getLong(ARGS_KEY_AMOUNT, 0));
        amountEditText.setText(UIUtils.coinFiatSpannable(getActivity(), amount, exchangeRate, true, 0.66f));

        Address addressTo = null;
        try {
            addressTo = Address.fromBase58(params, getAddressText());
        } catch (Exception e) {
            // ignore, not valid address
        }
        final Coin fee = walletService != null ? walletService.estimateFee(addressTo, amount) : null;
        if (fee != null) {
            feeEditText.setText(UIUtils.coinFiatSpannable(getActivity(), fee, exchangeRate, true, 1.0f));
        } else {
            feeEditText.setText(R.string.unknown);
        }

    }

    private void sendCoins() {
        try {
            Coin amount = Coin.valueOf(getArguments().getLong(ARGS_KEY_AMOUNT, 0));
            Address sendTo = Address.fromBase58(params, getAddressText());
            if (listener != null) {
                listener.sendCoins(sendTo, amount);
            }
            dismiss();
        } catch (WrongNetworkException e) {
            Toast.makeText(getActivity(),
                    getString(R.string.send_address_wrong_network, params.getId()),
                    Toast.LENGTH_SHORT)
                    .show();
        } catch (AddressFormatException e) {
            Toast.makeText(getActivity(),
                    R.string.send_address_parse_error, Toast.LENGTH_SHORT)
                    .show();
        }
    }

    private String getAddressText() {
        return addressEditText.getText().toString().trim();
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            walletService = (WalletService.WalletServiceBinder) service;

            refreshAmountAndFee();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            walletService = null;
        }
    };

    public interface SendDialogListener {
        /**
         * Called when "send" is clicked.
         * @param address the entered address
         * @param amount the entered amount
         */
        void sendCoins(Address address, Coin amount);
    }
}
