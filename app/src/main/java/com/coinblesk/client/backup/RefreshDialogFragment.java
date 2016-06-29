package com.coinblesk.client.backup;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import com.coinblesk.client.R;
import com.coinblesk.payments.WalletService;

/**
 * @author Andreas Albrecht
 */
public class RefreshDialogFragment extends DialogFragment {
    private final static String TAG = RefreshDialogFragment.class.getName();

    private WalletService.WalletServiceBinder walletServiceBinder;

    public static DialogFragment newInstance() {
        DialogFragment fragment = new RefreshDialogFragment();
        return fragment;
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
        Intent intent = new Intent(getActivity(), WalletService.class);
        getActivity().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        getActivity().unbindService(serviceConnection);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.AlertDialogAccent)
                .setTitle(R.string.backup_refresh_dialog_title)
                .setMessage(R.string.backup_refresh_dialog_message)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        refresh();
                        dialog.dismiss();
                        // close app such that wallet service is "restarted".
                        getActivity().finishAffinity();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        return builder.create();
    }

    private void refresh() {
        walletServiceBinder.resetWallet();
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

}