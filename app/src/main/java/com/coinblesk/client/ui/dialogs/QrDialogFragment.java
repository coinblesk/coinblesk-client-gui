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


import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
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
import com.coinblesk.client.utils.QREncoder;
import com.coinblesk.client.utils.UIUtils;
import com.coinblesk.client.utils.ClientUtils;
import com.google.zxing.WriterException;

import org.bitcoinj.uri.BitcoinURI;
import org.bitcoinj.uri.BitcoinURIParseException;

/**
 * @author ckiller
 * @author Andreas Albrecht
 */

public class QrDialogFragment extends DialogFragment {

    private final static String TAG = QrDialogFragment.class.getName();
    public final static String BITCOIN_URI_KEY = "BITCOIN_URI";

    private BitcoinURI bitcoinURI;

    public static DialogFragment newInstance(BitcoinURI bitcoinURI){
        String payload = ClientUtils.bitcoinUriToString(bitcoinURI);
        final DialogFragment dialogFragment = new QrDialogFragment();
        final Bundle arguments = new Bundle();
        arguments.putString(BITCOIN_URI_KEY, payload);
        dialogFragment.setArguments(arguments);
        return dialogFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            bitcoinURI = new BitcoinURI(getArguments().getString(BITCOIN_URI_KEY));
        } catch (BitcoinURIParseException e) {
            Log.e(TAG, "Could not parse Bitcoin URI: ", e);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_qr_dialog, container);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        final String uri = ClientUtils.bitcoinUriToString(bitcoinURI);

        final TextView addressTextView = (TextView) view.findViewById(R.id.address_textview);
        if (addressTextView != null && bitcoinURI.getAddress() != null) {
            addressTextView.setText(bitcoinURI.getAddress().toString());
        }

        final Button clipboardButton = (Button) view.findViewById(R.id.qr_dialog_copytoclipboard);
        if (clipboardButton != null) {
            clipboardButton.setOnClickListener(new CopyToClipboardClickListener());
        }

        final Button shareButton = (Button) view.findViewById(R.id.qr_dialog_share);
        if (shareButton != null) {
            shareButton.setOnClickListener(new ShareClickListener());
        }

        try {
            final ImageView qrCodeImageView = (ImageView) view.findViewById(R.id.qr_code);
            if (qrCodeImageView != null) {
                Bitmap qrBitmap = QREncoder.encodeAsBitmap(uri);
                qrCodeImageView.setImageBitmap(qrBitmap);
            }
        } catch (WriterException e) {
            Log.e(TAG, "Could not create QR Code.", e);
        }

        Log.d(TAG, "Show QR Code for BitcoinURI: " + uri);
        return view;
    }

    private class CopyToClipboardClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            copyToClipboard();
            showMessage();
        }

        private void copyToClipboard() {
            String uri = BitcoinURI.convertToBitcoinURI(
                    bitcoinURI.getAddress(),
                    bitcoinURI.getAmount(),
                    bitcoinURI.getLabel(),
                    bitcoinURI.getMessage());
            ClipData clip = ClipData.newPlainText("Address", uri);
            ClipboardManager clipboard = (ClipboardManager) getActivity()
                    .getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(clip);
        }

        private void showMessage() {
            CharSequence message = UIUtils.toFriendlySnackbarString(
                    getContext(), getString(R.string.snackbar_address_copied));
            Snackbar.make(getView(), message, Snackbar.LENGTH_SHORT).show();
        }
    }

    private class ShareClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            share(ClientUtils.bitcoinUriToString(bitcoinURI));
        }

        private void share(String text) {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Bitcoin Address");
            sendIntent.putExtra(Intent.EXTRA_TEXT, text);
            sendIntent.setType("text/plain");
            startActivity(sendIntent);
        }
    }
}
