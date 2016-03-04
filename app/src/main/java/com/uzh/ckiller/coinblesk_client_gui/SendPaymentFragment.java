package com.uzh.ckiller.coinblesk_client_gui;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.view.MotionEventCompat;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.uzh.ckiller.coinblesk_client_gui.ui.dialogs.SendDialogFragment;

import ch.papers.payments.WalletService;
import ch.papers.payments.communications.peers.bluetooth.BluetoothLEClient;
import ch.papers.payments.communications.peers.nfc.NFCClient;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 27/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class SendPaymentFragment extends KeyboardFragment {
    private final static String TAG = SendPaymentFragment.class.getSimpleName();

    private final static float THRESHOLD = 700;
    private ProgressDialog dialog;

    public static Fragment newInstance() {
        return new SendPaymentFragment();
    }

    @Override

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        //new NFCClient(getActivity(),walletServiceBinder).start();
        view.setOnTouchListener(new View.OnTouchListener() {
            private float startPoint = 0;
            private boolean isShowingDialog = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (MotionEventCompat.getActionMasked(event)) {
                    case (MotionEvent.ACTION_DOWN):
                        startPoint = event.getY();
                        break;
                    case (MotionEvent.ACTION_MOVE):
                        if (!isShowingDialog && event.getY() - startPoint > THRESHOLD) {
                            showDialog();
                            new NFCClient(getActivity(),walletServiceBinder).start();
                            new BluetoothLEClient(getActivity(),walletServiceBinder).start();
                            isShowingDialog = true;
                        }
                        break;
                    default:
                        if (isShowingDialog) {
                            dismissDialog();
                            isShowingDialog = false;
                        }
                        break;
                }
                return false;
            }
        });

        dialog = new ProgressDialog(this.getContext());
        dialog.setMessage("Your message..");

        return view;
    }

    private void dismissDialog() {
        dialog.dismiss();
    }

    private void showDialog() {
        dialog.show();
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
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder binder) {
            walletServiceBinder = (WalletService.WalletServiceBinder) binder;
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            walletServiceBinder = null;
        }
    };

    @Override
    protected DialogFragment getDialogFragment() {
        return SendDialogFragment.newInstance(this.getCoin());
    }

    @Override
    public void onSharedPrefsUpdated(String customKey) {
        super.initCustomButton(customKey);
    }
}
