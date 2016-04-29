package com.coinblesk.payments.communications.peers.nfc;


import java.util.Arrays;

class NFCUtils {
    public static final byte[] KEEPALIVE = {1, 2, 3, 4};
    public static final byte[] FINALACK = {4, 3, 2, 1};
    public static final byte[] AID_ANDROID = {(byte) 0xF0, 0x0C, 0x01, 0x04, 0x0B, 0x01, 0x03};
    public static final byte[] AID_ANDROID_ACS = {(byte) 0xF0, 0x0C, 0x01, 0x04, 0x0B, 0x01, 0x04};
    public static final byte[] CLA_INS_P1_P2 = {(byte) 0x00, (byte) 0xA4, (byte) 0x04, (byte) 0x00};

    public static final int DEFAULT_MAX_FRAGMENT_SIZE = 245;

    public static boolean isKeepAlive(byte[] payload) {
        boolean keepAlive = Arrays.equals(payload, KEEPALIVE);
        return keepAlive;
    }

    public static boolean isFinalAck(byte[] payload) {
        boolean isFinalAck = Arrays.equals(payload, FINALACK);
        return isFinalAck;
    }

    public static int maxFragmentSizeByAid(byte[] aid) {
        if (Arrays.equals(aid, AID_ANDROID)) {
            return 245;
        } else if (Arrays.equals(aid, AID_ANDROID_ACS)) {
            return 53;
        }
        return DEFAULT_MAX_FRAGMENT_SIZE;
    }

    public static boolean selectAidApdu(byte[] data) {
        return (data.length >= 2) && (data[0] == (byte) 0) && (data[1] == (byte) 0xa4);
    }

    public static byte[] createSelectAidApdu(byte[] data) {
        byte[] result = new byte[6 + data.length];
        System.arraycopy(CLA_INS_P1_P2, 0, result, 0, CLA_INS_P1_P2.length);
        result[4] = (byte) data.length;
        System.arraycopy(data, 0, result, 5, data.length);
        result[result.length - 1] = 0;
        return result;
    }

}
