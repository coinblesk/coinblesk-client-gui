package com.uzh.ckiller.coinblesk_client_gui;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.view.MotionEventCompat;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.uzh.ckiller.coinblesk_client_gui.authview.AuthenticationView;
import com.uzh.ckiller.coinblesk_client_gui.helpers.UIUtils;
import com.uzh.ckiller.coinblesk_client_gui.ui.dialogs.SendDialogFragment;

import org.bitcoinj.uri.BitcoinURI;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import ch.papers.payments.Utils;
import ch.papers.payments.WalletService;
import ch.papers.payments.communications.peers.AbstractClient;
import ch.papers.payments.communications.peers.PaymentRequestAuthorizer;
import ch.papers.payments.communications.peers.bluetooth.BluetoothLEClient;
import ch.papers.payments.communications.peers.bluetooth.BluetoothRFCommClient;
import ch.papers.payments.communications.peers.nfc.NFCClient;
import ch.papers.payments.communications.peers.wifi.WiFiClient;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 27/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class SendPaymentFragment extends KeyboardFragment {
    private final static String TAG = SendPaymentFragment.class.getSimpleName();

    private final static float THRESHOLD = 500;
    private ProgressDialog dialog;
    private final List<AbstractClient> clients = new ArrayList<AbstractClient>();

    public static Fragment newInstance() {
        return new SendPaymentFragment();
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        view.setOnTouchListener(new View.OnTouchListener() {
            private float startPoint = 0;
            private boolean isShowingDialog = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                float heightValue = event.getY();
                switch (MotionEventCompat.getActionMasked(event)) {
                    case (MotionEvent.ACTION_DOWN):
                        startPoint = heightValue;
                        return true;
                    case (MotionEvent.ACTION_MOVE):
                        if (!isShowingDialog && heightValue - startPoint > THRESHOLD) {
                            showDialog();
                            readyForInstantPayment();
                            isShowingDialog = true;
                        }
                        break;
                    default:
                        if (isShowingDialog) {
                            for (AbstractClient client : clients) {
                                if (client.isRunning()) {
                                    client.setReadyForInstantPayment(false);
                                    client.setPaymentRequestAuthorizer(PaymentRequestAuthorizer.DISALLOW_AUTHORIZER);
                                }
                            }
                            dismissDialog();
                            isShowingDialog = false;
                        }
                        break;
                }
                return false;
            }
        });

        dialog = new ProgressDialog(this.getContext());
        dialog.setMessage("Your message..");

        return view;
    }

    private void readyForInstantPayment() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (AbstractClient client : clients) {
                    if (client.isSupported()) {
                        client.setPaymentRequestAuthorizer(new PaymentRequestAuthorizer() {
                            private boolean response = false;

                            @Override
                            public boolean isPaymentRequestAuthorized(BitcoinURI paymentRequest) {
                                final CountDownLatch countDownLatch = new CountDownLatch(1); //because we need a syncronous answer
                                final View authViewDialog = LayoutInflater.from(getContext()).inflate(R.layout.fragment_authview_dialog, null);
                                final TextView amountTextView = (TextView) authViewDialog.findViewById(R.id.authview_amount_content);

                                amountTextView.setText(UIUtils.scaleCoinForDialogs(paymentRequest.getAmount(), getContext()));
                                final TextView addressTextView = (TextView) authViewDialog.findViewById(R.id.authview_address_content);
                                addressTextView.setText(paymentRequest.getAddress().toString());

                                final LinearLayout authviewContainer = (LinearLayout) authViewDialog.findViewById(R.id.authview_container);
                                authviewContainer.addView(new AuthenticationView(getContext(), Utils.bitcoinUriToString(paymentRequest).getBytes()));

                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        new AlertDialog.Builder(getActivity())
                                                .setTitle("Authview")
                                                .setView(authViewDialog)
                                                .setCancelable(true)
                                                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        response = false;
                                                        countDownLatch.countDown();
                                                    }
                                                }).setPositiveButton(R.string.accept, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                response = true;
                                                countDownLatch.countDown();
                                            }
                                        }).show();
                                    }
                                });

                                try {
                                    countDownLatch.await();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                return response;
                            }

                            @Override
                            public void onPaymentSuccess() {
                                Snackbar.make(getView(), R.string.payment_succesful_message, Snackbar.LENGTH_LONG);
                                stopClients();
                            }

                            @Override
                            public void onPaymentError(String errorMessage) {
                                Snackbar.make(getView(), errorMessage, Snackbar.LENGTH_LONG);
                            }
                        });
                        client.setReadyForInstantPayment(true);
                        client.start();
                    }
                }
            }
        }).start();
    }

    private void dismissDialog() {
        dialog.dismiss();
    }

    private void showDialog() {
        dialog.show();
    }

    @Override
    protected DialogFragment getDialogFragment() {
        return SendDialogFragment.newInstance(this.getCoin());
    }

    @Override
    public void onSharedPrefsUpdated(String customKey) {
        super.initCustomButton(customKey);
    }

    /* ------------------- PAYMENTS INTEGRATION STARTS HERE  ------------------- */
    private WalletService.WalletServiceBinder walletServiceBinder;

    @Override
    public void onStart() {
        super.onStart();


        Intent intent = new Intent(this.getActivity(), WalletService.class);
        this.getActivity().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop() {
        this.stopClients();
        this.getActivity().unbindService(serviceConnection);
        super.onStop();
    }

    private void stopClients() {
        for (AbstractClient peer : this.clients) {
            peer.stop();
        }
        clients.clear();
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder binder) {
            walletServiceBinder = (WalletService.WalletServiceBinder) binder;
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
            final Set<String> connectionSettings = sharedPreferences.getStringSet(CONNECTION_SETTINGS_PREF_KEY, new HashSet<String>());

            if (connectionSettings.contains(NFC_ACTIVATED)) {
                clients.add(new NFCClient(getActivity(), walletServiceBinder));
            }

            if (connectionSettings.contains(BT_ACTIVATED)) {
                clients.add(new BluetoothRFCommClient(getContext(), walletServiceBinder));
                clients.add(new BluetoothLEClient(getContext(), walletServiceBinder));
            }

            if (connectionSettings.contains(WIFIDIRECT_ACTIVATED)) {
                clients.add(new WiFiClient(getContext(), walletServiceBinder));
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            walletServiceBinder = null;
        }
    };
    /* -------------------- PAYMENTS INTEGRATION ENDS HERE  -------------------- */

}
