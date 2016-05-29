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

package com.coinblesk.client.utils;


import android.content.Context;

import com.coinblesk.client.BuildConfig;
import com.coinblesk.client.R;
import com.coinblesk.payments.communications.PaymentError;

/**
 * @author Andreas Albrecht
 */
public final class AppUtils {
    private AppUtils() {
        // prevent instances
    }

    public static String getAppVersion() {
        // strip build number
        String v = getVersionName();
        return v.substring(0, v.lastIndexOf('.'));
    }

    // Version has the form: x.y.build
    // see: build.gradle (app module)
    public static String getVersionName() {
        return BuildConfig.VERSION_NAME;
    }

    public static String getPaymentErrorMessage(Context context, PaymentError errorCode) {
        return context.getString(getPaymentErrorMessageResourceId(context, errorCode));
    }

    public static int getPaymentErrorMessageResourceId(Context context, PaymentError errorCode) {
        switch (errorCode) {
            case PROTOCOL_VERSION_NOT_SUPPORTED:
                return R.string.payment_error_message_protocol_version_not_supported;
            case WRONG_BITCOIN_NETWORK:
                return R.string.payment_error_message_wrong_bitcoin_network;
            case INVALID_PAYMENT_REQUEST:
                return R.string.payment_error_message_invalid_payment_request;
            case DER_SERIALIZE_ERROR:
                return R.string.payment_error_message_der_serialize_error;
            case INSUFFICIENT_FUNDS:
                return R.string.payment_error_message_insufficient_funds;
            case TRANSACTION_ERROR:
                return R.string.payment_error_message_transaction_error;
            case MESSAGE_SIGNATURE_ERROR:
                return R.string.payment_error_message_message_signature_error;
            case SERVER_ERROR:
                return R.string.payment_error_message_server_error;
            case ERROR: /* fall through */
            default:
                return R.string.payment_error_message_error;
        }
    }
}
