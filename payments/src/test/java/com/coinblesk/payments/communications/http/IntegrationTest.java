package com.coinblesk.payments.communications.http;

import com.coinblesk.json.KeyTO;
import com.coinblesk.client.config.Constants;
import com.coinblesk.client.models.ECKeyWrapper;
import com.coinblesk.util.SerializeUtils;

import junit.framework.Assert;

import org.bitcoinj.core.ECKey;

import java.io.File;

import ch.papers.objectstorage.UuidObjectStorage;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 08/03/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class IntegrationTest {

    private final Retrofit retrofit = new Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create(SerializeUtils.GSON))
            .baseUrl(Constants.COINBLESK_SERVER_BASE_URL)
            .build();
    private final CoinbleskWebService service = retrofit.create(CoinbleskWebService.class);


    public void setupMultiSigAddress() throws Exception {
        UuidObjectStorage.getInstance().init(new File("./"));
        final ECKey clientMultiSigKey = new ECKey();
        final KeyTO clientKey = new KeyTO();
        clientKey.publicKey(clientMultiSigKey.getPubKey());
        Response<KeyTO> response = service.keyExchange(clientKey).execute();
        Assert.assertEquals(true,response.isSuccess());
        if (response.isSuccess()) {
            final KeyTO serverKey = response.body();
            final ECKey serverMultiSigKey = ECKey.fromPublicOnly(serverKey.publicKey());
            UuidObjectStorage.getInstance().addEntry(new ECKeyWrapper(clientMultiSigKey.getPrivKeyBytes(), Constants.MULTISIG_CLIENT_KEY_NAME), ECKeyWrapper.class);
            UuidObjectStorage.getInstance().addEntry(new ECKeyWrapper(serverMultiSigKey.getPubKey(), Constants.MULTISIG_SERVER_KEY_NAME, true), ECKeyWrapper.class);
            UuidObjectStorage.getInstance().commit();
        }
    }
}
