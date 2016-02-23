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
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.TextView;

import com.google.zxing.client.android.Intents;
import com.google.zxing.integration.android.IntentIntegrator;
import com.uzh.ckiller.coinblesk_client_gui.R;

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

public class ReceiveDialogFragment extends DialogFragment {
    public static final int REQUEST_CODE = 0x0000c0de; // Only use bottom 16 bits
    private final static String TAG = ReceiveDialogFragment.class.getName();

    private TextView amountEditText;
    private Coin amount;

    public static DialogFragment newInstance(Coin amount) {
        DialogFragment fragment = new ReceiveDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putLong("amount", amount.value);
        fragment.setArguments(arguments);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        LayoutInflater inflater = LayoutInflater.from(getActivity());
        final View view = inflater.inflate(R.layout.fragment_receive_alertdialog, null);

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
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.amount = Coin.valueOf(this.getArguments().getLong("amount"));
    }

}

