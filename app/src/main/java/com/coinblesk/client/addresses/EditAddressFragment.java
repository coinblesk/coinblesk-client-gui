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

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;

import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;

import android.widget.EditText;
import android.widget.Toast;
import com.coinblesk.client.R;
import com.coinblesk.client.config.Constants;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.WrongNetworkException;
import org.bitcoinj.uri.BitcoinURI;
import org.bitcoinj.uri.BitcoinURIParseException;


/**
 * This dialog fragment allows adding and editing addresses of the address book.
 *
 * @author Andreas Albrecht
 */
public class EditAddressFragment extends DialogFragment {
    private static final String TAG = EditAddressFragment.class.getName();
    private static final String ARG_ADDRESS_LABEL = "address_label";
    private static final String ARG_ADDRESS = "address";

    private EditText labelEditText;
    private EditText addressEditText;
    private AddressFragmentInteractionListener listener;

    public EditAddressFragment() {
    }

    public static DialogFragment newInstance(String addressLabel, String address) {
        EditAddressFragment fragment = new EditAddressFragment();
        Bundle args = new Bundle();
        if (addressLabel != null) {
            args.putString(ARG_ADDRESS_LABEL, addressLabel);
        }
        if (address != null) {
            args.putString(ARG_ADDRESS, address);
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.fragment_edit_address, null);
        labelEditText = (EditText) view.findViewById(R.id.txtAddressLabel);
        addressEditText = (EditText) view.findViewById(R.id.txtAddress);

        final Bundle args = getArguments();
        String address = null;
        String addressLabel = null;
        if (args != null) {
            if (args.containsKey(ARG_ADDRESS_LABEL)) {
                addressLabel = args.getString(ARG_ADDRESS_LABEL);

            }
            if (args.containsKey(ARG_ADDRESS)) {
                // if we get an address, we only display it but do not permit
                // editing to avoid saving a wrong address.
                addressEditText.setEnabled(false);
                addressEditText.setInputType(InputType.TYPE_NULL);
                address = args.getString(ARG_ADDRESS);
            }
        }
        addressEditText.setText(address);
        labelEditText.setText(addressLabel);
        Log.d(TAG, "onCreateDialog with address=" + address + ", and addressLabel=" + addressLabel);

        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.edit_address_fragment_title)
                .setView(view)
                .setPositiveButton(R.string.ok, null) // will be set later in onResume
                .setNegativeButton(R.string.cancel, null)
                .create();
    }

    @Override
    public void onResume() {
        super.onResume();

        /* the click listener is set here such that we can perform input validation (is address correct, and so on).
         * unfortunately, the AlertDialog with the builder usually does autoDismiss with an additional after a button click.
         * Thus, we set the click listener manually after show().
        */
        final AlertDialog alertDialog = (AlertDialog) getDialog();
        alertDialog
                .getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new OnOkClickListener().onClick(alertDialog, AlertDialog.BUTTON_POSITIVE);
                    }
                });
    }

    private class OnOkClickListener implements DialogInterface.OnClickListener {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (listener == null) {
                return;
            }

            String label = labelEditText.getText().toString().trim();
            String address = addressEditText.getText().toString().trim();
            Pair<Boolean, String> result;

            // LABEL VALIDATION
            result = validateLabel(label);
            if (result.first) {
                label = result.second;
            } else {
                showToastError(result.second);
                return;
            }


            // ADDRESS VALIDATION
            result = addressValidation(address);
            if (result.first) {
                address = result.second;
            } else {
                showToastError(result.second);
                return;
            }

            // everything OK
            final AddressItem addressItem = new AddressItem(label, address);
            listener.onNewOrChangedAddress(addressItem);
            dialog.dismiss();
        }
    }

    private void showToastError(String message) {
        Toast.makeText(getDialog().getContext(),
                message,
                Toast.LENGTH_SHORT)
                .show();
    }

    private Pair<Boolean,String> addressValidation(String address) {
        // the address can be a Bitcoin address OR an bitcoin Uri from e.g. a QR Code
        // (from which we can extract the address part).
        String addressError = null;
        try {
            BitcoinURI bitcoinURI = new BitcoinURI(address);
            if (bitcoinURI.getAddress() != null) {
                address = bitcoinURI.getAddress().toString();
                return Pair.create(true, address);
            }
        } catch (BitcoinURIParseException e) {
            // ignore because we try a different approach below.
        }

        try {
            // bitcoinURI parsing failed. now try as regular address string
            Address btcAddress = new Address(Constants.PARAMS, address);
            return Pair.create(true, address);
        } catch (WrongNetworkException e) {
            addressError = getString(R.string.send_address_wrong_network, Constants.PARAMS.getId());
        } catch (AddressFormatException e) {
            addressError = getString(R.string.send_address_parse_error);
        }

        // all conversions failed.
        return Pair.create(false, addressError);
    }

    private Pair<Boolean, String> validateLabel(String addressLabel) {
        if (addressLabel.isEmpty()) {
            return Pair.create(false, getString(R.string.edit_address_msg_emptylabel));
        }

        return Pair.create(true, addressLabel);
    }

    /*
     * The issue is that depending on the API version, onAttach(context) or onAttach(activity)
     * gets called. Thus, we have both here because we need the context to set the listener
     * in onAttachToContext().
     */
    @TargetApi(23)
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        onAttachToContext(context);
    }

    /*
     * Deprecated on API 23
     * Use onAttachToContext instead
     */
    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (Build.VERSION.SDK_INT < 23) {
            onAttachToContext(activity);
        }
    }

    private void onAttachToContext(Context context) {
        if (context instanceof AddressFragmentInteractionListener) {
            listener = (AddressFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement AddressFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    interface AddressFragmentInteractionListener {
        /**
         * Called if item is added or changed.
         * Note: the address reference may not point to the same
         *       object even if the item is changed and not new.
         * @param address
         */
        void onNewOrChangedAddress(AddressItem address);
    }
}
