package ch.papers.payments;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 28/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class Utils {

    public static byte[] concatBytes(byte[]... byteArrays){
        int totalLength = 0;
        for (byte[] byteArray:byteArrays) {
            totalLength+=byteArray.length;
        }

        final byte[] concatBuffer = new byte[totalLength];
        int copyCounter = 0;
        for (byte[] byteArray:byteArrays) {
            System.arraycopy(concatBuffer, 0, byteArray, copyCounter, byteArray.length);
            copyCounter += byteArray.length;
        }

        return concatBuffer;
    }
}
