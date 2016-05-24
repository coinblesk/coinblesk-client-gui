package com.coinblesk.client.ui.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.coinblesk.client.R;
import com.coinblesk.client.config.Constants;


/**
 * @author Andreas Albrecht
 */
public class ProgressSuccessOrFailDialog extends DialogFragment {
    private final static String TAG = ProgressSuccessOrFailDialog.class.getName();
    private static final String ARG_STATE = "state";
    private static final String ARG_TITLE = "title";

    private enum State {
        PROGRESS,
        SUCCESS,
        FAILURE
    }

    private State currentState;
    private String title;
    private View viewProgress, viewSuccess, viewFailure;

    public static DialogFragment newInstance(String dialogTitle) {
        DialogFragment fragment = new ProgressSuccessOrFailDialog();
        Bundle args = new Bundle();
        args.putSerializable(ARG_STATE, State.PROGRESS);
        args.putString(ARG_TITLE, dialogTitle);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onStart() {
        super.onStart();
        getDialog().getWindow().setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);

        IntentFilter paymentBroadcastFilter = new IntentFilter();
        paymentBroadcastFilter.addAction(Constants.INSTANT_PAYMENT_SUCCESSFUL_ACTION);
        paymentBroadcastFilter.addAction(Constants.INSTANT_PAYMENT_FAILED_ACTION);
        LocalBroadcastManager
                .getInstance(getContext())
                .registerReceiver(paymentBroadcastReceiver, paymentBroadcastFilter);
    }

    @Override
    public void onStop() {
        super.onStop();
        LocalBroadcastManager
                .getInstance(getContext())
                .unregisterReceiver(paymentBroadcastReceiver);
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putSerializable(ARG_STATE, currentState);
        state.putString(ARG_TITLE, title);
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = getActivity().getLayoutInflater().inflate(R.layout.fragment_progress_success_fail, null);
        viewProgress = view.findViewById(R.id.viewProgress);
        viewSuccess = view.findViewById(R.id.viewSuccess);
        viewFailure = view.findViewById(R.id.viewFailure);

        if (getArguments() != null) {
            setState( (State) getArguments().getSerializable(ARG_STATE) );
            title = getArguments().getString(ARG_TITLE);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.AlertDialogAccent);
        Dialog dialog = builder
                .setTitle(title)
                .setCancelable(true)
                .setView(view)
                .create();
        return dialog;
    }

    private void setState(State state) {
        currentState = state;
        switch (state) {
            case SUCCESS:
                viewProgress.setVisibility(View.GONE);
                viewSuccess.setVisibility(View.VISIBLE);
                viewFailure.setVisibility(View.GONE);
                dismiss();
                // TODO: instead of dismiss, we can show a success message/icon.
                break;
            case FAILURE:
                viewProgress.setVisibility(View.GONE);
                viewSuccess.setVisibility(View.GONE);
                viewFailure.setVisibility(View.VISIBLE);
                // TODO: show failure message/icon
                break;
            case PROGRESS: /* fall through */
            default:
                viewProgress.setVisibility(View.VISIBLE);
                viewSuccess.setVisibility(View.GONE);
                viewFailure.setVisibility(View.GONE);
        }
        Log.d(TAG, "currentState: " + currentState);
    }

    private final BroadcastReceiver paymentBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case Constants.INSTANT_PAYMENT_SUCCESSFUL_ACTION:
                    setState(State.SUCCESS);
                    break;
                case Constants.INSTANT_PAYMENT_FAILED_ACTION:
                    setState(State.FAILURE);
                    break;
                default:
                    Log.w(TAG, "Received broadcast but do not know what to do... Check IntentFilter");
            }
        }
    };

}