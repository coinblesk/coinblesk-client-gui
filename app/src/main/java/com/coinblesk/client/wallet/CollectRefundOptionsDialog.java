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

package com.coinblesk.client.wallet;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.coinblesk.client.R;
import com.coinblesk.client.utils.UIUtils;

import org.bitcoinj.core.Coin;

/**
 * @author Andreas Albrecht
 */
public class CollectRefundOptionsDialog extends DialogFragment {
    private static final String TAG = CollectRefundOptionsDialog.class.getName();
    private static final String ARGS_AMOUNT = "amount";

    private CollectRefundOptionsListener listener;

    public static DialogFragment newInstance(Coin amount) {
        DialogFragment fragment = new CollectRefundOptionsDialog();
        Bundle args = new Bundle();
        args.putLong(ARGS_AMOUNT, amount.getValue());
        fragment.setArguments(args);
        return fragment;
    }

    /*
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context != null && context instanceof CollectRefundOptionsListener) {
            listener = (CollectRefundOptionsListener) context;
        } else {
            Log.e(TAG, "onAttach: context does not implement CollectRefundOptionsListener interface");
        }
    }
    */

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        listener = (CollectRefundOptionsListener) getTargetFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Coin amount = Coin.valueOf(getArguments().getLong(ARGS_AMOUNT, 0L));
        View v = inflater.inflate(R.layout.fragment_collect_refund, container);

        EditText amountText = (EditText) v.findViewById(R.id.amount_edit_text);
        if (amountText != null) {
            amountText.setText(UIUtils.scaleCoinForDialogs(amount, getContext()));
        }

        View sendView = v.findViewById(R.id.collect_refund_send_touch_area);
        if (sendView != null) {
            sendView.setOnClickListener(new SendOptionClickListener());
        }

        View topUpView = v.findViewById(R.id.collect_refund_topup_touch_area);
        if (topUpView != null) {
            topUpView.setOnClickListener(new TopUpOptionClickListener());
        }
        return v;
    }

    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setTitle(R.string.collect_refund_option_dialog_title);
        return dialog;
    }


    private class SendOptionClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (listener != null) {
                listener.onSendOptionSelected();
            }
            dismiss();
        }
    }

    private class TopUpOptionClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (listener != null) {
                listener.onTopUpOptionSelected();
            }
            dismiss();
        }
    }

    public interface CollectRefundOptionsListener {
        void onTopUpOptionSelected();
        void onSendOptionSelected();
    }

}
