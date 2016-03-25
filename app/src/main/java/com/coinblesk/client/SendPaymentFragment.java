package com.coinblesk.client;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.MotionEventCompat;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.coinblesk.client.ui.dialogs.SendDialogFragment;
import com.coinblesk.payments.Constants;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 27/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class SendPaymentFragment extends KeyboardFragment {
    private final static String TAG = SendPaymentFragment.class.getSimpleName();

    private final static float THRESHOLD = 500;

    public static Fragment newInstance() {
        return new SendPaymentFragment();
    }


    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = super.onCreateView(inflater, container, savedInstanceState);
        assert view != null;

        final ProgressDialog dialog = new ProgressDialog(this.getContext());
        dialog.setMessage(this.getString(R.string.fragment_send_dialog_scanning));
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                LocalBroadcastManager.getInstance(getContext()).sendBroadcast(new Intent(Constants.STOP_CLIENTS_ACTION));
            }
        });

        view.setOnTouchListener(new View.OnTouchListener() {
            private float startPoint = 0;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                float heightValue = event.getY();
                switch (MotionEventCompat.getActionMasked(event)) {
                    case (MotionEvent.ACTION_DOWN):
                        startPoint = heightValue;
                        return true;
                    case (MotionEvent.ACTION_MOVE):

                        if (heightValue - startPoint > THRESHOLD) {
                            if (!dialog.isShowing()) {
                                dialog.show();
                                IntentFilter instantPaymentFinishedIntentFilter = new IntentFilter(Constants.INSTANT_PAYMENT_FAILED_ACTION);
                                instantPaymentFinishedIntentFilter.addAction(Constants.INSTANT_PAYMENT_SUCCESSFUL_ACTION);

                                LocalBroadcastManager.getInstance(getContext()).registerReceiver(new BroadcastReceiver() {
                                    @Override
                                    public void onReceive(Context context, Intent intent) {
                                        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(this);
                                        dialog.dismiss();
                                    }
                                }, instantPaymentFinishedIntentFilter);
                                LocalBroadcastManager.getInstance(getContext()).sendBroadcast(new Intent(Constants.START_CLIENTS_ACTION));
                            }
                        }
                        break;
                }
                return false;
            }
        });


        return view;
    }

    @Override
    protected DialogFragment getDialogFragment() {
        return SendDialogFragment.newInstance(this.getCoin());
    }

    @Override
    public void onSharedPrefsUpdated(String customKey) {
        super.initCustomButton(customKey);
    }

}
