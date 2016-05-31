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
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.coinblesk.client.AppConstants;
import com.coinblesk.client.R;
import com.coinblesk.client.utils.EncryptionUtils;
import com.coinblesk.payments.WalletService;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.io.Files;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author Andreas Albrecht
 */
public class BackupRestoreDialogFragment extends DialogFragment {

    private final static String TAG = BackupRestoreDialogFragment.class.getName();

    private WalletService.WalletServiceBinder walletServiceBinder;

    private EditText txtPassword;
    private Button btnOk;
    private Spinner backupFilesList;

    public static DialogFragment newInstance() {
        DialogFragment fragment = new BackupRestoreDialogFragment();
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_backup_restore_dialog, container);
        txtPassword = (EditText) view.findViewById(R.id.backup_password_text);
        txtPassword.addTextChangedListener(new PasswordNotEmptyTextWatcher());

        btnOk = (Button) view.findViewById(R.id.fragment_backup_ok);
        btnOk.setEnabled(false);

        view.findViewById(R.id.fragment_backup_ok).setOnClickListener(new RestoreOkClickListener());
        view.findViewById(R.id.fragment_backup_cancel).setOnClickListener(new RestoreCancelClickListener());

        backupFilesList = (Spinner) view.findViewById(R.id.fragment_restore_file_spinner);
        initBackupFilesList();

        return view;
    }

    private void initBackupFilesList() {
        File backupDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File[] files = backupDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.startsWith(AppConstants.BACKUP_FILE_PREFIX);
            }
        });

        List<File> backupFiles = new ArrayList<>();
        if (files != null && files.length > 0) {
            for (File f : files) {
                backupFiles.add(f);
            }
        }
        Collections.sort(backupFiles);
        ArrayAdapter<File> fileAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, backupFiles);
        backupFilesList.setAdapter(fileAdapter);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setTitle(R.string.fragment_backup_restore_title);
        return dialog;
    }

    private class RestoreOkClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            Log.d(TAG, "Execute backup restore.");
            restore();
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
            boolean hasPassword = txtPassword.length() > 0;
            btnOk.setEnabled(hasPassword);
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    }

    private void restore() {
        final String password = txtPassword.getText().toString();
        final File selectedBackupFile = (File) backupFilesList.getSelectedItem();
        clearPasswordInput();
        Preconditions.checkState(password != null && password.length() > 0);
        Preconditions.checkState(selectedBackupFile != null && selectedBackupFile.exists());

        try {
            final String encryptedBackup = Files.toString(selectedBackupFile, Charsets.UTF_8);
            final byte[] decryptedBackup = EncryptionUtils.decryptBytes(encryptedBackup, password.toCharArray());
            Log.d(TAG, String.format("Decrypted backup file: [%s] (%d bytes)", selectedBackupFile, decryptedBackup.length));

            extractZip(decryptedBackup);
            cleanupAfterRestore();

        } catch (Exception e) {
            Log.w(TAG, "Could not restore backup: ", e);
            new AlertDialog.Builder(this.getContext())
                    .setTitle(R.string.fragment_backup_restore_failed_title)
                    .setMessage(getString(R.string.fragment_backup_restore_failed_message) + ": " + e.getMessage())
                    .setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .create().show();
        }
    }

    /**
     * Extracts all files from a zip to disk.
     * @param zip data (zip format)
     * @throws IOException
     */
    private void extractZip(byte[] zip) throws IOException {
        ZipInputStream zis = null;
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(zip);
            zis = new ZipInputStream(new BufferedInputStream(bais));
            ZipEntry ze = null;

            // extract all entries
            Log.d(TAG, "Start extracting backup.");
            int i = 0;
            while ((ze = zis.getNextEntry()) != null) {
                extractFromZipAndSaveToFile(zis, ze);
                ++i;
            }
            Log.i(TAG, "Extracted "+i+" elements from backup.");

        } finally {
            if (zis != null) {
                try {
                    zis.close();
                } catch (IOException e) {
                    Log.w(TAG, "Could not close zip input stream.", e);
                }
            }
        }
    }

    /**
     * Extracts the next file from the Zip and stores it on disk.
     * @param zis Zip
     * @param ze the next entry
     * @throws IOException
     */
    private void extractFromZipAndSaveToFile(ZipInputStream zis, ZipEntry ze) throws IOException {
        byte[] data = extractFileFromZip(zis);
        String filename = ze.getName();
        File toFile = new File(getActivity().getFilesDir(), filename);
        saveToFile(toFile, data);
        Log.i(TAG, "Extracted file from backup: ["+filename+"] -> ["+toFile+"]");
    }

    /**
     * Extracts the next file from the ZIP and returns the byte content.
     * @param zis Zip
     * @return content
     * @throws IOException
     */
    private byte[] extractFileFromZip(ZipInputStream zis) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int count;
        while ((count = zis.read(buffer)) != -1) {
            baos.write(buffer, 0, count);
        }
        byte[] data = baos.toByteArray();
        return data;
    }

    /**
     * Writes the data to the file. Directories that do not exist yet are created on demand.
     * @param file target path
     * @param data content
     * @throws IOException
     */
    private void saveToFile(File file, byte[] data) throws IOException {
        OutputStream fos = null;
        try {
            File parentDir = file.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
                Log.d(TAG, "Create parent dir: [" + parentDir + "]");
            }
            fos = new BufferedOutputStream(new FileOutputStream(file));
            fos.write(data);
            fos.flush();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch(IOException e) {
                    Log.w(TAG, "Could not close file stream.", e);
                }
            }
        }
    }

    private void cleanupAfterRestore() {
        walletServiceBinder.prepareWalletReset();
        Intent intent = new Intent(getActivity(), WalletService.class);
        getActivity().stopService(intent);
        dismiss();
        getActivity().finishAffinity();
    }

    private void clearPasswordInput() {
        txtPassword.setText(null);
    }

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
        Intent intent = new Intent(getActivity(), WalletService.class);
        this.getActivity().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop() {
        super.onStop();
        this.getActivity().unbindService(serviceConnection);
    }
}
