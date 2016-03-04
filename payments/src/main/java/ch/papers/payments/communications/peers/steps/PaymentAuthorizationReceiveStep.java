package ch.papers.payments.communications.peers.steps;

import android.util.Log;

import com.coinblesk.json.PrepareHalfSignTO;
import com.coinblesk.util.SerializeUtils;
import com.google.common.collect.ImmutableList;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.uri.BitcoinURI;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import ch.papers.payments.Constants;
import ch.papers.payments.Utils;
import ch.papers.payments.communications.http.CoinbleskWebService;
import ch.papers.payments.communications.messages.DERInteger;
import ch.papers.payments.communications.messages.DERObject;
import ch.papers.payments.communications.messages.DERParser;
import ch.papers.payments.communications.messages.DERSequence;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 28/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class PaymentAuthorizationReceiveStep implements Step {
    private final static String TAG = PaymentAuthorizationReceiveStep.class.getSimpleName();

    final private BitcoinURI bitcoinURI;

    private final Retrofit retrofit = new Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(Constants.COINBLESK_SERVER_BASE_URL)
            .build();

    public PaymentAuthorizationReceiveStep(BitcoinURI bitcoinURI) {
        this.bitcoinURI = bitcoinURI;
    }

    @Override
    public DERObject process(DERObject input) {
        try {
            final DERSequence inputSequence = (DERSequence) input;
            final ECKey clientPublicKey = ECKey.fromPublicOnly(inputSequence.getChildren().get(0).getPayload());
            final ECKey.ECDSASignature ecdsaSignature = new ECKey.ECDSASignature(((DERInteger) inputSequence.getChildren().get(2)).getBigInteger(), ((DERInteger) inputSequence.getChildren().get(3)).getBigInteger());
            final BigInteger timestamp = ((DERInteger)inputSequence.getChildren().get(1)).getBigInteger();

            Log.d(TAG,"key used for signing"+clientPublicKey.getPublicKeyAsHex());
            Log.d(TAG,"address used for signing"+bitcoinURI.getAddress());
            Log.d(TAG,"timestamp used for signing"+timestamp.longValue());
            Sha256Hash inputHash = Sha256Hash.of(Utils.concatBytes(BigInteger.valueOf(bitcoinURI.getAmount().getValue()).toByteArray(),bitcoinURI.getAddress().getHash160(),timestamp.toByteArray()));
            Log.d(TAG,"hash used for signing"+inputHash);
            if (clientPublicKey.verify(inputHash, ecdsaSignature)) {
                Log.d(TAG,"verify was successful!");
                final CoinbleskWebService service = retrofit.create(CoinbleskWebService.class);
                // let server sign first
                final PrepareHalfSignTO clientHalfSignTO = new PrepareHalfSignTO();
                clientHalfSignTO.amountToSpend(bitcoinURI.getAmount().longValue());
                clientHalfSignTO.clientPublicKey(clientPublicKey.getPubKey());
                clientHalfSignTO.p2shAddressTo(bitcoinURI.getAddress().toString());
                final PrepareHalfSignTO serverHalfSignTO = service.prepareHalfSign(clientHalfSignTO).execute().body();


                List<DERObject> derObjectList = new ArrayList<DERObject>();
                for (TransactionSignature signature : SerializeUtils.deserializeSignatures(serverHalfSignTO.signatures())) {
                    List<DERObject> signatureList = ImmutableList.<DERObject>of(new DERInteger(signature.r),new DERInteger(signature.s));
                    derObjectList.add(new DERSequence(signatureList));
                }
                byte[] dersequence = new DERSequence(derObjectList).serializeToDER();
                Log.d(TAG,"sending response"+dersequence.length);
                Log.d(TAG,"sending response exp"+ DERParser.extractPayloadEndIndex(dersequence));
                return new DERSequence(derObjectList);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new DERInteger(BigInteger.valueOf(-1));
    }
}
