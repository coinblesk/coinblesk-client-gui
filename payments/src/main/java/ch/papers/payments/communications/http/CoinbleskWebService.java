package ch.papers.payments.communications.http;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 22/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */

import com.coinblesk.json.KeyTO;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface CoinbleskWebService {
    @POST("/payment/key-exchange")
    Call<KeyTO> keyExchange(@Body KeyTO keyTO);
}
