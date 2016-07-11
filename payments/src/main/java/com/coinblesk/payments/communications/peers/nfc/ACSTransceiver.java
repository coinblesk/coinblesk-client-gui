/*
 * Copyright 2016 The Coinblesk team and the CSG Group at University of Zurich
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.coinblesk.payments.communications.peers.nfc;

import android.util.Log;

import com.acs.smartcard.Reader;
import com.acs.smartcard.ReaderException;

import java.io.IOException;
import java.util.Arrays;

/**
 * @author Alessandro De Carli
 * @author Andreas Albrecht
 */
public class ACSTransceiver {
    private final static String TAG = ACSTransceiver.class.getName();
    private final Reader reader;
    private final int maxLen;
    private final boolean acr122u;

    public ACSTransceiver(Reader reader, int maxLen, boolean acr122u) throws ReaderException {
        this.reader = reader;
        this.maxLen = maxLen;
        this.acr122u = acr122u;
    }

    private void disableBuzzer() throws ReaderException {
        if (!acr122u) {
            return;
        }
        // Disable the standard buzzer when a tag is detected (Section 6.7). It sounds
        // immediately after placing a tag resulting in people lifting the tag off before
        // we've had a chance to read the ID.
        byte[] sendBuffer = {(byte) 0xFF, (byte) 0x00, (byte) 0x52, (byte) 0x00, (byte) 0x00};
        byte[] recvBuffer = new byte[2];
        int length = reader.transmit(0, sendBuffer, sendBuffer.length, recvBuffer, recvBuffer.length);
        if (length != 2) {
            throw new RuntimeException("unexpected number of bytes, length=" + length);
        }
    }

    /*private String firwware() throws ReaderException {
        byte[] sendBuffer={(byte)0xFF, (byte)0x00, (byte)0x48, (byte)0x00, (byte)0x00};
        byte[] recvBuffer=new byte[10];
        int length = reader.transmit(0, sendBuffer, sendBuffer.length, recvBuffer, recvBuffer.length);
        if(length != 10) {
            nfcInit.tagFailed(NfcEvent.INIT_FAILED.name());
        }
        return new String(recvBuffer);
    }*/

    private boolean buzzerDisabled = false;

    public void initCard(final int slotNum) throws ReaderException {
        reader.power(slotNum, Reader.CARD_WARM_RESET);
        reader.setProtocol(slotNum, Reader.PROTOCOL_T0 | Reader.PROTOCOL_T1);
        if(!buzzerDisabled) {
            disableBuzzer();
            buzzerDisabled = true;
        }
    }

    public byte[] write(byte[] input) throws Exception {
        if (!reader.isOpened()) {
            Log.w(TAG, "could not write message, reader is not or no longer open");
            throw new IOException("NFCTRANSCEIVER_NOT_CONNECTED");
        }

        if (input.length > maxLen) {
            throw new IOException(String.format(
                    "The message length exceeds the maximum capacity of %d bytes, length is %d bytes.",
                    maxLen, input.length));
        }

        final byte[] recvBuffer = new byte[maxLen];
        final int length;
        try {
            Log.d(TAG, "write bytes: " + Arrays.toString(input));
            length = reader.transmit(0, input, input.length, recvBuffer, recvBuffer.length);
        } catch (ReaderException e) {
            Log.w(TAG, "could not write message - ReaderException", e);
            throw new IOException("UNEXPECTED_ERROR");
        }

        if (length <= 0) {
            Log.w(TAG, "could not write message - return value is 0");
            // most likely due to tag lost
            return null;
        }

        byte[] received = new byte[length];
        System.arraycopy(recvBuffer, 0, received, 0, length);
        return received;
    }

    public Reader getReader() {
        return reader;
    }
}