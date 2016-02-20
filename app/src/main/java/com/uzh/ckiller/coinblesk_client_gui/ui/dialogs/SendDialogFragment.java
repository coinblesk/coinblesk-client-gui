package com.uzh.ckiller.coinblesk_client_gui.ui.dialogs;

//import android.support.v7.app.AlertDialog;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.support.v7.widget.Toolbar;
import android.support.design.widget.TextInputLayout;


import com.google.zxing.client.android.Intents;
import com.google.zxing.integration.android.IntentIntegrator;
import com.uzh.ckiller.coinblesk_client_gui.R;

/**
 * Created by ckiller
 */

public class SendDialogFragment extends DialogFragment {
    public static final int REQUEST_CODE = 0x0000c0de; // Only use bottom 16 bits
    private final static String TAG = SendDialogFragment.class.getName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setStyle(DialogFragment.STYLE_NO_FRAME, android.R.style.Theme_Material);
        setStyle(DialogFragment.STYLE_NO_FRAME, android.R.style.ThemeOverlay_Material_Dark);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        this.getDialog().setTitle(R.string.send_dialog_title);
        View view = inflater.inflate(R.layout.fragment_send_dialog, container);
        view.findViewById(R.id.qr_scan_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IntentIntegrator.forSupportFragment(SendDialogFragment.this).initiateScan();
            }
        });

        Toolbar actionBar = (Toolbar) view.findViewById(R.id.fake_action_bar);
        if (actionBar!=null) {
            final SendDialogFragment window = this;
            actionBar.setTitle("Send Bitcoins");
            actionBar.setNavigationOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    window.dismiss();
                }
            });
        }


        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                final String contents = data.getStringExtra(Intents.Scan.RESULT);
                ((EditText)this.getView().findViewById(R.id.address_edit_text)).setText(contents);
            }
        }
    }
}
