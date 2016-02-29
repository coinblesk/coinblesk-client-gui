package ch.papers.payments.communications.peers.steps;

import org.bitcoinj.core.Transaction;

import ch.papers.payments.Constants;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 28/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class PaymentSendFinalSignatureStep implements Step{


    @Override
    public int expectedInputLength() {
        return 0;
    }

    @Override
    public byte[] process(byte[] input) {
        final Transaction refundTransaction = new Transaction(Constants.PARAMS,input);
        refundTransaction.verify();
        refundTransaction.getInput(0).verify();


        return new byte[0];
    }
}
