package com.coinblesk.client.additionalservices;

import android.app.Activity;
import android.os.AsyncTask;

import com.coinblesk.json.BaseTO;
import com.coinblesk.json.UserAccountStatusTO;
import com.coinblesk.json.UserAccountTO;
import com.coinblesk.client.CoinbleskWebService;
import com.coinblesk.util.Pair;

import org.bitcoinj.core.ECKey;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

/**
 * Created by draft on 25.06.16.
 */
public class AdditionalServicesTasks {

    public static class GetAccountTask extends AsyncTask<Void, Void, Boolean> {

        private final AdditionalServicesActivity.AdditionalServiceGUIState listener;

        public GetAccountTask(AdditionalServicesActivity.AdditionalServiceGUIState listener) {
            this.listener = listener;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            CoinbleskWebService coinbleskWebService = AdditionalServiceUtils.coinbleskService();
            Call<UserAccountTO> res = coinbleskWebService.getAccount();
            try {
                Response<UserAccountTO> resp = res.execute();
                if(resp.isSuccessful()) {
                    UserAccountTO to = resp.body();
                    listener.userAccountTO(to);
                } else {
                    listener.userAccountTO(null);
                }
            } catch (IOException e) {
                listener.userAccountTO(null);
            }
            return true;
        }
    }

    public static class TransferP2SHTask extends AsyncTask<Void, Void, Boolean> {

        private final AdditionalServicesActivity.AdditionalServiceGUIState listener;
        private final ECKey clientKey;
        public TransferP2SHTask(ECKey clientKey, AdditionalServicesActivity.AdditionalServiceGUIState listener) {
            this.clientKey = clientKey;
            this.listener = listener;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            CoinbleskWebService coinbleskWebService = AdditionalServiceUtils.coinbleskService();
            BaseTO b = new BaseTO();
            b.publicKey(clientKey.getPubKey());
            Call<UserAccountTO> res = coinbleskWebService.transferToP2SH(b);

            try {
                UserAccountTO to = res.execute().body();
                if (to.isSuccess()) {
                    listener.userAccountTO(to);

                } else {
                    listener.userAccountTO(null);
                }
            } catch (IOException e) {
                listener.userAccountTO(null);
            }
            return true;
        }
    }

    public interface OnTaskCompleted{
        void onTaskCompleted(boolean success, String message);
    }

    public static class SignupTask extends AsyncTask<Pair<String,String>, Void, Boolean> {

        final private Activity activity;
        final private OnTaskCompleted callback;
        public SignupTask(Activity activity, OnTaskCompleted callback) {
            this.activity = activity;
            this.callback = callback;
        }

        @Override
        protected Boolean doInBackground(Pair<String,String>... pairs) {
            CoinbleskWebService coinbleskService = AdditionalServiceUtils.coinbleskService();
            for(Pair<String,String> pair:pairs) {
                UserAccountTO to = new UserAccountTO()
                        .email(pair.element0())
                        .password(pair.element1());
                Call<UserAccountStatusTO> result = coinbleskService.signUp(to);
                try {
                    Response<UserAccountStatusTO> res =result.execute();
                    if(res.body().isSuccess()) {
                        callback.onTaskCompleted(true, null);
                    } else {
                        callback.onTaskCompleted(false, res.body().message());
                    }

                } catch (IOException e) {
                    callback.onTaskCompleted(false, e.getMessage());
                    e.printStackTrace();
                }
            }
            return true;
        }
    }

    public static class LogoutTask extends AsyncTask<Void, Void, Boolean> {

        final private Activity activity;
        final private OnTaskCompleted callback;
        public LogoutTask(Activity activity, OnTaskCompleted callback) {
            this.activity = activity;
            this.callback = callback;
        }

        @Override
        protected Boolean doInBackground(Void... pairs) {
            CoinbleskWebService coinbleskService = AdditionalServiceUtils.coinbleskService();

            Call<UserAccountStatusTO> result = coinbleskService.logout();
            try {
                Response<UserAccountStatusTO> res =result.execute();
                if(res.body().isSuccess()) {
                    AdditionalServiceUtils.setSessionID(activity, "");
                    callback.onTaskCompleted(true, null);
                } else {
                    callback.onTaskCompleted(false, res.body().message());
                }

            } catch (IOException e) {
                callback.onTaskCompleted(false, e.getMessage());
                e.printStackTrace();
            }

            return true;
        }
    }

    public static class LoginTask extends AsyncTask<Pair<String,String>, Void, Boolean> {

        final private Activity activity;
        final private OnTaskCompleted callback;
        public LoginTask(Activity activity, OnTaskCompleted callback) {
            this.activity = activity;
            this.callback = callback;
        }

        @Override
        protected Boolean doInBackground(Pair<String,String>... pairs) {
            CoinbleskWebService coinbleskService = AdditionalServiceUtils.coinbleskService();
            for(Pair<String,String> pair:pairs) {
                Call<ResponseBody> response = coinbleskService.login(pair.element0(), pair.element1());
                try {
                    Response<ResponseBody> res = response.execute();
                    if(res.isSuccessful()) {
                        String rawCookie = res.headers().get("Set-Cookie");
                        String cookie = AdditionalServiceUtils.parseCookie(rawCookie);
                        AdditionalServiceUtils.setSessionID(activity, cookie);
                        callback.onTaskCompleted(true, null);
                    } else {
                        callback.onTaskCompleted(false, null);
                    }
                } catch (IOException e) {
                    callback.onTaskCompleted(false, e.getMessage());
                    e.printStackTrace();
                }
            }
            return true;
        }
    }


}
