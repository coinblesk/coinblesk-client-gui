package com.coinblesk.payments.communications;


import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 05/03/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class CipherTest {

    @Test
    public void streamCipherTest() throws Exception {
        final byte[] iv = new byte[16];
        Arrays.fill(iv, (byte) 0x00);
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

        SecretKeySpec secretKey = new SecretKeySpec(iv, "AES");

        final Cipher writeCipher = Cipher.getInstance("AES/CFB8/NoPadding");
        writeCipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec);

        final Cipher readCipher = Cipher.getInstance("AES/CFB8/NoPadding");
        readCipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(60);

        final OutputStream encrytpedOutputStream = new CipherOutputStream(byteArrayOutputStream, writeCipher);
        Assert.assertEquals(0,byteArrayOutputStream.toByteArray().length);
        encrytpedOutputStream.write(new byte[15]);
        encrytpedOutputStream.flush();
        Assert.assertEquals(15,byteArrayOutputStream.toByteArray().length);

        final InputStream encryptedInputStream = new CipherInputStream(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()),readCipher);
        byte[] buffer = new byte[15];

        encryptedInputStream.read(buffer);
        Assert.assertArrayEquals(new byte[15],buffer);
        Assert.assertNotEquals((byte)0,byteArrayOutputStream.toByteArray());
    }
}
