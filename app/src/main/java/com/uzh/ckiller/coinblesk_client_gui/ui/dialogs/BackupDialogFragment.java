package com.uzh.ckiller.coinblesk_client_gui.ui.dialogs;


import android.app.Activity;
import android.app.Dialog;
import android.content.*;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import ch.papers.payments.Constants;
import ch.papers.payments.WalletService;
import com.google.zxing.client.android.Intents;
import com.google.zxing.integration.android.IntentIntegrator;
import com.uzh.ckiller.coinblesk_client_gui.R;
import com.uzh.ckiller.coinblesk_client_gui.helpers.UIUtils;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Coin;
import org.bitcoinj.uri.BitcoinURI;
import org.bitcoinj.uri.BitcoinURIParseException;

/**
 * Created by Andreas Albrecht
 */

public class BackupDialogFragment extends DialogFragment {

    public static final String AMOUNT_KEY = "AMOUNT_KEY";
    public static final String ADDRESS_KEY = "ADDRESS_KEY";

    private final static String TAG = BackupDialogFragment.class.getName();

    private EditText passwordEditText;
    private EditText passwordAgainEditText;
    private Button btnOk;

    public static DialogFragment newInstance() {
        DialogFragment fragment = new BackupDialogFragment();
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_backup_dialog, container);
        this.passwordEditText = (EditText) view.findViewById(R.id.backup_password_text);
        this.passwordAgainEditText = (EditText) view.findViewById(R.id.backup_password_again_text);
        this.btnOk = (Button) view.findViewById(R.id.fragment_backup_ok);
        btnOk.setEnabled(false);

        view.findViewById(R.id.fragment_backup_ok).setOnClickListener(new BackupOkClickListener());
        view.findViewById(R.id.fragment_backup_cancel).setOnClickListener(new BackupCancelClickListener());

        passwordEditText.addTextChangedListener(new PasswordsMatchTextWatcher());
        passwordAgainEditText.addTextChangedListener(new PasswordsMatchTextWatcher());

        return view;
    }

    private class BackupOkClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            Log.d(TAG, "Execute backup.");
            doBackup();
        }
    }

    private class BackupCancelClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            Log.d(TAG, "Cancel backup.");
            getDialog().cancel();
        }
    }

    private class PasswordsMatchTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // compare passwords and enable/disable button
            String pwd = passwordEditText.getText().toString();
            String pwdAgain = passwordAgainEditText.getText().toString();
            if (pwd.length() > 0 && pwdAgain.length() > 0 && pwd.contentEquals(pwdAgain)) {
                btnOk.setEnabled(true);
            } else {
                btnOk.setEnabled(false);
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    }



    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setTitle(R.string.fragment_backup_title);
        return dialog;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
            if (resultCode == Activity.RESULT_OK) {
                Log.d(TAG, "RESULT_OK");
            }

    }

    private void doBackup() {
    }


//    /* ------------------- PAYMENTS INTEGRATION STARTS HERE  ------------------- */
//    private final BroadcastReceiver walletCoinsSentReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            dismiss();
//        }
//    };
//
//    private void sendCoins() {
//        try {
////            Coin amount = Coin.valueOf(Long.parseLong(this.amountEditText.getText().toString()));
//            Coin amount = Coin.valueOf(this.getArguments().getLong(AMOUNT_KEY, 0));
//            walletServiceBinder.sendCoins(new Address(Constants.PARAMS, addressEditText.getText().toString()), amount);
//        } catch (AddressFormatException e) {
//            e.printStackTrace();
//        }
//    }
//
//    private WalletService.WalletServiceBinder walletServiceBinder;
//
//    @Override
//    public void onStart() {
//        super.onStart();
//        Intent intent = new Intent(this.getActivity(), WalletService.class);
//        this.getActivity().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
//    }
//
//    @Override
//    public void onStop() {
//        super.onStop();
//        this.getActivity().unbindService(serviceConnection);
//        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(walletCoinsSentReceiver);
//    }
//
//    private final ServiceConnection serviceConnection = new ServiceConnection() {
//
//        @Override
//        public void onServiceConnected(ComponentName className,
//                                       IBinder binder) {
//            walletServiceBinder = (WalletService.WalletServiceBinder) binder;
//
//            IntentFilter filter = new IntentFilter(Constants.WALLET_COINS_SENT_ACTION);
//            filter.addAction(Constants.INSTANT_PAYMENT_SUCCESSFUL_ACTION);
//            filter.addAction(Constants.INSTANT_PAYMENT_FAILED_ACTION);
//            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(walletCoinsSentReceiver, filter);
//        }
//
//        @Override
//        public void onServiceDisconnected(ComponentName className) {
//            walletServiceBinder = null;
//        }
//    };
//    /* -------------------- PAYMENTS INTEGRATION ENDS HERE  -------------------- */

}

