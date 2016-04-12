package com.coinblesk.client.addresses;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;

import android.widget.EditText;
import android.widget.Toast;
import com.coinblesk.client.R;
import com.coinblesk.payments.Constants;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.WrongNetworkException;


public class EditAddressFragment extends DialogFragment {

    private static final String ARG_ADDRESS_TITLE = "address_title";
    private static final String ARG_ADDRESS = "address";

    private EditText titleEditText;
    private EditText addressEditText;
    private AddressFragmentInteractionListener listener;

    public EditAddressFragment() {
    }

    public static DialogFragment newInstance(String addressTitle, String address) {
        EditAddressFragment fragment = new EditAddressFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ADDRESS_TITLE, addressTitle);
        args.putString(ARG_ADDRESS, address);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.fragment_edit_address, null);
        titleEditText = (EditText) view.findViewById(R.id.txtTitle);
        addressEditText = (EditText) view.findViewById(R.id.txtAddress);

        if (getArguments() != null) {
            titleEditText.setText(getArguments().getString(ARG_ADDRESS_TITLE));
            addressEditText.setText(getArguments().getString(ARG_ADDRESS));
        }

         return new AlertDialog.Builder(getActivity())
                .setTitle("Address")
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
                String title = titleEditText.getText().toString().trim();
                String address = addressEditText.getText().toString().trim();
                Address tmp = new Address(Constants.PARAMS, address); // only for validation and correctness
                AddressWrapper addressWrapper = new AddressWrapper(title, address);
                listener.onNewOrChangedAddress(addressWrapper);
                dialog.dismiss();
            } catch (WrongNetworkException e) {
                Toast.makeText(getDialog().getContext(),
                        R.string.send_address_wrong_network,
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

    public interface AddressFragmentInteractionListener {
        void onNewOrChangedAddress(AddressWrapper address);
    }
}
