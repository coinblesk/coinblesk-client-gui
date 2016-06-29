/*
 * Copyright 2016 The Coinblesk team and the CSG Group at University of Zurich
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.coinblesk.client.backup;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.FileProvider;
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

import com.coinblesk.client.AppConstants;
import com.coinblesk.client.R;
import com.coinblesk.client.utils.EncryptionUtils;
import com.coinblesk.client.utils.SharedPrefUtils;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;

import org.apache.commons.io.FileUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;


/**
 * Decrypt backup with openssl:
 * (1) openssl enc -d -aes-256-cbc -a -in coinblesk_wallet_backup_2016-03-12-16-03-31_encrypted > coinblesk_wallet_backup_2016-03-12-16-03-31_decrypted.zip
 * (2) enter password
 * (3) extract zip
 *
 * @author Andreas Albrecht
 */
public class BackupDialogFragment extends DialogFragment {

    private final static String TAG = BackupDialogFragment.class.getName();
    private final static String ARGS_BACKUP_FILE = "backup_file";

    private EditText txtPassword;
    private EditText txtPasswordAgain;
    private TextView passwordsMismatchHint;
    private Button btnOk;

    public static DialogFragment newInstance() {
        DialogFragment fragment = new BackupDialogFragment();
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_backup_dialog, container);
        txtPassword = (EditText) view.findViewById(R.id.backup_password_text);
        txtPasswordAgain = (EditText) view.findViewById(R.id.backup_password_again_text);
        passwordsMismatchHint = (TextView) view.findViewById(R.id.fragment_backup_passwordmismatch_textview);
        txtPassword.addTextChangedListener(new PasswordsMatchTextWatcher());
        txtPasswordAgain.addTextChangedListener(new PasswordsMatchTextWatcher());

        btnOk = (Button) view.findViewById(R.id.fragment_backup_ok);
        btnOk.setEnabled(false);

        view.findViewById(R.id.fragment_backup_ok).setOnClickListener(new BackupOkClickListener());
        view.findViewById(R.id.fragment_backup_cancel).setOnClickListener(new BackupCancelClickListener());

