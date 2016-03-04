package com.uzh.ckiller.coinblesk_client_gui.ui.dialogs;

//import android.support.v7.app.AlertDialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.uzh.ckiller.coinblesk_client_gui.R;

import org.bitcoinj.core.Coin;
import org.bitcoinj.uri.BitcoinURI;
import org.bitcoinj.uri.BitcoinURIParseException;

import ch.papers.payments.communications.peers.ServerPeerService;

/**
 * Created by ckiller
 */

public class ReceiveDialogFragment extends DialogFragment {
    public static final int REQUEST_CODE = 0x0000c0de; // Only use bottom 16 bits
    private final static String TAG = ReceiveDialogFragment.class.getName();

    public static final String BITCOIN_URI_KEY = "BITCOIN_URI_KEY";

    private TextView amountEditText;
    private Coin amount;


    public static DialogFragment newInstance(BitcoinURI bitcoinURI) {
        DialogFragment fragment = new ReceiveDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putString(BITCOIN_URI_KEY, BitcoinURI.convertToBitcoinURI(bitcoinURI.getAddress(), bitcoinURI.getAmount(), bitcoinURI.getLabel(), bitcoinURI.getMessage()));
        fragment.setArguments(arguments);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final String bitcoinUriString = this.getArguments().getString(BITCOIN_URI_KEY);

        try {
            final BitcoinURI bitcoinURI = new BitcoinURI(bitcoinUriString);

            LayoutInflater inflater = LayoutInflater.from(getActivity());
            final View view = inflater.inflate(R.layout.fragment_receive_alertdialog, null);
            view.findViewById(R.id.receive_email_touch_area).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
                    emailIntent.setType("text/html");
                    emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, String.format(getString(R.string.payment_request_html_subject)));
                    emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, Html.fromHtml(String.format(getString(R.string.payment_request_html_content), bitcoinUriString, bitcoinURI.getAddress(), bitcoinURI.getAmount())));
                    startActivity(Intent.createChooser(emailIntent, "Email:"));
                }
            });

            view.findViewById(R.id.receive_qrcode_touch_area).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    QrDialogFragment.newInstance(bitcoinURI.getAddress(), bitcoinURI.getAmount()).show(getFragmentManager(), "qr-fragment");
                }
            });

            view.findViewById(R.id.receive_contactless_touch_area).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    serverServiceBinder.broadcastPaymentRequest(bitcoinURI);
                }
            });

            return new AlertDialog.Builder(getActivity())
                    .setTitle("Receive Bitcoins")
                    .setView(view)
                    .setCancelable(true)
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    }).create();
        } catch (BitcoinURIParseException e) {
            e.printStackTrace();
        }

        return null;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.amount = Coin.valueOf(this.getArguments().getLong("amount"));
    }

    /* ------------------- PAYMENTS INTEGRATION STARTS HERE  ------------------- */
    private ServerPeerService.ServerServiceBinder serverServiceBinder;

    @Override
    public void onStart() {
        super.onStart();

        Intent serverServiceIntent = new Intent(this.getActivity(), ServerPeerService.class);
        this.getActivity().bindService(serverServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

    }

    @Override
    public void onStop() {
        super.onStop();
        this.getActivity().unbindService(serviceConnection);
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder binder) {
            serverServiceBinder = (ServerPeerService.ServerServiceBinder) binder;
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            serverServiceBinder = null;
        }
    };
    /* -------------------- PAYMENTS INTEGRATION ENDS HERE  -------------------- */

}

