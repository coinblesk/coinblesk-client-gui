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

package com.coinblesk.client.utils;

import com.google.common.primitives.UnsignedBytes;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.uri.BitcoinURI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;



public final class ClientUtils {
    private static final Logger log = LoggerFactory.getLogger(ClientUtils.class);

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

    public static void fixECKeyComparator() {
        final Comparator<ECKey> ecKeyComparator = new Comparator<ECKey>() {
            private Comparator<byte[]> comparator = PureJavaComparator.INSTANCE;

            @Override
            public int compare(ECKey k1, ECKey k2) {
                return comparator.compare(k1.getPubKey(), k2.getPubKey());
            }
        };

        try {
            setFinalStatic(ECKey.class.getField("PUBKEY_COMPARATOR"),ecKeyComparator);
            log.debug("ECKeyComparator fix successful");
        } catch (Exception e) {
            log.warn("Error during ECKeyComparator fix: " + e.getMessage());
        }
    }

    private static void setFinalStatic(Field field, Object newValue) throws Exception {
        field.setAccessible(true);

        // remove final modifier from field
        try {
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        } catch (Exception e) {
            log.warn("Could not remove 'final' modifier: " + e.getMessage());
        }
        field.set(null, newValue);
    }

    public static String bitcoinUriToString(BitcoinURI bitcoinURI) {
        return BitcoinURI.convertToBitcoinURI(
                bitcoinURI.getAddress(),
                bitcoinURI.getAmount(),
                bitcoinURI.getLabel(),
                bitcoinURI.getMessage());
    }

    public static byte[] trimLeadingZeros(byte[] byteArray){
        int zeroCounter;
        for (zeroCounter = 0; zeroCounter < byteArray.length; zeroCounter++) {
            if (byteArray[zeroCounter] != 0){
                break;
            }
        }
        return Arrays.copyOfRange(byteArray, zeroCounter, byteArray.length);
    }

    public static byte[] concatBytes(byte[]... byteArrays) {
        int totalLength = 0;
        for (byte[] byteArray : byteArrays) {
            if (byteArray != null) {
                totalLength += byteArray.length;
            }
        }

        final byte[] concatBuffer = new byte[totalLength];
        int copyCounter = 0;
        for (byte[] byteArray : byteArrays) {
            if (byteArray != null) {
                System.arraycopy(byteArray, 0, concatBuffer, copyCounter, byteArray.length);
                copyCounter += byteArray.length;
            }
        }
        return concatBuffer;
    }


    public static boolean isMainNet(NetworkParameters params) {
        return params.getId().equals(NetworkParameters.ID_MAINNET);
    }

    public static boolean isTestNet(NetworkParameters params) {
        return params.getId().equals(NetworkParameters.ID_TESTNET);
    }
}
