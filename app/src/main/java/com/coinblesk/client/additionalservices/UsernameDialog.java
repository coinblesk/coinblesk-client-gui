package com.coinblesk.client.additionalservices;

import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.coinblesk.client.R;

/**
 * Created by draft on 16.06.16.
 */
public class UsernameDialog extends DialogFragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.username_password, container);
        //mEditText = (EditText) view.findViewById(R.id.txt_your_name);
        //getDialog().setTitle("Hello");

        return view;
    }
}
