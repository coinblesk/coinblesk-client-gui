package ch.papers.payments.communications.peers.handlers;

import org.bitcoinj.uri.BitcoinURI;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import ch.papers.payments.communications.peers.steps.PaymentAuthorizationReceiveStep;
import ch.papers.payments.communications.peers.steps.PaymentRequestSendStep;
import ch.papers.payments.communications.peers.steps.Step;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 04/03/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class InstantPaymentServerHandler extends DERObjectStreamHandler {
    private final static String TAG = InstantPaymentServerHandler.class.getSimpleName();

    private final List<Step> stepList = new ArrayList<Step>();

    public InstantPaymentServerHandler(InputStream inputStream, OutputStream outputStream, BitcoinURI paymentUri) {
        super(inputStream,outputStream);
        this.stepList.add(new PaymentRequestSendStep(paymentUri));
        this.stepList.add(new PaymentAuthorizationReceiveStep(paymentUri));
    }


    @Override
    public void run() {
        int stepCounter = 0;

        writeDERObject(stepList.get(stepCounter++).process(readDERObject()));
        writeDERObject(stepList.get(stepCounter++).process(readDERObject()));
        //writeDERObject();
    }


}
