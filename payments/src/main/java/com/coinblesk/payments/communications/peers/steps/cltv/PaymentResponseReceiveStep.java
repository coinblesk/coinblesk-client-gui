package com.coinblesk.payments.communications.peers.steps.cltv;

import android.util.Log;
import com.coinblesk.json.SignTO;
import com.coinblesk.json.TxSig;
import com.coinblesk.client.config.Constants;
import com.coinblesk.payments.communications.http.CoinbleskWebService;
import com.coinblesk.der.DERObject;
import com.coinblesk.payments.communications.peers.steps.AbstractStep;
import com.coinblesk.client.utils.DERPayloadBuilder;
import com.coinblesk.client.utils.DERPayloadParser;
import com.coinblesk.der.DERSequence;
import com.coinblesk.util.SerializeUtils;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.uri.BitcoinURI;
import retrofit2.Response;

import java.io.IOException;
import java.util.List;

/**
 * @author Alessandro De Carli
 * @author Andreas Albrecht
 */
public class PaymentResponseReceiveStep extends AbstractStep {
    private final static String TAG = PaymentResponseReceiveStep.class.getName();

    public PaymentResponseReceiveStep(BitcoinURI bitcoinURI) {
        super(bitcoinURI);
    }

    @Override
    public DERObject process(DERObject input) {
        try {
            final DERSequence inputSequence = (DERSequence) input;
            final DERPayloadParser parser = new DERPayloadParser(inputSequence);
            final int protocolVersion = parser.getInt(); // TODO: check it
            isProtocolVersionSupported(protocolVersion);

            SignTO signTO = extractSignTO(parser);
            ECKey clientPublicKey = ECKey.fromPublicOnly(signTO.publicKey());

            Log.d(TAG, String.format("Got signTO: pubKey of message: %s, address(hex): %s, timestamp of signing: %s",
                    clientPublicKey.getPublicKeyAsHex(), getBitcoinURI().getAddress(), signTO.currentDate()));

            if (!SerializeUtils.verifyJSONSignature(signTO, clientPublicKey)) {
                // TODO: error!
                setError();
            }

            Log.d(TAG, "signTO - verify successful");
            final CoinbleskWebService service = Constants.RETROFIT.create(CoinbleskWebService.class);
            final Response<SignTO> serverResponse = service.signTx(signTO).execute();
            // TODO: IF payment was successful but tag is lost now --> payment went through even tough tag is lost exception is thrown (but client will not get Tx/signatures from server).
            if (!serverResponse.isSuccess()) {
                // TODO: error
                setError();
            }

            final SignTO serverSignTO = serverResponse.body();
            if (!serverSignTO.isSuccess()) {
                // TODO: error
                setError();
            }

            DERPayloadBuilder builder = new DERPayloadBuilder()
                    .add(getProtocolVersion());
            appendSignTO(builder, serverSignTO);
            DERObject response = builder.getAsDERSequence();
            setSuccess();
            return response;

        } catch (IOException e) {
            Log.e(TAG, "Exception in the authorization step: ", e);
        }
        return DERObject.NULLOBJECT;
    }

    private void appendSignTO(DERPayloadBuilder builder, SignTO signTO) {
        builder.add(signTO.type().nr())
                .add(signTO.signatures());
    }

    private SignTO extractSignTO(DERPayloadParser parser) {
        long currentDate = parser.getLong();
        byte[] publicKey = parser.getBytes();
        byte[] serializedTx = parser.getBytes();
        List<TxSig> signatures = parser.getTxSigList();
        TxSig messageSig = parser.getTxSig();

        SignTO signTO = new SignTO()
                .currentDate(currentDate)
                .publicKey(publicKey)
                .transaction(serializedTx)
                .signatures(signatures)
                .messageSig(messageSig);
        return signTO;
    }
}
