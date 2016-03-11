package ch.papers.payments;

import android.util.Log;

import com.google.common.primitives.UnsignedBytes;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.uri.BitcoinURI;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 28/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class Utils {
    private final static String TAG = Utils.class.getName();

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


    enum PureJavaComparator implements Comparator<byte[]> {
        INSTANCE;

        @Override public int compare(byte[] left, byte[] right) {
            int minLength = Math.min(left.length, right.length);
            for (int i = 0; i < minLength; i++) {
                int result = UnsignedBytes.compare(left[i], right[i]);
                if (result != 0) {
                    return result;
                }
            }
            return left.length - right.length;
        }
    }

    public static void fixECKeyComparator(){
        final Comparator<ECKey> ecKeyComparator = new Comparator<ECKey>() {
            private Comparator<byte[]> comparator = PureJavaComparator.INSTANCE;

            @Override
            public int compare(ECKey k1, ECKey k2) {
                return comparator.compare(k1.getPubKey(), k2.getPubKey());
            }
        };

        try {
            setFinalStatic(ECKey.class.getField("PUBKEY_COMPARATOR"),ecKeyComparator);
        } catch (Exception e) {
            Log.d(TAG,"Error during ECKeyComparator fix: " + e.getMessage(), e);
        }
    }

    private static void setFinalStatic(Field field, Object newValue) throws Exception {
        field.setAccessible(true);

        // remove final modifier from field --> This is not required on Android, final modifier does not exist!
        // see: https://stackoverflow.com/questions/11185453/android-changing-private-static-final-field-using-java-reflection
        /*
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        */

        field.set(null, newValue);
    }
}
