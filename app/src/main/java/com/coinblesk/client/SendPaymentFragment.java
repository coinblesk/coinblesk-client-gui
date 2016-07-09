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

package com.coinblesk.client;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.MotionEventCompat;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.coinblesk.client.config.Constants;
import com.coinblesk.client.ui.dialogs.SendDialogFragment;
import com.coinblesk.client.utils.UIUtils;

import org.bitcoinj.core.Coin;

/**
 * @author Alessandro De Carli
 * @author Andreas Albrecht
 */
public class SendPaymentFragment extends KeyboardFragment {
    private final static String TAG = SendPaymentFragment.class.getSimpleName();

    private final static float THRESHOLD = 10;

    public static Fragment newInstance() {
        return new SendPaymentFragment();
    }


    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = super.onCreateView(inflater, container, savedInstanceState);
        assert view != null;

        final ProgressDialog dialog = new ProgressDialog(this.getContext());
        dialog.setMessage(getString(R.string.fragment_send_dialog_scanning));
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
            }
        });
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                LocalBroadcastManager
                        .getInstance(getContext())
                        .sendBroadcast(new Intent(Constants.STOP_CLIENTS_ACTION));
            }
        });

        /*view.setOnTouchListener(new View.OnTouchListener() {
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

                                LocalBroadcastManager
                                        .getInstance(getContext())
                                        .registerReceiver(new BroadcastReceiver() {
                                            @Override
                                            public void onReceive(Context context, Intent intent) {
                                                LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(this);
                                                dialog.dismiss();
                                            }
                                         }, instantPaymentFinishedIntentFilter);
                                LocalBroadcastManager
                                        .getInstance(getContext())
                                        .sendBroadcast(new Intent(Constants.START_CLIENTS_ACTION));
                            }
                        }
                        break;
                }
                return false;
            }
        });*/


        return view;
    }

    @Override
    protected DialogFragment getDialogFragment() {
        // calculate max amount to spend minus some fee estimate.
        Coin maxSpendableAmount = getWalletServiceBinder().getMaxSpendableAmount();
        boolean notEnoughMoney = maxSpendableAmount.isLessThan(getCoin());
        if (notEnoughMoney) {
            Snackbar.make(
                    getActivity().findViewById(android.R.id.content),
                    UIUtils.toFriendlySnackbarString(getContext(), getString(R.string.insufficient_funds)),
                    Snackbar.LENGTH_LONG)
                    .show();
            setAmountByCoin(maxSpendableAmount);
        } else {
            return SendDialogFragment.newInstance(getCoin());
        }
        return null;
    }

    @Override
    public void onSharedPrefsUpdated(String customKey) {
        super.initCustomButton(customKey);
    }

}
