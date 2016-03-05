package ch.papers.payments;

import org.bitcoinj.uri.BitcoinURI;

import java.util.Arrays;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 28/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class Utils {

    public static String bitcoinUriToString(BitcoinURI bitcoinURI){
        return BitcoinURI.convertToBitcoinURI(bitcoinURI.getAddress(), bitcoinURI.getAmount(), bitcoinURI.getLabel(), bitcoinURI.getMessage());
    }

    public static byte[] trim(byte[] byteArray){
        int zeroCounter=0;
        for (zeroCounter=0; zeroCounter<byteArray.length; zeroCounter++){
            if(byteArray[zeroCounter]!=0){
                break;
            }
        }

        return Arrays.copyOfRange(byteArray,zeroCounter,byteArray.length);
    }

    public static byte[] concatBytes(byte[]... byteArrays){
        int totalLength = 0;
        for (byte[] byteArray:byteArrays) {
            if(byteArray!=null) {
                totalLength += byteArray.length;
            }
        }

        final byte[] concatBuffer = new byte[totalLength];
        int copyCounter = 0;
        for (byte[] byteArray:byteArrays) {
            if(byteArray!=null) {
                System.arraycopy(byteArray, 0, concatBuffer, copyCounter, byteArray.length);
                copyCounter += byteArray.length;
            }
        }

        return concatBuffer;
    }
}
