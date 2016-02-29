package ch.papers.payments.communications.http;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 22/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */

import com.coinblesk.json.CompleteSignTO;
import com.coinblesk.json.KeyTO;
import com.coinblesk.json.PrepareHalfSignTO;
import com.coinblesk.json.RefundTO;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface CoinbleskWebService {
    @POST("payment/key-exchange")
    Call<KeyTO> keyExchange(@Body KeyTO keyTO);

    @POST("payment/prepare")
    Call<PrepareHalfSignTO> prepareHalfSign(@Body PrepareHalfSignTO prepareSignTO);

    @POST("payment/refund")
    Call<RefundTO> refund(@Body RefundTO refundTO);

    @POST("payment/complete-sign")
    Call<CompleteSignTO> sign(@Body CompleteSignTO signTO);
}
