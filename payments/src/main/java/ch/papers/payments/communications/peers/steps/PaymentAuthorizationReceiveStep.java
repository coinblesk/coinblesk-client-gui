package ch.papers.payments.communications.peers.steps;

import com.coinblesk.json.PrepareHalfSignTO;
import com.coinblesk.util.SerializeUtils;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.uri.BitcoinURI;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import ch.papers.payments.Constants;
import ch.papers.payments.Utils;
import ch.papers.payments.communications.http.CoinbleskWebService;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 28/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class PaymentAuthorizationReceiveStep implements Step {
    final private BitcoinURI bitcoinURI;
    final private byte[] payload;

    private final Retrofit retrofit = new Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(Constants.COINBLESK_SERVER_BASE_URL)
            .build();

    public PaymentAuthorizationReceiveStep(BitcoinURI bitcoinURI) {
        this.bitcoinURI = bitcoinURI;
        byte[] amountBytes = ByteBuffer.allocate(Long.SIZE / Byte.SIZE).putLong(bitcoinURI.getAmount().getValue()).array();
        payload = Utils.concatBytes(amountBytes, bitcoinURI.getAddress().getHash160());
    }

    @Override
    public int expectedInputLength() {
        return 160;
    }

    @Override
    public byte[] process(byte[] input) {
        try {
            int signatureSize = input[0];
            byte[] signatureBytes = Arrays.copyOfRange(input, 1, signatureSize);
            byte[] publicKeyBytes = Arrays.copyOfRange(input, signatureSize, 32);
            ECKey clientPublicKey = ECKey.fromPublicOnly(publicKeyBytes);
            if (clientPublicKey.verify(payload, signatureBytes)) {
                final CoinbleskWebService service = retrofit.create(CoinbleskWebService.class);
                // let server sign first
                final PrepareHalfSignTO clientHalfSignTO = new PrepareHalfSignTO();
                clientHalfSignTO.amountToSpend(bitcoinURI.getAmount().longValue());
                clientHalfSignTO.clientPublicKey(publicKeyBytes);
                clientHalfSignTO.p2shAddressTo(bitcoinURI.getAddress().toString());
                final PrepareHalfSignTO serverHalfSignTO = service.prepareHalfSign(clientHalfSignTO).execute().body();
                for (TransactionSignature signature:SerializeUtils.deserializeSignatures(serverHalfSignTO.signatures())) {
                    // TODO: handle multiple
                    byte[] paddingTail = new byte[80-signature.encodeToDER().length];
                    return Utils.concatBytes(signature.encodeToDER(),paddingTail);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new byte[0];
    }
}
