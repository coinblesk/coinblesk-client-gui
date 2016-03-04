package ch.papers.payments.communications.messages;

import org.bitcoinj.core.ECKey;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

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

    @Test
    public void nullDERTest() throws Exception {

        DERObject nullDER = new DERObject(new byte[0]);
        DERObject nullDERReparsed = DERParser.parseDER(nullDER.serializeToDER());

        Assert.assertArrayEquals(nullDER.getPayload(),nullDERReparsed.getPayload());
        Assert.assertEquals(nullDER.serializeToDER().length,2);
    }

    @Test
    public void longDERTest() throws Exception {

        byte[] payload = new byte[1024];
        Arrays.fill(payload,(byte) 1);

        DERObject payloadObject = new DERObject(payload);
        Assert.assertEquals(DERParser.extractPayloadEndIndex(payloadObject.serializeToDER()),payloadObject.serializeToDER().length);
    }

}
