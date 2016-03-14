package com.coinblesk.client.ui.dialogs;

//import android.support.v7.app.AlertDialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.coinblesk.client.R;
import com.coinblesk.client.authview.AuthenticationView;
import com.coinblesk.client.helpers.UIUtils;
import com.coinblesk.payments.Constants;
import com.coinblesk.payments.Utils;
import com.coinblesk.payments.WalletService;
import com.coinblesk.payments.communications.peers.ServerPeerService;
import com.coinblesk.payments.communications.peers.nfc.NFCClient2;

import org.bitcoinj.uri.BitcoinURI;
import org.bitcoinj.uri.BitcoinURIParseException;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by ckiller
 */

public class ReceiveDialogFragment extends DialogFragment {
    private final static String TAG = ReceiveDialogFragment.class.getName();
    private final String CONNECTION_SETTINGS_PREF_KEY = "pref_connection_settings";
    public static final String BITCOIN_URI_KEY = "BITCOIN_URI_KEY";

    View contactlessTouchView;
    NFCClient2 nfcClient2;

    public static DialogFragment newInstance(BitcoinURI bitcoinURI) {
        DialogFragment fragment = new ReceiveDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putString(BITCOIN_URI_KEY, Utils.bitcoinUriToString(bitcoinURI));
        fragment.setArguments(arguments);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final String bitcoinUriString = this.getArguments().getString(BITCOIN_URI_KEY);

        try {
            final BitcoinURI bitcoinURI = new BitcoinURI(bitcoinUriString);

            final LayoutInflater inflater = LayoutInflater.from(getActivity());
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

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
            final Set<String> connectionSettings = sharedPreferences.getStringSet(CONNECTION_SETTINGS_PREF_KEY, new HashSet<String>());
            if (!connectionSettings.isEmpty()) {
                contactlessTouchView = view.findViewById(R.id.receive_contactless_touch_area);
                contactlessTouchView.setVisibility(View.VISIBLE);
                contactlessTouchView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        final View authViewDialogContent = inflater.inflate(R.layout.fragment_authview_dialog, null);
                        final TextView amountTextView = (TextView) authViewDialogContent.findViewById(R.id.authview_amount_content);
                        amountTextView.setText(UIUtils.scaleCoinForDialogs(bitcoinURI.getAmount(), getContext()));
                        final TextView addressTextView = (TextView) authViewDialogContent.findViewById(R.id.authview_address_content);
                        addressTextView.setText(bitcoinURI.getAddress().toString());

                        final LinearLayout authviewContainer = (LinearLayout) authViewDialogContent.findViewById(R.id.authview_container);
                        authviewContainer.addView(new AuthenticationView(getContext(), Utils.bitcoinUriToString(bitcoinURI).getBytes()));
                        final AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                                .setTitle(getResources().getString(R.string.authview_dialog_title))
                                .setView(authViewDialogContent)
                                .setCancelable(true)
                                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        serverServiceBinder.cancelPaymentRequest();
                                    }
                                }).show();

                        IntentFilter instantPaymentFilter = new IntentFilter(Constants.INSTANT_PAYMENT_SUCCESSFUL_ACTION);
                        instantPaymentFilter.addAction(Constants.INSTANT_PAYMENT_FAILED_ACTION);
                        instantPaymentFilter.addAction(Constants.WALLET_COINS_RECEIVED_ACTION);
                        LocalBroadcastManager.getInstance(getContext()).registerReceiver(new BroadcastReceiver() {
                            @Override
                            public void onReceive(Context context, Intent intent) {
                                LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(this);
                                try {
                                    alertDialog.dismiss();
                                } catch (Exception e){}

                            }
                        }, instantPaymentFilter);

                        serverServiceBinder.broadcastPaymentRequest(bitcoinURI);
                        nfcClient2 = new NFCClient2(getActivity(), walletServiceBinder);
                        nfcClient2.bitcoinURI(bitcoinURI);
                        nfcClient2.onSuccess(new Runnable(){
                            @Override
                            public void run() {
                                    try {
                                        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                                        Ringtone r = RingtoneManager.getRingtone(getContext(), notification);
                                        r.play();
                                        Log.d(TAG, "play sound");
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }

                            }
                        });
                        nfcClient2.onError(new Runnable(){
                            @Override
                            public void run() {
                                try {
                                    Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                                    Ringtone r = RingtoneManager.getRingtone(getContext(), notification);
                                    r.play();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                        nfcClient2.onChangePaymentRequest();

                    }

                });
            } else {
                view.findViewById(R.id.receive_contactless_touch_area).setVisibility(View.GONE);
            }



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

    /* ------------------- PAYMENTS INTEGRATION STARTS HERE  ------------------- */
    private ServerPeerService.ServerServiceBinder serverServiceBinder;
    final BroadcastReceiver transactionListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(this);
            ReceiveDialogFragment.this.dismiss();
        }
    };

    @Override
    public void onStart() {
        super.onStart();

        Intent serverServiceIntent = new Intent(this.getActivity(), ServerPeerService.class);
        this.getActivity().startService(serverServiceIntent);
        this.getActivity().bindService(serverServiceIntent, peerServiceConnection, Context.BIND_AUTO_CREATE);

        Intent intent = new Intent(this.getActivity(), WalletService.class);
        this.getActivity().bindService(intent, walletServiceConnection, Context.BIND_AUTO_CREATE);

        IntentFilter instantPaymentFilter = new IntentFilter(Constants.INSTANT_PAYMENT_SUCCESSFUL_ACTION);
        instantPaymentFilter.addAction(Constants.INSTANT_PAYMENT_FAILED_ACTION);
        instantPaymentFilter.addAction(Constants.WALLET_COINS_RECEIVED_ACTION);

        LocalBroadcastManager.getInstance(getContext()).registerReceiver(transactionListener, instantPaymentFilter);
    }

    @Override
    public void onStop() {
        Intent serverServiceIntent = new Intent(this.getActivity(), ServerPeerService.class);
        this.getActivity().stopService(serverServiceIntent);
        this.getActivity().unbindService(peerServiceConnection);
        this.getActivity().unbindService(walletServiceConnection);
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(transactionListener);
        super.onStop();
        if(nfcClient2 != null) {
            nfcClient2.stop();
        }
    }


    private WalletService.WalletServiceBinder walletServiceBinder;


    private final ServiceConnection walletServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder binder) {
            walletServiceBinder = (WalletService.WalletServiceBinder) binder;
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            walletServiceBinder = null;
        }
    };



    private final ServiceConnection peerServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder binder) {
            serverServiceBinder = (ServerPeerService.ServerServiceBinder) binder;

            //if(contactlessTouchView != null && !serverServiceBinder.hasSupportedServers()){
            //    contactlessTouchView.setVisibility(View.GONE);
            //}
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            serverServiceBinder = null;
        }
    };
    /* -------------------- PAYMENTS INTEGRATION ENDS HERE  -------------------- */

}

