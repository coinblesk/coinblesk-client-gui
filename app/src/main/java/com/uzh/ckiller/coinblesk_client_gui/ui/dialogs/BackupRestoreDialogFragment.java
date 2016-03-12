package com.uzh.ckiller.coinblesk_client_gui.ui.dialogs;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.*;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import ch.papers.payments.WalletService;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.io.Files;
import com.uzh.ckiller.coinblesk_client_gui.R;
import com.uzh.ckiller.coinblesk_client_gui.helpers.Encryption;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by Andreas Albrecht
 */

public class BackupRestoreDialogFragment extends DialogFragment {

    private WalletService.WalletServiceBinder walletServiceBinder;

    private final static String TAG = BackupRestoreDialogFragment.class.getName();

    private EditText passwordEditText;
    private Button btnOk;
    private Spinner fileList;

    private File selectedFile;

    public static DialogFragment newInstance() {
        DialogFragment fragment = new BackupRestoreDialogFragment();
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_backup_restore_dialog, container);
        this.passwordEditText = (EditText) view.findViewById(R.id.backup_password_text);
        this.btnOk = (Button) view.findViewById(R.id.fragment_backup_ok);
        this.btnOk.setEnabled(false);

        view.findViewById(R.id.fragment_backup_ok).setOnClickListener(new RestoreOkClickListener());
        view.findViewById(R.id.fragment_backup_cancel).setOnClickListener(new RestoreCancelClickListener());

        passwordEditText.addTextChangedListener(new PasswordNotEmptyTextWatcher());

        fileList = (Spinner) view.findViewById(R.id.fragment_restore_file_spinner);
        File backupDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File[] backupFiles = backupDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                // TODO: implement something nicer than searching by file prefix...
                return filename.startsWith("coinblesk_wallet_backup");
            }
        });

        ArrayAdapter<File> fileAdapter = new ArrayAdapter<File>(this.getContext(), android.R.layout.simple_list_item_1, backupFiles);
        fileList.setAdapter(fileAdapter);

        return view;
    }

    private class RestoreOkClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            Log.d(TAG, "Execute backup restore.");
            doRestore();

        }
    }

    private class RestoreCancelClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            Log.d(TAG, "Cancel backup restore.");
            getDialog().cancel();
        }
    }

    private class PasswordNotEmptyTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            boolean hasPassword = passwordEditText.length() > 0;
            btnOk.setEnabled(hasPassword);
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    }

    private void doRestore() {
        final String password = passwordEditText.getText().toString();
        final File selectedBackupFile = (File)fileList.getSelectedItem();
        clearPasswordInput();
        Preconditions.checkState(password != null && password.length() > 0);
        Preconditions.checkState(selectedBackupFile != null && selectedBackupFile.exists());

        try {
            final String encryptedBackup = Files.toString(selectedBackupFile, Charsets.UTF_8);
            final byte[] decryptedBackup = Encryption.decryptBytes(encryptedBackup, password.toCharArray());

            ZipInputStream zis = null;
            try {
                ByteArrayInputStream bais = new ByteArrayInputStream(decryptedBackup);
                zis = new ZipInputStream(new BufferedInputStream(bais));
                ZipEntry ze = null;
                while ((ze = zis.getNextEntry()) != null) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int count;
                    while ((count = zis.read(buffer)) != -1) {
                        baos.write(buffer, 0, count);
                    }
                    String filename = ze.getName();
                    byte[] fileBytes = baos.toByteArray();

                    OutputStream fos = null;
                    try {
                        File extractedFile = new File(getActivity().getFilesDir(), filename);
                        fos = new BufferedOutputStream(new FileOutputStream(extractedFile));
                        fos.write(fileBytes);
                        fos.flush();
                        Log.i(TAG, "Extracted file from zip: ["+filename+"] -> ["+extractedFile+"]");
                    } finally {
                        fos.close();
                    }
                }

                cleanupAfterRestore();
                dismiss();

            } finally {
                if (zis != null) {
                    try {
                        zis.close();
                    } catch (IOException e) {
                        Log.w(TAG, "Could not close zip input stream.", e);
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not restore backup: ", e);
            new AlertDialog.Builder(this.getContext())
                    .setTitle(R.string.fragment_backup_restore_failed_title)
                    .setMessage(getString(R.string.fragment_backup_restore_failed_message) + ": " + e.getMessage())
                    .setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    })
                    .create().show();
        }
    }

    private void cleanupAfterRestore() {
        walletServiceBinder.prepareWalletReset();
        walletServiceBinder.deleteWalletFile();
        // TODO: fix this. at the moment we must restart the app. otherwise old addresses are displayed (e.g. qr code).
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    private void clearPasswordInput() {
        passwordEditText.setText(null);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setTitle(R.string.fragment_backup_restore_title);
        return dialog;
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

