package ch.papers.payments;
// needed as long as server is not working properly

import com.coinblesk.json.KeyTO;

import junit.framework.Assert;

import org.apache.commons.codec.binary.Base64;
import org.junit.Test;

import java.io.IOException;

import ch.papers.payments.communications.http.CoinbleskWebService;
import ch.papers.payments.models.User;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 06/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class ServerCommunicationUnitTest {

    @Test
    public void keyExchange() throws IOException {
        User testUser = new User();

        Retrofit retrofit = new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(Constants.COINBLESK_SERVER_BASE_URL)
                .build();


        CoinbleskWebService service = retrofit.create(CoinbleskWebService.class);

        KeyTO clientKey = new KeyTO();
        clientKey.publicKey(Base64.encodeBase64String(testUser.getEcKey().getPubKey()));

        Response<KeyTO> serverKey = service.keyExchange(clientKey).execute();
        Assert.assertTrue(serverKey.body().isSuccess());

    }

}
