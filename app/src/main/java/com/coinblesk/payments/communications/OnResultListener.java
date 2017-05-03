package com.coinblesk.payments.communications;

public interface OnResultListener<T> {

    void onSuccess(T result);

    void onError(String message);
}
