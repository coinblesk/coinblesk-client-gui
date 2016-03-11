package com.uzh.ckiller.coinblesk_client_gui.ui.dialogs;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import static android.view.View.VISIBLE;
import static android.view.View.INVISIBLE;
import ch.papers.payments.Constants;
import ch.papers.payments.WalletService;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.zxing.client.android.Intents;
import com.google.zxing.integration.android.IntentIntegrator;
import com.uzh.ckiller.coinblesk_client_gui.R;
import com.uzh.ckiller.coinblesk_client_gui.helpers.Encryption;
import com.uzh.ckiller.coinblesk_client_gui.helpers.UIUtils;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Coin;
import org.bitcoinj.uri.BitcoinURI;
import org.bitcoinj.uri.BitcoinURIParseException;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Andreas Albrecht
 */

public class BackupDialogFragment extends DialogFragment {

    private WalletService.WalletServiceBinder walletServiceBinder;

    private final static String TAG = BackupDialogFragment.class.getName();

    private EditText passwordEditText;
    private EditText passwordAgainEditText;
    private Button btnOk;
    private TextView passwordsMatchTextView;

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
        passwordsMatchTextView = (TextView) view.findViewById(R.id.fragment_backup_passwordmismatch_textview);

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
            dismiss();
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
            boolean passwordsMatch = passwordsMatch();
            btnOk.setEnabled(passwordsMatch);
            passwordsMatchTextView.setVisibility(passwordsMatch ? INVISIBLE : VISIBLE);
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    }

    private void clearPasswordInput() {
        passwordEditText.setText(null);
        passwordAgainEditText.setText(null);
    }

    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setTitle(R.string.fragment_backup_title);
        return dialog;
    }

    private boolean passwordsMatch() {
        final String pwd = passwordEditText.getText().toString();
        final String pwdAgain = passwordAgainEditText.getText().toString();
        boolean pwdMatch = !pwd.isEmpty() && !pwdAgain.isEmpty() && pwd.contentEquals(pwdAgain);
        return pwdMatch;
    }

    private void doBackup() {
        final File backupFile = getWalletBackupFile();
        final String password = passwordEditText.getText().toString();
        Preconditions.checkState(password.equals(passwordAgainEditText.getText().toString()) && password.length() > 0);
        clearPasswordInput();

        byte[] wallet = walletServiceBinder.getSerializedWallet();
        if (wallet != null && wallet.length > 0) {
            Writer out = null;
            try {

                out = new OutputStreamWriter(new FileOutputStream(backupFile), Charsets.UTF_8);
                out.write(Encryption.encrypt(wallet, password.toCharArray()));
                out.flush();
                Log.i(TAG, "Wallet backup finished. File = [" + backupFile + "]");

                // ask user whether he wants backup file as mail attachment
                showSendMailDialog(backupFile);

            } catch (Exception e) {
                Log.w(TAG, "Could not save backup.", e);

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle("Wallet Backup Failed")
                        .setMessage("The wallet could not be stored on the device: " + e.getMessage())
                        .setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                            }
                        })
                        .create()
                        .show();

            } finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        Log.d(TAG, "Could not close output writer for backup file.", e);
                    }
                }
            }
        }
    }

    private File getWalletBackupFile() {
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File walletFile = null;

        for (int i = 0; ; ++i) {
            String currentTime = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date());
            String postfix = i > 0 ? String.format("_%d", i) : "";
            String fileName = String.format("coinblesk_wallet_backup_%s%s", currentTime, postfix);

            walletFile = new File(path, fileName);
            if (!walletFile.exists()) {
                return walletFile;
            }
        }
    }

    private void showSendMailDialog(File backupFile) {
        DialogFragment newFragment = SendMailDialogFragment.newInstance(backupFile.getAbsolutePath());
        newFragment.show(getFragmentManager(), "backup_send_mail_dialog");
    }

    public static class SendMailDialogFragment extends DialogFragment {
        public static SendMailDialogFragment newInstance(String backupFile) {
            SendMailDialogFragment frag = new SendMailDialogFragment();
            Bundle args = new Bundle();
            args.putString("backup_file", backupFile);
            frag.setArguments(args);
            return frag;
        }

        private void sendMailWithBackup(String backupFile) {
            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            // The intent does not have a URI, so declare the "text/html". With text/plain, many messengers etc. appear.
            emailIntent.setType("text/html");
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Wallet Backup");
            emailIntent.putExtra(Intent.EXTRA_TEXT, "Wallet backup");
            emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://"+backupFile));
            startActivity(Intent.createChooser(emailIntent , "Send wallet backup..."));
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final String backupFile = getArguments().getString("backup_file");
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                    .setTitle("Wallet Backup")
                    .setMessage(Html.fromHtml("The wallet was stored on your device: <pre>"
                            +backupFile+
                            "</pre>.<br /><br />Do you want to send the backup via email?"))
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            Log.d(TAG, "User wants backup as mail attachment");
                            sendMailWithBackup(backupFile);
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User cancelled the dialog -- nothing to do
                            Log.d(TAG, "User does not want backup as mail attachment.");
                            dialog.cancel();
                        }
                    });
            return builder.create();

        }
    }


    /* -------------- WALLET SERVICE BINDER START ----------------*/

    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            walletServiceBinder = (WalletService.WalletServiceBinder) binder;
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            walletServiceBinder = null;
        }
    };

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
    }

    /* -------------- WALLET SERVICE BINDER END ----------------*/
}

