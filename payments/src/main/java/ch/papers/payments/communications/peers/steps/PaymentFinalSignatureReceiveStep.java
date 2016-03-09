package ch.papers.payments.communications.peers.steps;

import android.util.Log;

import com.coinblesk.json.CompleteSignTO;
import com.coinblesk.json.TxSig;
import com.coinblesk.util.SerializeUtils;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.ECKey;

import java.io.IOException;
import java.math.BigInteger;

import ch.papers.payments.Constants;
import ch.papers.payments.communications.http.CoinbleskWebService;
import ch.papers.payments.communications.messages.DERInteger;
import ch.papers.payments.communications.messages.DERObject;
import ch.papers.payments.communications.messages.DERSequence;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 28/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class PaymentFinalSignatureReceiveStep implements Step {
    private final static String TAG = PaymentFinalSignatureReceiveStep.class.getSimpleName();

    private final Retrofit retrofit = new Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create(SerializeUtils.GSON))
            .baseUrl(Constants.COINBLESK_SERVER_BASE_URL)
            .build();

    private final ECKey multisigClientKey;
    private final Address recipientAddress;

    public PaymentFinalSignatureReceiveStep(ECKey multisigClientKey, Address recipientAddress) {
        this.multisigClientKey = multisigClientKey;
        this.recipientAddress = recipientAddress;
    }

    @Override
    public DERObject process(DERObject input) {
        try {
            final DERSequence derSequence = (DERSequence) input;
            final BigInteger timestamp = ((DERInteger) derSequence.getChildren().get(0)).getBigInteger();
            final TxSig txSig = new TxSig();
            txSig.sigR(((DERInteger) derSequence.getChildren().get(2)).getBigInteger().toString());
            txSig.sigS(((DERInteger) derSequence.getChildren().get(3)).getBigInteger().toString());

            CompleteSignTO completeSignTO = new CompleteSignTO()
                    .clientPublicKey(multisigClientKey.getPubKey())
                    .p2shAddressTo(recipientAddress.toString())
                    .fullSignedTransaction(derSequence.getChildren().get(1).getPayload())
                    .messageSig(txSig)
                    .currentDate(timestamp.longValue());

            final CoinbleskWebService service = retrofit.create(CoinbleskWebService.class);


            CompleteSignTO responseCompleteSignTO = service.sign(completeSignTO).execute().body();
            Log.d(TAG, "instant payment was " + responseCompleteSignTO.type());
            return DERObject.NULLOBJECT;
        } catch (IOException e){
            e.printStackTrace();
        }

        return null;
    }
}
