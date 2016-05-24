package com.coinblesk.payments.communications.peers.steps.cltv;

import android.util.Log;
import com.coinblesk.der.DERObject;
import com.coinblesk.payments.communications.peers.steps.AbstractStep;
import com.coinblesk.client.utils.DERPayloadBuilder;
import org.bitcoinj.uri.BitcoinURI;

/**
 * @author Alessandro De Carli
 * @author Andreas Albrecht
 */
public class PaymentRequestSendStep extends AbstractStep {
    private final static String TAG = PaymentRequestSendStep.class.getName();
    private final BitcoinURI requestURI;

    public PaymentRequestSendStep(BitcoinURI requestURI) {
        super();
        this.requestURI = requestURI;
    }

    @Override
    public DERObject process(DERObject input) {
        DERPayloadBuilder builder = new DERPayloadBuilder()
                .add(getProtocolVersion())
                .add(requestURI.getAmount())
                .add(requestURI.getAddress().isP2SHAddress())
                .add(requestURI.getAddress().getHash160());

        DERObject payload = builder.getAsDERSequence();
        Log.d(TAG, String.format(
                "Payment request - sending payment request: %s, (length=%d bytes)",
                requestURI, payload.serializeToDER().length));
        setSuccess();
        return payload;
    }
}