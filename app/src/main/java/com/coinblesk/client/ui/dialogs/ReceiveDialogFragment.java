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

package com.coinblesk.client.ui.dialogs;


import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.coinblesk.client.R;
import com.coinblesk.client.config.Constants;
import com.coinblesk.client.utils.ClientUtils;

import org.bitcoinj.uri.BitcoinURI;
import org.bitcoinj.uri.BitcoinURIParseException;

/**
 * Created by ckiller
 */

public class ReceiveDialogFragment extends DialogFragment {
    private final static String TAG = ReceiveDialogFragment.class.getName();
    public static final String BITCOIN_URI_KEY = "BITCOIN_URI_KEY";

    public static DialogFragment newInstance(BitcoinURI bitcoinURI) {
        DialogFragment fragment = new ReceiveDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putString(BITCOIN_URI_KEY, ClientUtils.bitcoinUriToString(bitcoinURI));
        fragment.setArguments(arguments);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final String bitcoinUriString = this.getArguments().getString(BITCOIN_URI_KEY);

        try {
            final BitcoinURI bitcoinURI = new BitcoinURI(bitcoinUriString);
            final View view = inflater.inflate(R.layout.fragment_receive_alertdialog, null);
            view.findViewById(R.id.receive_email_touch_area).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
                    emailIntent.setType("text/html");
                    emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, String.format(getString(R.string.payment_request_html_subject)));
                    emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, Html.fromHtml(String.format(getString(R.string.payment_request_html_content), bitcoinUriString, bitcoinURI.getAmount().toFriendlyString(), bitcoinURI.getAddress())));
                    startActivity(Intent.createChooser(emailIntent, "Email:"));
                }
            });

            view.findViewById(R.id.receive_qrcode_touch_area).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    QrDialogFragment.newInstance(bitcoinURI).show(getFragmentManager(), "qr-fragment");
                }
            });

            view.findViewById(R.id.receive_contactless_touch_area).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent action = new Intent(Constants.START_SERVERS_ACTION);
                    action.putExtra(BITCOIN_URI_KEY, bitcoinUriString);
                    LocalBroadcastManager.getInstance(getContext()).sendBroadcast(action);
                }
            });

            return view;
        } catch (BitcoinURIParseException e) {
            Log.e(TAG, "Could not parse Bitcoin URI: ", e);
        }
        return null;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setTitle(R.string.fragment_receive_title);
        return dialog;
    }
}

