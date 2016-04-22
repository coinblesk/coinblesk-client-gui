package com.coinblesk.payments;
// needed as long as server is not working properly

import com.coinblesk.json.KeyTO;
import com.coinblesk.payments.communications.http.CoinbleskWebService;
import com.coinblesk.payments.models.User;
import com.google.gson.Gson;

import junit.framework.Assert;

import org.junit.Test;

import java.io.IOException;

import retrofit2.Response;

import static org.junit.Assert.assertArrayEquals;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 06/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class ServerCommunicationUnitTest {

    @Test
    public void keyExchange() throws IOException {
        User testUser = new User();

        CoinbleskWebService service = Constants.RETROFIT.create(CoinbleskWebService.class);

        KeyTO clientKey = new KeyTO();
        clientKey.publicKey(testUser.getEcKey().getPubKey());

        System.out.println(new Gson().toJson(clientKey));

        Response<KeyTO> serverKey = service.keyExchange(clientKey).execute();
        Assert.assertTrue(serverKey.isSuccess());
        Assert.assertTrue(serverKey.body().isSuccess());

        byte[] publicServerKey = serverKey.body().publicKey();
        serverKey = service.keyExchange(clientKey).execute();
        Assert.assertTrue(serverKey.isSuccess());
        Assert.assertTrue(serverKey.body().isSuccess());

        assertArrayEquals(serverKey.body().publicKey(),publicServerKey);


    }

}
