package ch.papers.payments.communications.peers;

import org.bitcoinj.uri.BitcoinURI;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 05/03/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public interface PaymentRequestAuthorizer {
    PaymentRequestAuthorizer DISALLOW_AUTHORIZER = new PaymentRequestAuthorizer() {
        @Override
        public boolean isPaymentRequestAuthorized(BitcoinURI paymentRequest) {
            return false;
        }
    };

    PaymentRequestAuthorizer ALLOW_AUTHORIZER = new PaymentRequestAuthorizer() {
        @Override
        public boolean isPaymentRequestAuthorized(BitcoinURI paymentRequest) {
            return true;
        }
    };

    public boolean isPaymentRequestAuthorized(BitcoinURI paymentRequest);
}
