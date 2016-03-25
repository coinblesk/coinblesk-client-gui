package com.coinblesk.payments.communications.peers;

import org.bitcoinj.uri.BitcoinURI;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 05/03/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public interface PaymentRequestDelegate {
    PaymentRequestDelegate DISALLOW_AUTHORIZER = new PaymentRequestDelegate() {
        @Override
        public boolean isPaymentRequestAuthorized(BitcoinURI paymentRequest) {
            return false;
        }

        @Override
        public void onPaymentSuccess() {

        }

        @Override
        public void onPaymentError(String errorMessage) {

        }
    };

    PaymentRequestDelegate ALLOW_DELEGATE = new PaymentRequestDelegate() {
        @Override
        public boolean isPaymentRequestAuthorized(BitcoinURI paymentRequest) {
            return true;
        }

        @Override
        public void onPaymentSuccess() {

        }

        @Override
        public void onPaymentError(String errorMessage) {

        }
    };

    public boolean isPaymentRequestAuthorized(BitcoinURI paymentRequest);

    public void onPaymentSuccess();

    public void onPaymentError(String errorMessage);
}
