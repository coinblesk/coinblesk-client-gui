package com.uzh.ckiller.coinblesk_client_gui.ui.dialogs;

//import android.support.v7.app.AlertDialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.Toolbar;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.google.zxing.client.android.Intents;
import com.google.zxing.integration.android.IntentIntegrator;
import com.uzh.ckiller.coinblesk_client_gui.R;
import com.uzh.ckiller.coinblesk_client_gui.helpers.SpannableStringFormatter;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Coin;
import org.bitcoinj.utils.ExchangeRate;
import org.bitcoinj.utils.Fiat;
import org.w3c.dom.Text;

import ch.papers.payments.Constants;
import ch.papers.payments.WalletService;

/**
 * Created by ckiller
 */

public class SendDialogFragment extends DialogFragment {
    public static final int REQUEST_CODE = 0x0000c0de; // Only use bottom 16 bits
    private final static String TAG = SendDialogFragment.class.getName();
    private SpannableStringFormatter spannableStringFormatter;

    private EditText addressEditText;
    private EditText amountEditText;
    private TextInputLayout addressTextInputLayout;
    private TextInputLayout amountTextInputLayout;
    private Coin amount;

    public static DialogFragment newInstance(Coin amount) {
        DialogFragment fragment = new SendDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putLong("amount", amount.value);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.amount = Coin.valueOf(this.getArguments().getLong("amount"));
        this.spannableStringFormatter = new SpannableStringFormatter(getContext());
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        // QR Code
        // IntentIntegrator.forSupportFragment(SendDialogFragment.this).initiateScan();

        LayoutInflater inflater = LayoutInflater.from(getActivity());
        final View view = inflater.inflate(R.layout.fragment_send_alertdialog, null);

        this.addressTextInputLayout = (TextInputLayout) view.findViewById(R.id.address_text_input_layout);
        this.addressEditText = ((EditText) view.findViewById(R.id.address_edit_text));

        this.amountTextInputLayout = (TextInputLayout) view.findViewById(R.id.amount_text_input_layout);
        this.amountEditText = (EditText) view.findViewById(R.id.amount_edit_text);

        amountEditText.setText(amount.toString());

        return new AlertDialog.Builder(getActivity())
                .setTitle("Send Bitcoins")
                .setView(view)
                .setCancelable(true)
                .setPositiveButton("Send", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        sendCoins();
                        Snackbar.make(view, spannableStringFormatter.toFriendlySnackbarString(getResources()
                                .getString(R.string.snackbar_coins_sent)), Snackbar.LENGTH_SHORT)
                                .show();
                    }
                })
                .setNeutralButton("QR-Scan", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        IntentIntegrator.forSupportFragment(SendDialogFragment.this).initiateScan();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                }).create();
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                final String contents = data.getStringExtra(Intents.Scan.RESULT);
                this.addressEditText.setText(contents);
            }
        }
    }


    /* ------------------- PAYMENTS INTEGRATION STARTS HERE  ------------------- */
    private final BroadcastReceiver walletCoinsSentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            dismiss();
        }
    };

    private void sendCoins() {
        try {
            walletServiceBinder.sendCoins(new Address(Constants.PARAMS, addressEditText.getText().toString()), amount);
        } catch (AddressFormatException e) {
            e.printStackTrace();
        }
    }

    private WalletService.WalletServiceBinder walletServiceBinder;

    @Override
    public void onStart() {
        super.onStart();
        Intent intent = new Intent(this.getActivity(), WalletService.class);
        this.getActivity().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop() {
        super.onStop();
        this.getActivity().unbindService(serviceConnection);
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(walletCoinsSentReceiver);
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder binder) {
            walletServiceBinder = (WalletService.WalletServiceBinder) binder;
            walletServiceBinder.setExchangeRate(new ExchangeRate(Fiat.parseFiat("CHF", "430")));

            // TODO handle errors
            IntentFilter filter = new IntentFilter(Constants.WALLET_COINS_SENT);
            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(walletCoinsSentReceiver, filter);
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            walletServiceBinder = null;
        }
    };
    /* -------------------- PAYMENTS INTEGRATION ENDS HERE  -------------------- */
}
