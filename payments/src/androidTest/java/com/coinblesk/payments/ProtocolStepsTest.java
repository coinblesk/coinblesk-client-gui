package com.coinblesk.payments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ServiceTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.content.LocalBroadcastManager;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

import com.coinblesk.der.DERObject;
import com.coinblesk.payments.communications.steps.PaymentAuthorizationReceiveStep;
import com.coinblesk.payments.communications.steps.PaymentFinalSignatureOutpointsReceiveStep;
import com.coinblesk.payments.communications.steps.PaymentFinalSignatureOutpointsSendStep;
import com.coinblesk.payments.communications.steps.PaymentRefundReceiveStep;
import com.coinblesk.payments.communications.steps.PaymentRefundSendStep;
import com.coinblesk.payments.communications.steps.PaymentRequestReceiveStep;
import com.coinblesk.payments.communications.steps.PaymentRequestSendStep;

import org.bitcoinj.uri.BitcoinURI;
import org.bitcoinj.uri.BitcoinURIParseException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeoutException;

import ch.papers.objectstorage.UuidObjectStorage;

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

    private final CountDownLatch countDownLatch = new CountDownLatch(1);
    private boolean isRunning = false;
    private final BroadcastReceiver walletBalanceChangeBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!isRunning) {
                isRunning = true;

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(10000);
                            Log.d("my address", "" + walletServiceBinder.getCurrentReceiveAddress());
                            final BitcoinURI bitcoinURI = new BitcoinURI("bitcoin:msLAVManatorGU1k9Xf4otoLD5on93537U?amount=0.0001");
                            PaymentRequestSendStep paymentRequestSendStep = new PaymentRequestSendStep(bitcoinURI);
                            PaymentRequestReceiveStep paymentRequestReceiveStep = new PaymentRequestReceiveStep(walletServiceBinder);
                            PaymentAuthorizationReceiveStep paymentAuthorizationReceiveStep = new PaymentAuthorizationReceiveStep(bitcoinURI);
                            PaymentRefundSendStep paymentRefundSendStep = new PaymentRefundSendStep(walletServiceBinder, bitcoinURI, paymentRequestReceiveStep.getTimestamp());
                            PaymentRefundReceiveStep paymentRefundReceiveStep = new PaymentRefundReceiveStep(walletServiceBinder.getMultisigClientKey());


                            long startTime = System.currentTimeMillis();
                            DERObject output = paymentRequestSendStep.process(DERObject.NULLOBJECT);
                            Log.d("benchmark", "" + (System.currentTimeMillis() - startTime));
                            startTime = System.currentTimeMillis();

                            output = paymentRequestReceiveStep.process(output);
                            Log.d("benchmark", "" + (System.currentTimeMillis() - startTime));
                            assertThat(paymentRequestReceiveStep.getBitcoinURI().toString(), is(bitcoinURI.toString()));
                            startTime = System.currentTimeMillis();

                            output = paymentAuthorizationReceiveStep.process(output);
                            Log.d("benchmark", "" + (System.currentTimeMillis() - startTime));
                            assertThat(paymentAuthorizationReceiveStep.getClientPublicKey().getPublicKeyAsHex(), is(walletServiceBinder.getMultisigClientKey().getPublicKeyAsHex()));
                            startTime = System.currentTimeMillis();

                            output = paymentRefundSendStep.process(output);
                            Log.d("benchmark", "" + (System.currentTimeMillis() - startTime));
                            assertThat(paymentRefundSendStep.getFullSignedTransaction(), not(nullValue()));
                            assertThat(paymentRefundSendStep.getHalfSignedRefundTransaction(), not(nullValue()));
                            startTime = System.currentTimeMillis();

                            output = paymentRefundReceiveStep.process(output);
                            Log.d("benchmark", "" + (System.currentTimeMillis() - startTime));
                            startTime = System.currentTimeMillis();


                            PaymentFinalSignatureOutpointsSendStep paymentFinalSignatureOutpointsSendStep = new PaymentFinalSignatureOutpointsSendStep(walletServiceBinder, bitcoinURI.getAddress(), paymentRefundSendStep.getClientSignatures(), paymentRefundSendStep.getServerSignatures(), paymentRefundSendStep.getFullSignedTransaction(), paymentRefundSendStep.getHalfSignedRefundTransaction());
                            PaymentFinalSignatureOutpointsReceiveStep paymentFinalSignatureOutpointsReceiveStep = new PaymentFinalSignatureOutpointsReceiveStep(walletServiceBinder.getMultisigClientKey(), paymentRefundSendStep.getServerSignatures(), bitcoinURI);
                            output = paymentFinalSignatureOutpointsSendStep.process(output);
                            Log.d("benchmark", "" + (System.currentTimeMillis() - startTime));
                            assertThat(paymentFinalSignatureOutpointsSendStep.getFullSignedRefundTransation(), not(nullValue()));
                            startTime = System.currentTimeMillis();

                            output = paymentFinalSignatureOutpointsReceiveStep.process(output);
                            Log.d("benchmark", "" + (System.currentTimeMillis() - startTime));
                            startTime = System.currentTimeMillis();
                            countDownLatch.countDown();
                        } catch (BitcoinURIParseException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                    }
                }).start();
            }
        }
    };

    private WalletService.WalletServiceBinder walletServiceBinder;


    @Test
    public void dummy() {
        Intent serviceIntent =
                new Intent(InstrumentationRegistry.getTargetContext(),
                        WalletService.class);

        File objectStorageDir = new File(InstrumentationRegistry.getTargetContext().getFilesDir(), Constants.WALLET_FILES_PREFIX + "_uuid_object_storage");
        objectStorageDir.mkdirs();
        UuidObjectStorage.getInstance().init(objectStorageDir);

        IntentFilter filter = new IntentFilter(Constants.WALLET_TRANSACTIONS_CHANGED_ACTION);
        filter.addAction(Constants.WALLET_READY_ACTION);
        LocalBroadcastManager.getInstance(InstrumentationRegistry.getTargetContext()).registerReceiver(walletBalanceChangeBroadcastReceiver, filter);

        try {
            walletServiceBinder = (WalletService.WalletServiceBinder) serviceRule.bindService(serviceIntent);
            countDownLatch.await();
        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
