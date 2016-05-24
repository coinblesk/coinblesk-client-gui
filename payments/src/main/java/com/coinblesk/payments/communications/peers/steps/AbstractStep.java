package com.coinblesk.payments.communications.peers.steps;

import com.coinblesk.client.config.Constants;
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
        TRANSACTION_ERROR(-4);

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

    private ResultCode resultCode;
    private BitcoinURI bitcoinURI;

    protected AbstractStep() {
        this(null);
    }

    protected AbstractStep(BitcoinURI bitcoinURI) {
        this.bitcoinURI = bitcoinURI;
        setSuccess();
    }

    public BitcoinURI getBitcoinURI() {
        return bitcoinURI;
    }

    public void setBitcoinURI(BitcoinURI bitcoinURI) {
        this.bitcoinURI = bitcoinURI;
    }

    protected boolean isProtocolVersionSupported(int otherVersion) {
        // could add other supported versions here, e.g. with switch fall through
        switch (otherVersion) {
            case Constants.PROTOCOL_VERSION:
                return true;
        }
        return false;
    }

    protected int getProtocolVersion() {
        return Constants.PROTOCOL_VERSION;
    }

    protected void setSuccess() {
        setResultCode(ResultCode.SUCCESS);
    }

    protected void setError() {
        setResultCode(ResultCode.ERROR);
    }

    protected void setResultCode(ResultCode resultCode) {
        this.resultCode = resultCode;
    }

    public ResultCode getResultCode() {
        return resultCode;
    }
}
