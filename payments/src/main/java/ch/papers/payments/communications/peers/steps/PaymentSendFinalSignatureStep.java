package ch.papers.payments.communications.peers.steps;

import ch.papers.payments.communications.messages.DERObject;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 28/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class PaymentSendFinalSignatureStep implements Step{


    @Override
    public DERObject process(DERObject input) {
        /*final Transaction refundTransaction = new Transaction(Constants.PARAMS,input);
        refundTransaction.verify();
        refundTransaction.getInput(0).verify();*/


        return null;
    }
}
