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

package com.coinblesk.client.ui.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.coinblesk.client.R;


/**
 * @author Andreas Albrecht
 */
public class ProgressSuccessOrFailDialog extends DialogFragment {
    private final static String TAG = ProgressSuccessOrFailDialog.class.getName();
    private static final String ARG_STATE = "state";
    private static final String ARG_TITLE = "title";
    private static final String ARG_MESSAGE = "message";

    private enum State {
        PROGRESS,
        SUCCESS,
        FAILURE
    }

    private State currentState;
    private String title;
    private View viewProgress, viewSuccess, viewFailure, viewMessage;
    private TextView txtMessage;

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

        // do not create new instance on rotation
        // this way we can update the view from an async task / future (otherwise, we the reference is lost)
        setRetainInstance(true);
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putSerializable(ARG_STATE, currentState);
        state.putString(ARG_TITLE, title);
        if (txtMessage != null) {
            state.putString(ARG_MESSAGE, txtMessage.getText().toString());
        }
    }

    @Override
    public void onDestroyView() {
        // on rotation, do not dismiss the dialog (iff instance is retained)
        // may be a bug, see:
        // - https://code.google.com/p/android/issues/detail?id=17423
        // - https://stackoverflow.com/questions/13934951
        if (getDialog() != null && getRetainInstance()) {
            getDialog().setDismissMessage(null);
        }
        super.onDestroyView();
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = getActivity().getLayoutInflater().inflate(R.layout.fragment_progress_success_fail, null);
        viewProgress = view.findViewById(R.id.viewProgress);
        viewSuccess = view.findViewById(R.id.viewSuccess);
        viewFailure = view.findViewById(R.id.viewFailure);
        viewMessage = view.findViewById(R.id.viewMessage);
        txtMessage = (TextView) view.findViewById(R.id.txtMessage);

        if (savedInstanceState != null) {
            setState( (State) savedInstanceState.getSerializable(ARG_STATE) );
            title = savedInstanceState.getString(ARG_TITLE);
            setMessage(savedInstanceState.getString(ARG_MESSAGE, ""));
        } else if (getArguments() != null) {
            setState( (State) getArguments().getSerializable(ARG_STATE) );
            title = getArguments().getString(ARG_TITLE);
            setMessage(getArguments().getString(ARG_MESSAGE, ""));
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
                break;
            case FAILURE:
                viewProgress.setVisibility(View.GONE);
                viewSuccess.setVisibility(View.GONE);
                viewFailure.setVisibility(View.VISIBLE);
                break;
            case PROGRESS: /* fall through */
            default:
                viewProgress.setVisibility(View.VISIBLE);
                viewSuccess.setVisibility(View.GONE);
                viewFailure.setVisibility(View.GONE);
        }
        Log.d(TAG, "currentState: " + currentState);
    }

    public void setSuccess() {
        setState(State.SUCCESS);
        setMessage(null);
    }

    public void setFailure(String message) {
        setState(State.FAILURE);
        setMessage(message);
    }

    public void setProgress() {
        setState(State.PROGRESS);
        setMessage(null);
    }

    private void setMessage(String message) {
        if (message == null) {
            message = "";
        }
        if (txtMessage != null) {
            txtMessage.setText(message);
            viewMessage.setVisibility(message.isEmpty() ? View.GONE : View.VISIBLE);
        }
    }

}