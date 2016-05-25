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

package com.coinblesk.payments.communications;


/**
 * @author Andreas Albrecht
 */
public class PaymentException extends Exception {

    private PaymentError errorCode;

    public PaymentException(PaymentError errorCode) {
        this(errorCode, errorCode.toString());
    }

    public PaymentException(PaymentError errorCode, String message) {
        this(message);
        this.errorCode = errorCode;
    }

    public PaymentException(PaymentError errorCode, Throwable cause) {
        this(errorCode.toString(), cause);
        this.errorCode = errorCode;
    }

    private PaymentException(String message) {
        super(message);
        errorCode = PaymentError.ERROR;
    }

    private PaymentException(String message, Throwable cause) {
        super(message, cause);
        errorCode = PaymentError.ERROR;
    }

    public PaymentError getErrorCode() {
        return errorCode;
    }

}
