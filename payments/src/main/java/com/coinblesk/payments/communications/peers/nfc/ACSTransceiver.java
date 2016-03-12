package com.coinblesk.payments.communications.peers.nfc;

import android.util.Log;

import com.acs.smartcard.Reader;
import com.acs.smartcard.ReaderException;

import java.io.IOException;
import java.util.Arrays;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 12/03/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class ACSTransceiver {
    final private static String TAG = ACSTransceiver.class.getSimpleName();
    final private Reader reader;
    final private int maxLen;
    final private boolean acr122u;

    public ACSTransceiver(Reader reader, final int maxLen, boolean acr122u) {
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
        byte[] recvBuffer = new byte[8];
        int length = reader.transmit(0, sendBuffer, sendBuffer.length, recvBuffer, recvBuffer.length);
        if (length != 8) {
            throw new RuntimeException("unexcpeted number of bytes");
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

    public void initCard(final int slotNum) throws ReaderException {
        reader.power(slotNum, Reader.CARD_WARM_RESET);
        reader.setProtocol(slotNum, Reader.PROTOCOL_T0 | Reader.PROTOCOL_T1);
    }

    public byte[] write(byte[] input) throws Exception {
        if (!reader.isOpened()) {
            Log.d(TAG, "could not write message, reader is not or no longe open");
            throw new IOException("NFCTRANSCEIVER_NOT_CONNECTED");
        }

        if (input.length > maxLen) {
            throw new IOException("The message length exceeds the maximum capacity of " + maxLen + " bytes.");
        }

        final byte[] recvBuffer = new byte[maxLen];
        final int length;
        try {
            Log.d(TAG, "write bytes: " + Arrays.toString(input));
            length = reader.transmit(0, input, input.length, recvBuffer, recvBuffer.length);
        } catch (ReaderException e) {
            Log.d(TAG, "could not write message - ReaderException", e);
            throw new IOException("UNEXPECTED_ERROR");
        }

        if (length <= 0) {
            Log.d(TAG, "could not write message - return value is 0");
            //most likely due to tag lost
            return null;
        }

        byte[] received = new byte[length];
        System.arraycopy(recvBuffer, 0, received, 0, length);
        return received;
    }
}
