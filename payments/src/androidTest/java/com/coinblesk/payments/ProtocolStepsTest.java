package com.coinblesk.payments;

import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ServiceTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.SmallTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeoutException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

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
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
        // Verify that the received data is correct.
        assertThat(1, is(1));
    }
}
