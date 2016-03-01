package ch.papers.payments.communications.messages;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 01/03/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class DERParser {
    /*
    Taken from https://en.wikipedia.org/wiki/X.690
    ----------------------------------------------
    EOC (End-of-Content)	0
    BOOLEAN	1
    INTEGER	2
    BIT STRING	3
    OCTET STRING	4
    NULL	5
    OBJECT IDENTIFIER	6
    Object Descriptor	7
    EXTERNAL	8
    REAL (float)	9
    ENUMERATED	10
    EMBEDDED PDV	11
    UTF8String	12
    RELATIVE-OID	13
    (reserved)	14
    (reserved)	15
    SEQUENCE and SEQUENCE OF	16
    SET and SET OF	17
    NumericString	18
    PrintableString	19
    T61String	20
    VideotexString	21
    IA5String	22
    UTCTime	23
    GeneralizedTime	24
    GraphicString	25
    VisibleString	26
    GeneralString	27
    UniversalString	28
    CHARACTER STRING	29
    BMPString	30
    (use long-form)	31
     */
    public static <T extends DERObject> T parseDER(byte[] derPayload) {
        int type = parseDERType(derPayload[0]);
        switch (type) {
            case 2:
                return (T) new DERInteger(extractDERPayload(derPayload));
            case 12:
            case 22:
                return (T) new DERString(extractDERPayload(derPayload));
            case 16:
                return (T) new DERSequence(extractDERPayload(derPayload));
            default:
                return (T) new DERObject(extractDERPayload(derPayload));
        }
    }

    public static int extractPayloadEndIndex(byte[] derPayload) {
        BigInteger payloadLength = null;
        int lengthBytes = extractPayloadStartIndex(derPayload);
        if (lengthBytes > 2) {
            byte[] lengthArray = Arrays.copyOfRange(derPayload, 1, lengthBytes);
            lengthArray[0] = 0;
            payloadLength = new BigInteger(lengthArray);
        } else {
            payloadLength = new BigInteger(new byte[]{0, derPayload[1]});
        }
        return lengthBytes + payloadLength.intValue();
    }

    public static byte[] extractDERPayload(byte[] derPayload) {
        return Arrays.copyOfRange(derPayload, extractPayloadStartIndex(derPayload), extractPayloadEndIndex(derPayload));
    }

    public static int extractPayloadStartIndex(byte[] derPayload) {
        BigInteger lengthBytes = new BigInteger(new byte[]{0, derPayload[1]}).testBit(7) ? new BigInteger(new byte[]{0, derPayload[1]}).clearBit(7) : new BigInteger(new byte[]{0});
        return 2 + lengthBytes.intValue();
    }

    private static int parseDERType(byte b) {
        int mask = 0b11111;
        return b & mask;
    }
    //http://crypto.stackexchange.com/questions/1795/how-can-i-convert-a-der-ecdsa-signature-to-asn-1
}
