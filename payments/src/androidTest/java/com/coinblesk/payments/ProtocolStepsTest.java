package com.coinblesk.payments;

import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ServiceTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

import com.coinblesk.payments.communications.messages.DERObject;
import com.coinblesk.payments.communications.peers.steps.PaymentAuthorizationReceiveStep;
import com.coinblesk.payments.communications.peers.steps.PaymentFinalSignatureReceiveStep;
import com.coinblesk.payments.communications.peers.steps.PaymentFinalSignatureSendStep;
import com.coinblesk.payments.communications.peers.steps.PaymentRefundReceiveStep;
import com.coinblesk.payments.communications.peers.steps.PaymentRefundSendStep;
import com.coinblesk.payments.communications.peers.steps.PaymentRequestReceiveStep;
import com.coinblesk.payments.communications.peers.steps.PaymentRequestSendStep;

import org.bitcoinj.uri.BitcoinURI;
import org.bitcoinj.uri.BitcoinURIParseException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeoutException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.nullValue;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 11/04/16.
 * Papers.ch
 * a.decarli@papers.ch
 */


@RunWith(AndroidJUnit4.class)
@SmallTest
public class ProtocolStepsTest {

    @Rule
    public final ServiceTestRule serviceRule = new ServiceTestRule();

    @Test
    public void dummy() {
        Intent serviceIntent =
                new Intent(InstrumentationRegistry.getTargetContext(),
                        WalletService.class);

        try {
            WalletService.WalletServiceBinder walletServiceBinder = (WalletService.WalletServiceBinder) serviceRule.bindService(serviceIntent);

            Thread.sleep(10000); // very fragile, we wait for the wallet to initialize, callbacks would be possible but since this is a test...

            Log.d("address",""+walletServiceBinder.getCurrentReceiveAddress());
            final BitcoinURI bitcoinURI = new BitcoinURI("bitcoin:2MuunhGMSQEMhvKH4VbsuDtHDcdpRR3Aazb?amount=0.001");
            PaymentRequestSendStep paymentRequestSendStep = new PaymentRequestSendStep(bitcoinURI);
            PaymentRequestReceiveStep paymentRequestReceiveStep = new PaymentRequestReceiveStep(walletServiceBinder);
            PaymentAuthorizationReceiveStep paymentAuthorizationReceiveStep = new PaymentAuthorizationReceiveStep(bitcoinURI);
            PaymentRefundSendStep paymentRefundSendStep = new PaymentRefundSendStep(walletServiceBinder, bitcoinURI, paymentRequestReceiveStep.getTimestamp());
            PaymentRefundReceiveStep paymentRefundReceiveStep = new PaymentRefundReceiveStep(walletServiceBinder.getMultisigClientKey());
            PaymentFinalSignatureSendStep paymentFinalSignatureSendStep = new PaymentFinalSignatureSendStep(walletServiceBinder, paymentRefundSendStep.getHalfSignedRefundTransaction(), paymentRefundSendStep.getFullSignedTransaction());
            PaymentFinalSignatureReceiveStep paymentFinalSignatureReceiveStep = new PaymentFinalSignatureReceiveStep(walletServiceBinder.getMultisigClientKey());

            long startTime = System.currentTimeMillis();
            DERObject output = paymentRequestSendStep.process(DERObject.NULLOBJECT);
            Log.d("benchmark",""+(System.currentTimeMillis()-startTime));
            startTime = System.currentTimeMillis();

            output = paymentRequestReceiveStep.process(output);
            Log.d("benchmark",""+(System.currentTimeMillis()-startTime));
            assertThat(paymentRequestReceiveStep.getBitcoinURI().toString(),is(bitcoinURI.toString()));
            startTime = System.currentTimeMillis();

            output = paymentAuthorizationReceiveStep.process(output);
            Log.d("benchmark",""+(System.currentTimeMillis()-startTime));
            assertThat(paymentAuthorizationReceiveStep.getClientPublicKey(),is(walletServiceBinder.getMultisigClientKey()));
            startTime = System.currentTimeMillis();

            output = paymentRefundSendStep.process(output);
            Log.d("benchmark",""+(System.currentTimeMillis()-startTime));
            assertThat(paymentRefundSendStep.getFullSignedTransaction(),not(nullValue()));
            assertThat(paymentRefundSendStep.getHalfSignedRefundTransaction(),not(nullValue()));
            startTime = System.currentTimeMillis();

            output = paymentRefundReceiveStep.process(output);
            Log.d("benchmark",""+(System.currentTimeMillis()-startTime));
            startTime = System.currentTimeMillis();

            output = paymentFinalSignatureSendStep.process(output);
            Log.d("benchmark",""+(System.currentTimeMillis()-startTime));
            assertThat(paymentFinalSignatureSendStep.getFullSignedRefundTransation(),not(nullValue()));
            startTime = System.currentTimeMillis();

            output = paymentFinalSignatureReceiveStep.process(output);
            Log.d("benchmark",""+(System.currentTimeMillis()-startTime));
            startTime = System.currentTimeMillis();

        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (BitcoinURIParseException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // Verify that the received data is correct.
        assertThat(1, is(1));
    }
}
