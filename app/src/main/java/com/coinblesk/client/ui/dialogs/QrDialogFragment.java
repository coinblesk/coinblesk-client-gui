package com.coinblesk.client.ui.dialogs;

//import android.support.v7.app.AlertDialog;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.coinblesk.client.R;
import com.coinblesk.client.helpers.QREncoder;
import com.coinblesk.client.helpers.UIUtils;
import com.coinblesk.payments.Utils;
import com.google.zxing.WriterException;

import org.bitcoinj.uri.BitcoinURI;
import org.bitcoinj.uri.BitcoinURIParseException;

/**
 * Created by ckiller
 */

public class QrDialogFragment extends DialogFragment {

    private final static String TAG = QrDialogFragment.class.getName();
    public final static String BITCOIN_URI_KEY = "BITCOIN_URI_KEY";

    public static DialogFragment newInstance(BitcoinURI bitcoinURI){
        String payload = Utils.bitcoinUriToString(bitcoinURI);
        final DialogFragment dialogFragment = new QrDialogFragment();
        final Bundle arguments = new Bundle();
        arguments.putString(BITCOIN_URI_KEY,payload);
        dialogFragment.setArguments(arguments);
        return dialogFragment;
    }

    private BitcoinURI bitcoinURI;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            this.bitcoinURI = new BitcoinURI(this.getArguments().getString(BITCOIN_URI_KEY));
        } catch (BitcoinURIParseException e) {
            e.printStackTrace();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_qr_dialog, container);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        final TextView addressTextView = (TextView) view.findViewById(R.id.address_textview);
        addressTextView.setText(bitcoinURI.getAddress().toString());

        final Button clipboardButton = (Button) view.findViewById(R.id.qr_dialog_copytoclipboard);
        clipboardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(getContext().CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Your address", BitcoinURI.convertToBitcoinURI(bitcoinURI.getAddress(),bitcoinURI.getAmount(),bitcoinURI.getLabel(),bitcoinURI.getMessage()));
                clipboard.setPrimaryClip(clip);
                Snackbar.make(getView(), UIUtils.toFriendlySnackbarString(getContext(), (getResources()
                        .getString(R.string.snackbar_address_copied))), Snackbar.LENGTH_SHORT)
                        .show();
            }
        });

        final ImageView qrCodeImageView = (ImageView) view.findViewById(R.id.qr_code);
        try {
            Bitmap qrBitmap = QREncoder.encodeAsBitmap(Utils.bitcoinUriToString(bitcoinURI));
            qrCodeImageView.setImageBitmap(qrBitmap);
        } catch (WriterException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "bitcoin uri: " + Utils.bitcoinUriToString(bitcoinURI));


        return view;
    }
}