        return view;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setTitle(R.string.fragment_backup_title);
        return dialog;
    }

    private class BackupOkClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            Log.d(TAG, "Execute backup.");
            backup();
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
            passwordsMismatchHint.setVisibility(passwordsMatch ? INVISIBLE : VISIBLE);
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    }

    private void clearPasswordInput() {
        txtPassword.setText(null);
        txtPasswordAgain.setText(null);
    }

    private boolean passwordsMatch() {
        final String pwd = txtPassword.getText().toString();
        final String pwdAgain = txtPasswordAgain.getText().toString();
        boolean pwdMatch = !pwd.isEmpty() && !pwdAgain.isEmpty() && pwd.contentEquals(pwdAgain);
        return pwdMatch;
    }

    private void backup() {
        final File backupFile = newWalletBackupFile();
        final String password = txtPassword.getText().toString();
        Preconditions.checkState(password.equals(txtPasswordAgain.getText().toString()) && password.length() > 0);
        clearPasswordInput();

        Writer fileOut = null;
        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final ZipOutputStream zos = new ZipOutputStream(baos);
            Log.i(TAG, "Backup file: " + backupFile);

            // assemble all content to backup
            addFilesToZip(zos);

            // encrypt zip bytes and write to file
            zos.close();
            byte[] plainBackup = baos.toByteArray();
            String encryptedBackup = EncryptionUtils.encrypt(plainBackup, password.toCharArray());
            fileOut = new OutputStreamWriter(new FileOutputStream(backupFile), Charsets.UTF_8);
            fileOut.write(encryptedBackup);
            fileOut.flush();
            Log.i(TAG, "Wallet backup finished. File = [" + backupFile + "], size = " + backupFile.length() + "bytes, exists = " + backupFile.exists());

            // ask user whether he wants backup file as mail attachment
            showBackupCompletedDialog(backupFile);

        } catch (Exception e) {
            Log.w(TAG, "Could not write to file", e);
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.fragment_backup_failed_title)
                    .setMessage(getString(R.string.fragment_backup_failed_message) + ": " + e.getMessage())
                    .setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                        }
                    })
                    .create()
                    .show();
        } finally {
            if (fileOut != null) {
                try {
                    fileOut.close();
                } catch (IOException e) {
                    Log.i(TAG, "Could not close output stream");
                }
            }
        }
    }

    private void addFilesToZip(ZipOutputStream zos) throws IOException {
        File appRoot = new File(getActivity().getApplicationInfo().dataDir);
        Collection<File> filesToBackup = filesToBackup();
        for (File file : filesToBackup) {
            byte[] data = FileUtils.readFileToByteArray(file);
            String zipName = stripParentPath(file, appRoot);
            addZipEntry(zipName, data, zos);
            Log.i(TAG, "Added file to zip: app["+file.toString()+"] -> backup["+zipName+"]");
        }
    }

    private Collection<File> filesToBackup() {
        Collection<File> files = new ArrayList<>();
        files.add(SharedPrefUtils.getSharedPreferencesFile(getContext()));
        return files;
    }

    private String stripParentPath(File file, File parent) {
        // make a relative path
        String zipName = file.getAbsolutePath().replace(parent.getAbsolutePath(), "");
        if (zipName.startsWith(File.separator)) {
            zipName = zipName.substring(1);
        }
        return zipName;
    }

    private void addZipEntry(String filename, byte[] data, ZipOutputStream zos) throws IOException {
        ZipEntry entry = new ZipEntry(filename);
        zos.putNextEntry(entry);
        zos.write(data);
        zos.closeEntry();
        zos.flush();
    }

    private File newWalletBackupFile() {
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File walletFile = null;

        for (int i = 0; ; ++i) {
            String currentTime = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date());
            String postfix = i > 0 ? String.format("_%d", i) : "";
            String fileName = String.format("%s_%s%s", AppConstants.BACKUP_FILE_PREFIX, currentTime, postfix);

            walletFile = new File(path, fileName);
            if (!walletFile.exists()) {
                return walletFile;
            }
        }
    }

    private void showBackupCompletedDialog(File backupFile) {
        FragmentManager fm = getFragmentManager();
        DialogFragment newFragment = BackupCompletedDialogFragment.newInstance(backupFile.getAbsolutePath());
        newFragment.show(fm, "backup_completed_dialog");
    }

    public static class BackupCompletedDialogFragment extends DialogFragment {
        public static BackupCompletedDialogFragment newInstance(String backupFile) {
            BackupCompletedDialogFragment frag = new BackupCompletedDialogFragment();
            Bundle args = new Bundle();
            args.putString(ARGS_BACKUP_FILE, backupFile);
            frag.setArguments(args);
            return frag;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final String backupFile = getArguments().getString(ARGS_BACKUP_FILE);
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.fragment_backup_success_title)
                    .setMessage(Html.fromHtml(getString(R.string.fragment_backup_success_message, backupFile)))
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            Log.d(TAG, "User wants backup as mail attachment");
                            sendMailWithBackup(new File(backupFile));
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

        private void sendMailWithBackup(File backupFile) {
            Uri backupFileUri = FileProvider.getUriForFile(
                    getContext(), AppConstants.FILE_PROVIDER_AUTHORITY, backupFile);

            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            // The "mail intent" does not have a type, so declare the "text/html".
            // With text/plain, many messengers etc. appear.
            emailIntent.setType("text/html");
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.backup_mail_subject));
            emailIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.backup_mail_message));
            emailIntent.putExtra(Intent.EXTRA_STREAM, backupFileUri);
            startActivity(Intent.createChooser(emailIntent , getString(R.string.backup_mail_chooser)));
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
    }

}