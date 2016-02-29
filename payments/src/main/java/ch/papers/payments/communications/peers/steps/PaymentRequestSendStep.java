package ch.papers.payments.communications.peers.steps;

import org.bitcoinj.uri.BitcoinURI;

import java.nio.ByteBuffer;

import ch.papers.payments.Utils;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 28/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class PaymentRequestSendStep implements Step {
    final private byte[] payload;

    public PaymentRequestSendStep(BitcoinURI bitcoinURI){
        byte[] amountBytes = ByteBuffer.allocate(Long.SIZE / Byte.SIZE).putLong(bitcoinURI.getAmount().getValue()).array();
        payload = Utils.concatBytes(amountBytes,bitcoinURI.getAddress().getHash160());
    }

    @Override
    public int expectedInputLength() {
        return 0;
    }

    @Override
    public byte[] process(byte[] input) {
        return payload;
    }
}
