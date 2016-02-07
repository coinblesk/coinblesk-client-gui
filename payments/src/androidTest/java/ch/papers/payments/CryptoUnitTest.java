package ch.papers.payments;

import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.SmallTest;

import org.bitcoinj.core.ECKey;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 06/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */

// this needs to run on android device because the spongycastle does not work on jvm
@RunWith(AndroidJUnit4.class)
@SmallTest
public class CryptoUnitTest {
    @Test
    public void dhKeyExchange(){
        ECKey clientAKey = new ECKey();
        ECKey clientAPubKey = ECKey.fromPublicOnly(clientAKey.getPubKey());
        ECKey clientBKey = new ECKey();
        ECKey clientBPubKey = ECKey.fromPublicOnly(clientBKey.getPubKey());

        byte[] secretA = Crypto.generateCommonSecret(clientAKey,clientBPubKey);
        byte[] secretB = Crypto.generateCommonSecret(clientBKey,clientAPubKey);

        Assert.assertArrayEquals(secretA,secretB);
    }
}
