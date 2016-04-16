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

import android.widget.Toast;
import com.coinblesk.client.helpers.UIUtils;
import com.coinblesk.client.ui.dialogs.SendDialogFragment;
import com.coinblesk.payments.Constants;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Transaction;

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
        Coin currentBalance = getWalletServiceBinder().getBalance();
        boolean notEnoughMoney = currentBalance.isLessThan(getCoin());
        if (notEnoughMoney) {
            Snackbar.make(
                    getActivity().findViewById(android.R.id.content),
                    UIUtils.toFriendlySnackbarString(getContext(), getString(R.string.insufficient_funds)),
                    Snackbar.LENGTH_LONG)
                    .show();

            // calculate max amount to spend with a fee estimate.
            Coin fee = estimateFeeToEmptyWallet();
            Coin maxAmount = currentBalance.subtract(fee);
            setAmountByCoin(maxAmount);
        } else{
            return SendDialogFragment.newInstance(this.getCoin());
        }
        return null;
    }

    private Coin estimateFeeToEmptyWallet() {
        // TODO: move this calculation to bitcoin utils respectively use the calculation of the newest version of lib.
        int txInputs = getWalletServiceBinder().getUnspentInstantOutputs().size();
        final int len = 10 + (260 * txInputs) + 66;
        final int fee = (int) (len * 10.562);
        return Coin.valueOf(fee);
    }

    @Override
    public void onSharedPrefsUpdated(String customKey) {
        super.initCustomButton(customKey);
    }

}
