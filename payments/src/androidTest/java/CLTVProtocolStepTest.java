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

import com.coinblesk.client.CoinbleskApp;
import com.coinblesk.client.config.Constants;
import com.coinblesk.client.utils.SharedPrefUtils;
import com.coinblesk.der.DERObject;
import com.coinblesk.payments.WalletService;
import com.coinblesk.payments.communications.steps.cltv.PaymentFinalizeStep;
import com.coinblesk.payments.communications.steps.cltv.PaymentRequestReceiveStep;
import com.coinblesk.payments.communications.steps.cltv.PaymentRequestSendStep;
import com.coinblesk.payments.communications.steps.cltv.PaymentResponseReceiveStep;
import com.coinblesk.payments.communications.steps.cltv.PaymentResponseSendStep;
import com.coinblesk.payments.communications.steps.cltv.PaymentServerSignatureReceiveStep;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.uri.BitcoinURI;
import org.bitcoinj.wallet.Wallet;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class CLTVProtocolStepTest {

    private static final String TAG = CLTVProtocolStepTest.class.getName();

    // testnet faucet: https://testnet.manu.backend.hamburg/faucet
    private static final Address TARGET_ADDRESS = Address.fromBase58(null, "mwCwTceJvYV27KXBc3NJZys6CjsgsoeHmf");

    private static final int NUMBER_OF_PAYMENTS = 30;
    private static final int PAYMENTS_INTERVAL_SECONDS = 60;
    private static final Coin AMOUNT = Coin.MILLICOIN;

    @Rule
    public final ServiceTestRule serviceRule = new ServiceTestRule();

    private final CountDownLatch totalPaymentsCountDown = new CountDownLatch(NUMBER_OF_PAYMENTS);
    private boolean isRunning = false;
    private AtomicInteger paymentsDone = new AtomicInteger(0);
    private WalletService.WalletServiceBinder walletServiceBinder;

    private final BroadcastReceiver walletBalanceChangeBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (isRunning) {
                return;
            }

            isRunning = true;

            sleep(5); // make sure init is done

            Log.d(TAG, "Current receive address: " + walletServiceBinder.getCurrentReceiveAddress().toBase58());

            Coin requiredBalance = AMOUNT.times(NUMBER_OF_PAYMENTS).add(Coin.CENT); // + some fee

            try {
                if (walletServiceBinder.getBalance().isLessThan(requiredBalance)) {
                    Log.d(TAG, "waiting for top up... > " + requiredBalance.toFriendlyString());
                    walletServiceBinder
                            .getWallet()
                            .getBalanceFuture(requiredBalance, Wallet.BalanceType.ESTIMATED)
                            .get(5, TimeUnit.MINUTES);
                }

                for (int i = 0; i < NUMBER_OF_PAYMENTS; ++i) {
                    final CountDownLatch nextPaymentCountDown = new CountDownLatch(1);

                    new Thread(new PaymentRunnable(nextPaymentCountDown)).start();

                    nextPaymentCountDown.await();
                    Log.d(TAG, "Payment done: " + (i+1) + "/" + NUMBER_OF_PAYMENTS);
                    sleep(PAYMENTS_INTERVAL_SECONDS);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    };

    @BeforeClass
    public static void before() {
        SharedPrefUtils.setNetwork(InstrumentationRegistry.getTargetContext(), "testnet");
        assertThat(SharedPrefUtils.isNetworkTestnet(InstrumentationRegistry.getTargetContext()), is(true));

        Context c = InstrumentationRegistry.getTargetContext().getApplicationContext();
        CoinbleskApp app = (CoinbleskApp) c;
        app.refreshAppConfig(
                c.getString(com.coinblesk.client.common.R.string.pref_network_testnet));
    }

    private class PaymentRunnable implements Runnable {
        CountDownLatch paymentDoneCounDown;

        public PaymentRunnable(CountDownLatch paymentDoneCountDown) {
            this.paymentDoneCounDown = paymentDoneCountDown;
        }

        @Override
        public void run() {
            try {
                NetworkParameters params = walletServiceBinder.getNetworkParameters();

                // create new address to receive next change
                walletServiceBinder.createTimeLockedAddress();

                Address changeAddress = walletServiceBinder.getCurrentReceiveAddress();
                Log.d(TAG, "Current receive address (for change): " + changeAddress.toBase58());

                BitcoinURI paymentRequestUri = new BitcoinURI("bitcoin:"+TARGET_ADDRESS.toBase58()+"?amount="+AMOUNT.toPlainString());

                Log.d(TAG, "Payment URI: " + paymentRequestUri.toString());

                ECKey signKey = walletServiceBinder.getMultisigClientKey();

                // execute payment protocol:
                DERObject output;
                PaymentRequestSendStep requestSend = new PaymentRequestSendStep(paymentRequestUri, signKey);
                output = requestSend.process(null);
                assertNotNull(output);

                PaymentRequestReceiveStep requestReceive = new PaymentRequestReceiveStep(params);
                requestReceive.process(output);
                assertNotNull(requestReceive.paymentRequest());

                PaymentResponseSendStep responseSend = new PaymentResponseSendStep(paymentRequestUri, walletServiceBinder);
                output = responseSend.process(null);
                assertNotNull(output);
                assertNotNull(responseSend.getTransaction());
                assertNotNull(responseSend.getClientTransactionSignatures());
                assertTrue(responseSend.getClientTransactionSignatures().size() > 0);

                PaymentResponseReceiveStep responseReceive = new PaymentResponseReceiveStep(paymentRequestUri, walletServiceBinder);
                output = responseReceive.process(output);
                assertNotNull(output);

                PaymentServerSignatureReceiveStep signaturesReceive = new PaymentServerSignatureReceiveStep(walletServiceBinder);
                signaturesReceive.process(output);
                assertNotNull(signaturesReceive.serverResponse());
                assertNotNull(signaturesReceive.getServerTransactionSignatures());
                assertTrue(signaturesReceive.getServerTransactionSignatures().size() > 0);

                assertEquals(signaturesReceive.getServerTransactionSignatures().size(), responseSend.getClientTransactionSignatures().size());

                PaymentFinalizeStep finalize = new PaymentFinalizeStep(
                        paymentRequestUri,
                        responseSend.getTransaction(),
                        responseSend.getClientTransactionSignatures(),
                        signaturesReceive.getServerTransactionSignatures(),
                        walletServiceBinder
                );
                finalize.process(null);
                assertNotNull(finalize.getTransaction());

                paymentsDone.incrementAndGet();

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                // continue test
                totalPaymentsCountDown.countDown();
                paymentDoneCounDown.countDown();
            }

        }
    }


    private void sleep(double seconds) {
        try {
            Thread.sleep((long)seconds*1000L);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testInit() {
        Log.d(TAG, "started.");
        try {
            Intent serviceIntent = new Intent(InstrumentationRegistry.getTargetContext(), WalletService.class);
            IntentFilter filter = new IntentFilter(Constants.WALLET_DOWNLOAD_DONE_ACTION);
            LocalBroadcastManager
                    .getInstance(InstrumentationRegistry.getTargetContext())
                    .registerReceiver(walletBalanceChangeBroadcastReceiver, filter);

            walletServiceBinder = (WalletService.WalletServiceBinder) serviceRule.bindService(serviceIntent);

            // wait for payment to complete.
            totalPaymentsCountDown.await(
                    NUMBER_OF_PAYMENTS * (PAYMENTS_INTERVAL_SECONDS + 5),
                    TimeUnit.SECONDS);

            int done = paymentsDone.get();
            Log.d(TAG, "Payments done: " + done);
            assertThat(done, is(NUMBER_OF_PAYMENTS));
        } catch (Exception e) {
            e.printStackTrace();
            fail("failed");
        }

    }
}
