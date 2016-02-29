package ch.papers.payments.communications.peers.steps;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Sha256Hash;

import java.security.SecureRandom;

import ch.papers.payments.Utils;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 28/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class PaymentRequestReceiveStep implements Step{
    private final ECKey ecKey;
    private final static int PAYLOAD_SIZE = 80+80;

    public PaymentRequestReceiveStep(ECKey ecKey){
        this.ecKey = ecKey;
    }

    @Override
    public int expectedInputLength() {
        return 28;
    }

    @Override
    public byte[] process(byte[] input) {
        Sha256Hash inputHash = Sha256Hash.of(input);
        final byte[] signaturePayload = this.ecKey.sign(inputHash).encodeToDER();
        byte signatureLength = (byte)signaturePayload.length;
        byte[] randomTail = new byte[PAYLOAD_SIZE-(signatureLength+80)];
        new SecureRandom().nextBytes(randomTail);

        return Utils.concatBytes(new byte[]{signatureLength},signaturePayload,this.ecKey.getPubKey(),randomTail);
    }
}
