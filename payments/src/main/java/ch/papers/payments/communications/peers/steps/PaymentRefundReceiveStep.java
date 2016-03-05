package ch.papers.payments.communications.peers.steps;

import android.util.Log;

import org.bitcoinj.core.ECKey;

import ch.papers.payments.Constants;
import ch.papers.payments.communications.messages.DERObject;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 28/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class PaymentRefundReceiveStep implements Step {
    private final static String TAG = PaymentRefundReceiveStep.class.getSimpleName();

    private final Retrofit retrofit = new Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(Constants.COINBLESK_SERVER_BASE_URL)
            .build();

    private final ECKey multisigClientKey;

    public PaymentRefundReceiveStep(ECKey multisigClientKey) {
        this.multisigClientKey = multisigClientKey;
    }

    @Override
    public DERObject process(DERObject input) {
        Log.d(TAG,"received refund");
/*        int refundTransactionSize = input[0];
        int refundTransactionSignatureSize = input[refundTransactionSize+1];

        byte[] refundTransactionBytes = Arrays.copyOfRange(input, 1, refundTransactionSize);
        byte[] refundTransactionSignature =Arrays.copyOfRange(input, refundTransactionSize, refundTransactionSignatureSize);

        TransactionSignature transactionSignature = new TransactionSignature(TransactionSignature.decodeFromDER(refundTransactionSignature), Transaction.SigHash.ALL, false);

        final RefundTO clientRefundTO = new RefundTO();
        clientRefundTO.clientPublicKey(multisigClientKey.getPubKey());
        clientRefundTO.clientSignatures(SerializeUtils.serializeSignatures(ImmutableList.of(transactionSignature)));
        clientRefundTO.refundTransaction(refundTransactionBytes);


        final CoinbleskWebService service = retrofit.create(CoinbleskWebService.class);
        try {
            final RefundTO serverRefundTo = service.refund(clientRefundTO).execute().body();
            return serverRefundTo.refundTransaction();
        } catch (IOException e) {
            e.printStackTrace();
        }*/
        return null;
    }
}
