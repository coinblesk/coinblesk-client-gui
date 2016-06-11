/*
 * Copyright 2016 The Coinblesk team and the CSG Group at University of Zurich
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package com.coinblesk.client.utils;

import android.support.annotation.Nullable;

import com.coinblesk.client.ui.dialogs.ProgressSuccessOrFailDialog;
import com.coinblesk.client.utils.AppUtils;
import com.coinblesk.payments.communications.PaymentException;
import com.google.common.util.concurrent.FutureCallback;

import java.lang.ref.WeakReference;

/**
 * @author Andreas Albrecht
 */
public final class PaymentFutureCallback implements FutureCallback<Object> {

    private final WeakReference<ProgressSuccessOrFailDialog> dialogReference;

    public PaymentFutureCallback(ProgressSuccessOrFailDialog dialog)  {
        dialogReference = new WeakReference<>(dialog);
    }

    @Override
    public void onSuccess(@Nullable Object  result) {
        final ProgressSuccessOrFailDialog dialog = dialogReference.get();
        if (dialog != null) {
            dialog.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    dialog.setSuccess();
                }
            });
        }
    }

    @Override
    public void onFailure(final Throwable t) {
        final ProgressSuccessOrFailDialog dialog = dialogReference.get();
        if (dialog != null) {
            dialog.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String message;
                    if (t instanceof PaymentException) {
                        PaymentException pex = (PaymentException) t;
                        message = AppUtils.getPaymentErrorMessage(dialog.getContext(), pex.getErrorCode());
                        if (pex.getMessage() != null && !pex.getMessage().isEmpty()) {
                            message += " (" + pex.getMessage() + ")";
                        }
                    } else {
                        message = t.getMessage();
                    }
                    dialog.setFailure(message);
                }
            });
        }
    }
}
