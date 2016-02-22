/*
package com.uzh.ckiller.coinblesk_client_gui.ui.dialogs;

*/
/**
 * Created by ckiller on 22/02/16.
 *//*


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.google.zxing.client.android.Intents;
import com.google.zxing.integration.android.IntentIntegrator;
import com.uzh.ckiller.coinblesk_client_gui.R;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Coin;
import org.bitcoinj.utils.ExchangeRate;
import org.bitcoinj.utils.Fiat;

import ch.papers.payments.Constants;
import ch.papers.payments.WalletService;

public class ReceiveDialogFragment extends DialogFragment{

    public static final int REQUEST_CODE = 0x0000c0de; // Only use bottom 16 bits
    private final static String TAG = ReceiveDialogFragment.class.getName();

    private Coin amount;

    public static DialogFragment newInstance(Coin amount){
        DialogFragment fragment = new ReceiveDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putLong("amount",amount.value);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.ThemeOverlay_Material_Dark);
        this.amount = Coin.valueOf(this.getArguments().getLong("amount"));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_send_dialog, container);
        view.findViewById(R.id.send_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendCoins();
            }
        });

        this.addressEditText =((EditText)view.findViewById(R.id.address_edit_text));

        Toolbar dialogToolbar = (Toolbar) view.findViewById(R.id.fake_action_bar);
        if (dialogToolbar!=null) {
            final SendDialogFragment window = this;

            */
/* ------------------- OPTIONAL SEND ICON TOP RIGHT BEGIN ------------------- *//*

            MenuItem sendItem = dialogToolbar.getMenu().add(0, R.id.confirm_send_dialog, 0, "SEND");
            sendItem.setIcon(R.drawable.ic_send_arrow_48px);
            MenuItemCompat.setShowAsAction(sendItem, MenuItem.SHOW_AS_ACTION_IF_ROOM);

            dialogToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    sendCoins();
                    return true;
                }
            });
            */
/* ------------------- OPTIONAL SEND ICON TOP RIGHT END ------------------- *//*



            dialogToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    window.dismiss();
                }
            });
        }

        Toolbar cardToolbar = (Toolbar) view.findViewById(R.id.card_action_bar);
        if (cardToolbar!=null) {
            MenuItem qrScanItem = cardToolbar.getMenu().add(0, R.id.action_qr_code, 0, R.string.action_qr_code);
            qrScanItem.setIcon(R.drawable.ic_qrcode_scan_white_18dp);

            MenuItemCompat.setShowAsAction(qrScanItem, MenuItem.SHOW_AS_ACTION_IF_ROOM);

            cardToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    IntentIntegrator.forSupportFragment(SendDialogFragment.this).initiateScan();
                    return true;
                }
            });
        }

        */
/* ------------------- TEXTINPUTLAYOUT REFERENCES FOR VALIDATION AND ERROR HANDLING BEGIN ------------------- *//*

        this.addressTextInputLayout = (TextInputLayout) view.findViewById(R.id.address_text_input_layout);
        this.amountTextInputLayout = (TextInputLayout) view.findViewById(R.id.amount_text_input_layout);
        this.amountEditText = (EditText) view.findViewById(R.id.amount_edit_text);

        amountEditText.setText(amount.toString());
        */
/* ------------------- TEXTINPUTLAYOUT REFERENCES FOR VALIDATION AND ERROR HANDLING END ------------------- *//*



        return view;
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


    */
/* ------------------- PAYMENTS INTEGRATION STARTS HERE  ------------------- *//*

    private final BroadcastReceiver walletCoinsSentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            dismiss();
        }
    };

    private void sendCoins(){
        try {
            walletServiceBinder.sendCoins(new Address(Constants.PARAMS, addressEditText.getText().toString()),amount);
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
    */
/* -------------------- PAYMENTS INTEGRATION ENDS HERE  -------------------- *//*

}


}
*/
