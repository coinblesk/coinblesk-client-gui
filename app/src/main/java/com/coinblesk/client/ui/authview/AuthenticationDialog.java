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

package com.coinblesk.client.ui.authview;


import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.coinblesk.client.R;
import com.coinblesk.client.utils.ClientUtils;
import com.coinblesk.client.utils.UIUtils;
import com.coinblesk.payments.WalletService;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.uri.BitcoinURI;
import org.bitcoinj.utils.ExchangeRate;

/**
 * @author Andreas Albrecht
 */
public class AuthenticationDialog extends DialogFragment {

    private static final String TAG = AuthenticationDialog.class.getName();

    private static final String ARG_ADDRESS = "ADDRESS";
    private static final String ARG_AMOUNT = "AMOUNT";
    private static final String ARG_PAYMENT_REQUEST = "PAYMENT_REQUEST";
    private static final String ARG_SHOW_ACCEPT = "SHOW_ACCEPT";

    private View authView;

    private String address;
    private Coin amount;

    private WalletService.WalletServiceBinder walletService;

    private AuthenticationDialogListener listener;

    public static AuthenticationDialog newInstance(BitcoinURI paymentRequest, boolean showAccept) {
        return newInstance(
                paymentRequest.getAddress(),
                paymentRequest.getAmount(),
                ClientUtils.bitcoinUriToString(paymentRequest),
                showAccept);
    }

    public static AuthenticationDialog newInstance(Address address,
                                                   Coin amount,
                                                   String paymentRequestStr,
                                                   boolean showAccept) {
        AuthenticationDialog frag = new AuthenticationDialog();
        Bundle args = new Bundle();
        args.putString(ARG_ADDRESS, address.toString());
        args.putLong(ARG_AMOUNT, amount.getValue());
        args.putString(ARG_PAYMENT_REQUEST, paymentRequestStr);
        args.putBoolean(ARG_SHOW_ACCEPT, showAccept);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        getActivity().setRequestedOrientation(
                ActivityInfo.SCREEN_ORIENTATION_LOCKED);

        Intent walletServiceIntent = new Intent(getContext(), WalletService.class);
        getActivity().bindService(walletServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        setCancelable(false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().setRequestedOrientation(
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        if (listener != null) {
            listener.authViewDestroy();
        }

        getActivity().unbindService(serviceConnection);
    }

     @Override
    public void onAttach(Context context) {
        super.onAttach(context);
         if (context instanceof AuthenticationDialogListener) {
             listener = (AuthenticationDialogListener) context;
         } else {
             Log.e(TAG, "onAttach - context does not implement AuthenticationDialogListener");
         }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        address = getArguments().getString(ARG_ADDRESS);
        amount = Coin.valueOf(getArguments().getLong(ARG_AMOUNT));

        final String paymentReq = getArguments().getString(ARG_PAYMENT_REQUEST);
        final boolean showAccept = getArguments().getBoolean(ARG_SHOW_ACCEPT);

        authView = getActivity().getLayoutInflater().inflate(R.layout.fragment_authview_dialog, null);

        final TextView addressTextView = (TextView) authView.findViewById(R.id.authview_address_content);
        addressTextView.setText(address);

        final LinearLayout authviewContainer = (LinearLayout) authView.findViewById(R.id.authview_container);
        authviewContainer.addView(new AuthenticationView(getContext(), paymentReq.getBytes()));

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.AlertDialogAccent);
        builder
            .setTitle(R.string.authview_title)
            .setView(authView)
            .setCancelable(false)
            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (listener != null) {
                        listener.authViewNegativeResponse();
                    }
                }
            });
            if (showAccept) {
                builder.setPositiveButton(R.string.accept, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (listener != null) {
                            listener.authViewPositiveResponse();
                        }
                    }
                });
            }

        return builder.create();
    }

    private void refreshAmountAndFee() {
        ExchangeRate exchangeRate = null;
        Address addressTo = null;
        Coin fee = null;
        if (walletService != null) {
            exchangeRate = walletService.getExchangeRate();
            try {
                addressTo = Address.fromBase58(walletService.getNetworkParameters(), address);
            } catch (Exception e) { /* ignore, not valid address */ }
            fee = walletService.estimateFee(addressTo, amount);
        }

        TextView amountText = (TextView) authView.findViewById(R.id.authview_amount_content);
        amountText.setText(UIUtils.coinFiatSpannable(getContext(), amount, exchangeRate, true, 0.75f));

        TextView feeText = (TextView) authView.findViewById(R.id.authview_fee_content);
        if (fee != null) {
            feeText.setText(UIUtils.coinFiatSpannable(getContext(), fee, exchangeRate, true, 0.75f));
        } else {
            feeText.setText(R.string.unknown);
        }
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            walletService = (WalletService.WalletServiceBinder) service;
            refreshAmountAndFee();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            walletService = null;
        }
    };

    public interface AuthenticationDialogListener {
        void authViewNegativeResponse();
        void authViewPositiveResponse();
        void authViewDestroy();
    }
}
