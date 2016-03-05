package com.uzh.ckiller.coinblesk_client_gui.ui.dialogs;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.uzh.ckiller.coinblesk_client_gui.R;
import com.uzh.ckiller.coinblesk_client_gui.authview.AuthenticationView;

import org.bitcoinj.uri.BitcoinURI;
import org.bitcoinj.uri.BitcoinURIParseException;

import ch.papers.payments.Utils;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 05/03/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class AuthViewDialogFragment extends DialogFragment {
    private final static String TAG = AuthViewDialogFragment.class.getName();
    public final static String AUTH_PAYLOAD_KEY = "AUTH_PAYLOAD_KEY";

    public static DialogFragment newInstance(BitcoinURI bitcoinURI){
        String payload = BitcoinURI.convertToBitcoinURI(bitcoinURI.getAddress(), bitcoinURI.getAmount(), bitcoinURI.getLabel(), bitcoinURI.getMessage());
        final DialogFragment dialogFragment = new AuthViewDialogFragment();
        final Bundle arguments = new Bundle();
        arguments.putString(AUTH_PAYLOAD_KEY,payload);
        dialogFragment.setArguments(arguments);
        return dialogFragment;
    }

    private BitcoinURI bitcoinURI;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            this.bitcoinURI = new BitcoinURI(this.getArguments().getString(AUTH_PAYLOAD_KEY));
        } catch (BitcoinURIParseException e) {
            e.printStackTrace();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_authview_dialog, container);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        final TextView amountTextView = (TextView) view.findViewById(R.id.amount_textview);
        amountTextView.setText(bitcoinURI.getAmount().toString());
        final TextView addressTextView = (TextView) view.findViewById(R.id.address_textview);
        addressTextView.setText(bitcoinURI.getAddress().toString());

        view.findViewById(R.id.authview_dialog_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDialog().cancel();
            }
        });

        view.findViewById(R.id.authview_dialog_accept).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDialog().dismiss();
            }
        });


        final LinearLayout authviewContainer = (LinearLayout) view.findViewById(R.id.authview_container);
        authviewContainer.addView(new AuthenticationView(this.getContext(), Utils.bitcoinUriToString(bitcoinURI).getBytes()));

        return view;
    }
}
