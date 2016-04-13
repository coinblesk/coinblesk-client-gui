package com.coinblesk.client.addresses;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;

import android.widget.EditText;
import android.widget.Toast;
import com.coinblesk.client.R;
import com.coinblesk.payments.Constants;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.WrongNetworkException;


/**
 * This dialog fragment allows adding and editing addresses of the address book.
 */
public class EditAddressFragment extends DialogFragment {

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
        if (args != null) {
            if (args.containsKey(ARG_ADDRESS_LABEL)) {
                labelEditText.setText(args.getString(ARG_ADDRESS_LABEL));
            }
            if (args.containsKey(ARG_ADDRESS)) {
                // if we get an address, we only display it but do not permit
                // editing to avoid saving a wrong address.
                addressEditText.setEnabled(false);
                addressEditText.setInputType(InputType.TYPE_NULL);
                addressEditText.setText(args.getString(ARG_ADDRESS));
            }
        }

         return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.edit_address_fragment_title)
                .setView(view)
                .setPositiveButton(R.string.ok, new OnOkClickListener())
                .setNegativeButton(R.string.cancel, new OnCancelClickListener())
                .create();
    }

    private class OnCancelClickListener implements DialogInterface.OnClickListener {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            dialog.cancel();
        }
    }

    private class OnOkClickListener implements DialogInterface.OnClickListener {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (listener == null) {
                return;
            }

            try {
                final String label = labelEditText.getText().toString().trim();
                final String address = addressEditText.getText().toString().trim();
                new Address(Constants.PARAMS, address); // only for validation and correctness
                final AddressWrapper addressWrapper = new AddressWrapper(label, address);
                listener.onNewOrChangedAddress(addressWrapper);
                dialog.dismiss();
            } catch (WrongNetworkException e) {
                Toast.makeText(getDialog().getContext(),
                        getString(R.string.send_address_wrong_network, Constants.PARAMS.getId()),
                        Toast.LENGTH_LONG).show();
            } catch (AddressFormatException e) {
                Toast.makeText(getDialog().getContext(),
                        R.string.send_address_parse_error,
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

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
        void onNewOrChangedAddress(AddressWrapper address);
    }
}
