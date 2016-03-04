package ch.papers.payments.communications.peers.handlers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import ch.papers.payments.Constants;
import ch.papers.payments.Utils;
import ch.papers.payments.communications.messages.DERObject;
import ch.papers.payments.communications.messages.DERParser;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 04/03/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public abstract class DERObjectStreamHandler implements Runnable {

    private final InputStream inputStream;
    private final OutputStream outputStream;

    public DERObjectStreamHandler(InputStream inputStream, OutputStream outputStream) {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
    }

    public DERObject readDERObject() {
        try {
            byte[] buffer = new byte[Constants.BUFFER_SIZE];

            int bytesReadCounter = inputStream.read(buffer);
            int totalBytesRead = bytesReadCounter;
            byte[] requestPayload =  Arrays.copyOfRange(buffer,0,bytesReadCounter);
            final int endIndex = DERParser.extractPayloadEndIndex(requestPayload);

            while (totalBytesRead < endIndex && (bytesReadCounter = inputStream.read(buffer)) > 0) {
                requestPayload = Utils.concatBytes(requestPayload,Arrays.copyOfRange(buffer,0,bytesReadCounter));
                totalBytesRead+=bytesReadCounter;
            }

            return DERParser.parseDER(requestPayload);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return DERObject.NULLOBJECT;
    }

    public void writeDERObject(DERObject derObject){
        try {
            outputStream.write(derObject.serializeToDER());
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
