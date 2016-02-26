package com.uzh.ckiller.coinblesk_client_gui.ui.dialogs;

//import android.support.v7.app.AlertDialog;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.WriterException;
import com.uzh.ckiller.coinblesk_client_gui.R;
import com.uzh.ckiller.coinblesk_client_gui.helpers.QREncoder;

import ch.papers.payments.WalletService;

/**
 * Created by ckiller
 */

public class QrDialogFragment extends DialogFragment {

    private final static String TAG = QrDialogFragment.class.getName();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        this.getDialog().setTitle(R.string.qr_code_dialog_title);
        View view = inflater.inflate(R.layout.fragment_qr_dialog, container);
        return view;
    }

    /* ------------------- PAYMENTS INTEGRATION STARTS HERE  ------------------- */
    private WalletService.WalletServiceBinder walletServiceBinder;

    @Override
    public void onStart() {
        super.onStart();
        Intent intent = new Intent(this.getActivity(), WalletService.class);
        this.getActivity().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder binder) {
            walletServiceBinder = (WalletService.WalletServiceBinder) binder;
            final TextView addressTextView = (TextView) QrDialogFragment.this.getView().findViewById(R.id.address_textview);
            addressTextView.setText(walletServiceBinder.getCurrentReceiveAddress().toString());

            final Button clipboardButton = (Button) QrDialogFragment.this.getView().findViewById(R.id.qr_dialog_copytoclipboard);
            clipboardButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(getContext().CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Your address", walletServiceBinder.getCurrentReceiveAddress().toString());
                    clipboard.setPrimaryClip(clip);
                }
            });

            final ImageView qrCodeImageView = (ImageView) QrDialogFragment.this.getView().findViewById(R.id.qr_code);
            try {
                Bitmap qrBitmap = QREncoder.encodeAsBitmap(walletServiceBinder.getCurrentReceiveAddress().toString());
                qrCodeImageView.setImageBitmap(qrBitmap);
            } catch (WriterException e) {
                e.printStackTrace();
            }
            Log.d(TAG, "address: " + walletServiceBinder.getCurrentReceiveAddress().toString());
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            walletServiceBinder = null;
        }
    };
    /* -------------------- PAYMENTS INTEGRATION ENDS HERE  -------------------- */
}
