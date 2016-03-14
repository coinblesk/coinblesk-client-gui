package com.coinblesk.payments.communications.peers.handlers;

import android.util.Log;

import com.coinblesk.payments.Constants;
import com.coinblesk.payments.Utils;
import com.coinblesk.payments.communications.messages.DERObject;
import com.coinblesk.payments.communications.messages.DERParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 04/03/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public abstract class DERObjectStreamHandler implements Runnable {
    private final static String TAG = DHKeyExchangeHandler.class.getSimpleName();

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
            Log.d(TAG,"reading raw bytes:"+bytesReadCounter);
            int totalBytesRead = bytesReadCounter;
            byte[] requestPayload =  Arrays.copyOfRange(buffer,0,bytesReadCounter);
            final int endIndex = DERParser.extractPayloadEndIndex(requestPayload);

            while (totalBytesRead < endIndex && (bytesReadCounter = inputStream.read(buffer)) > 0) {
                Log.d(TAG,"next der object");
                requestPayload = Utils.concatBytes(requestPayload,Arrays.copyOfRange(buffer,0,bytesReadCounter));
                totalBytesRead+=bytesReadCounter;
            }
            Log.d(TAG,"reading bytes:"+requestPayload.length);
            return DERParser.parseDER(requestPayload);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return DERObject.NULLOBJECT;
    }

    public void writeDERObject(DERObject derObject){
        try {
            Log.d(TAG,"writing bytes:"+derObject.serializeToDER().length);
            outputStream.write(derObject.serializeToDER());
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
