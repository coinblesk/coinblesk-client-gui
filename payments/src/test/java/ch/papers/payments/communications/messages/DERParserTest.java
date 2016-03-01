package ch.papers.payments.communications.messages;

import org.bitcoinj.core.ECKey;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 01/03/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class DERParserTest {

    @Test
    public void derParseTest() throws Exception {
        ECKey ecKey = new ECKey();
        DERSequence derSequence = DERParser.parseDER(ecKey.toASN1());

        Assert.assertEquals(derSequence.getChildren().size(),4);

        DERSequence derSequence1 = new DERSequence(derSequence.getChildren());
        for(int i=0; i<4; i++) {
            Assert.assertArrayEquals(derSequence.getChildren().get(i).getPayload(), derSequence1.getChildren().get(i).getPayload());
        }
    }

}
