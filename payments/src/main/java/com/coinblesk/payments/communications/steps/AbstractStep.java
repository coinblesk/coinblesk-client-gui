package com.coinblesk.payments.communications.steps;

import com.coinblesk.client.config.Constants;
import com.coinblesk.der.DERObject;
import com.coinblesk.payments.communications.PaymentException;

import org.bitcoinj.uri.BitcoinURI;

/**
 * @author Andreas Albrecht
 */
public abstract class AbstractStep implements Step {

    public enum ResultCode {
        // positive: success, negative: error
        SUCCESS(1),
        ERROR(0),
        PROTOCOL_VERSION_NOT_SUPPORTED(-1),
        PARSE_ERROR(-2),
        INSUFFICIENT_FUNDS(-3),
        TRANSACTION_ERROR(-4),
        SIGNATURE_ERROR(-5),
        SERVER_ERROR(-6);

        private final int code;

        ResultCode(int code) {
            this.code = code;
        }

        public boolean isSuccess() {
            return (code > 0);
        }

        public boolean isError() {
            return !isSuccess();
        }
    }

    private BitcoinURI bitcoinURI;

    protected AbstractStep() {
        this(null);
    }

    protected AbstractStep(BitcoinURI bitcoinURI) {
        this.bitcoinURI = bitcoinURI;
    }

    public BitcoinURI getBitcoinURI() {
        return bitcoinURI;
    }

    public void setBitcoinURI(BitcoinURI bitcoinURI) {
        this.bitcoinURI = bitcoinURI;
    }

    @Override
    public abstract DERObject process(DERObject input) throws PaymentException;

    protected boolean isProtocolVersionSupported(int otherVersion) {
        // could add other supported versions here, e.g. with switch fall through
        switch (otherVersion) {
            case Constants.CLIENT_COMMUNICATION_PROTOCOL_VERSION:
                return true;
        }
        return false;
    }

    protected int getProtocolVersion() {
        return Constants.CLIENT_COMMUNICATION_PROTOCOL_VERSION;
    }

}
