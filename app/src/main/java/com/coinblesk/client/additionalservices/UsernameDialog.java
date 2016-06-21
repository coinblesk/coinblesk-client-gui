package com.coinblesk.client.additionalservices;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.coinblesk.client.R;

/**
 * Created by draft on 16.06.16.
 */
public class UsernameDialog extends DialogFragment {

    private static final String TAG = UsernameDialog.class.getName();
    /*@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.additional_services_username_password, container);
        //mEditText = (EditText) view.findViewById(R.id.txt_your_name);
        //getDialog().setTitle("Hello");

        return view;
    }*/

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.additional_services_username_password, null);
        //labelEditText = (EditText) view.findViewById(R.id.txtAddressLabel);
        //addressEditText = (EditText) view.findViewById(R.id.txtAddress);

        final Bundle args = getArguments();
        String address = null;
        String addressLabel = null;
        /*if (args != null) {
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
        labelEditText.setText(addressLabel);*/
        Log.d(TAG, "onCreateDialog with address=" + address + ", and addressLabel=" + addressLabel);

        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.edit_login)
                .setView(view)
                .setPositiveButton(R.string.ok, null) // will be set later in onResume
                .setNegativeButton(R.string.cancel, null)
                .setNeutralButton(R.string.signup, null)
                .create();
    }
}
