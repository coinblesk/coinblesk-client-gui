package com.coinblesk.payments.communications.steps.cltv;

import android.util.Log;

import com.coinblesk.client.config.Constants;
import com.coinblesk.der.DERObject;
import com.coinblesk.der.DERSequence;
import com.coinblesk.payments.communications.steps.AbstractStep;
import com.coinblesk.client.utils.DERPayloadParser;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.uri.BitcoinURI;

/**
 * @author Alessandro De Carli
 * @author Andreas Albrecht
 */
public class PaymentRequestReceiveStep extends AbstractStep {
    private final static String TAG = PaymentRequestReceiveStep.class.getName();

    public PaymentRequestReceiveStep() {
        super();
    }

    @Override
    public DERObject process(DERObject input) {
        final int protocolVersion;
        final Address addressTo;
        final Coin amount;
        try {
            final DERSequence derSequence = (DERSequence) input;
            final DERPayloadParser parser = new DERPayloadParser(derSequence);
            final NetworkParameters params = Constants.PARAMS;

            /* protocol version */
            protocolVersion = parser.getInt();
            Log.d(TAG, "Received protocol version: " + protocolVersion);
            if (!isProtocolVersionSupported(protocolVersion)) {
                Log.w(TAG, String.format(
                        "Protocol version not supported. ours: %d - theirs: %d",
                        getProtocolVersion(), protocolVersion));
                setResultCode(ResultCode.PROTOCOL_VERSION_NOT_SUPPORTED);
                return DERObject.NULLOBJECT;
            }

            /* payment amount */
            amount = parser.getCoin();
            Log.d(TAG, "Received amount: " + amount);

            /* payment address */
            boolean isP2SH = parser.getBoolean();
            byte[] addressPayload = parser.getBytes();
            if (isP2SH) {
                addressTo = Address.fromP2SHHash(params, addressPayload);
            } else {
                addressTo = new Address(params, addressPayload);
            }
            Log.d(TAG, "Received address: " + addressTo);

            String bitcoinURIStr = BitcoinURI.convertToBitcoinURI(addressTo, amount, "", "");
            setBitcoinURI(new BitcoinURI(bitcoinURIStr));
            Log.i(TAG, "Received payment request: " + getBitcoinURI());
            setSuccess();
        } catch (Exception e) {
            Log.w(TAG, "Parse error: ", e);
            setResultCode(ResultCode.PARSE_ERROR);
        }
        return DERObject.NULLOBJECT;
    }
}