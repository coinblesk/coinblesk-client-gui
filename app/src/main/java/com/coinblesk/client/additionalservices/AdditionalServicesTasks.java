package com.coinblesk.client.additionalservices;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;

import com.coinblesk.client.utils.ClientUtils;
import com.coinblesk.json.v1.BaseTO;
import com.coinblesk.json.v1.UserAccountStatusTO;
import com.coinblesk.json.v1.UserAccountTO;
import com.coinblesk.client.CoinbleskWebService;
import com.coinblesk.util.Pair;

import org.bitcoinj.core.ECKey;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import okhttp3.Interceptor;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

/**
 * Created by draft on 25.06.16.
 */
public class AdditionalServicesTasks {

    public static class GetAccountTask extends AsyncTask<Void, Void, Boolean> {

        private final Context context;

        public GetAccountTask(Context context) {
            this.context = context;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            CoinbleskWebService coinbleskWebService = AdditionalServiceUtils.coinbleskService();
            Call<UserAccountTO> res = coinbleskWebService.getAccount();
            Intent i = new Intent(AdditionalServicesAdapter.BROADCAST_UI);
            try {
                Response<UserAccountTO> resp = res.execute();

                if(resp.isSuccessful()) {
                    UserAccountTO to = resp.body();
                    i.putExtra("", to);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(i);
                } else {
                    LocalBroadcastManager.getInstance(context).sendBroadcast(i);
                }
            } catch (IOException e) {
                LocalBroadcastManager.getInstance(context).sendBroadcast(i);
            }
            return true;
        }
    }

    public static class TransferP2SHTask extends AsyncTask<Void, Void, Boolean> {

        private final Context context;
        private final ECKey clientKey;
        public TransferP2SHTask(ECKey clientKey, Context context) {
            this.clientKey = clientKey;
            this.context = context;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            CoinbleskWebService coinbleskWebService = AdditionalServiceUtils.coinbleskService();
            BaseTO baseTO = new BaseTO()
                    .currentDate(System.currentTimeMillis())
                    .publicKey(clientKey.getPubKey());
            Call<UserAccountTO> res = coinbleskWebService.transferToP2SH(baseTO);
            Intent i = new Intent(AdditionalServicesAdapter.BROADCAST_UI);
            try {
                UserAccountTO to = res.execute().body();
                if (to.isSuccess()) {
                    i.putExtra("", to);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(i);

                } else {
                    LocalBroadcastManager.getInstance(context).sendBroadcast(i);
                }
            } catch (IOException e) {
                LocalBroadcastManager.getInstance(context).sendBroadcast(i);
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
                        .currentDate(System.currentTimeMillis())
                        .email(pair.element0())
                        .password(pair.element1());
                Call<UserAccountStatusTO> result = coinbleskService.signUp(to);
                try {
                    Response<UserAccountStatusTO> res =result.execute();
                    if(res.body().isSuccess()) {
                        callback.onTaskCompleted(true, null);
                    } else {
                        String msg = ClientUtils.getMessageByType(activity, res.body());
                        callback.onTaskCompleted(false, msg);
                    }

                } catch (IOException e) {
                    callback.onTaskCompleted(false, e.getMessage());
                    e.printStackTrace();
                }
            }
            return true;
        }
    }

    public static class ForgotTask extends AsyncTask<String, Void, Boolean> {

        final private Activity activity;
        final private OnTaskCompleted callback;
        public ForgotTask(Activity activity, OnTaskCompleted callback) {
            this.activity = activity;
            this.callback = callback;
        }

        @Override
        protected Boolean doInBackground(String... emails) {
            CoinbleskWebService coinbleskService = AdditionalServiceUtils.coinbleskService();
            for (String email : emails) {
                Call<UserAccountStatusTO> result = coinbleskService.forgot(email);
                try {
                    Response<UserAccountStatusTO> res = result.execute();
                    if (res.body().isSuccess()) {
                        callback.onTaskCompleted(true, null);
                    } else {
                        String msg = ClientUtils.getMessageByType(activity, res.body());
                        callback.onTaskCompleted(false, msg);
                    }

                } catch (IOException e) {
                    callback.onTaskCompleted(false, e.getMessage());
                    e.printStackTrace();
                }
            }
            return true;
        }
    }

    public static class ChangePassword extends AsyncTask<Pair<String,String>, Void, Boolean> {

        final private Activity activity;
        final private OnTaskCompleted callback;
        public ChangePassword(Activity activity, OnTaskCompleted callback) {
            this.activity = activity;
            this.callback = callback;
        }

        @Override
        protected Boolean doInBackground(Pair<String,String>... pairs) {
            CoinbleskWebService coinbleskService = AdditionalServiceUtils.coinbleskService();
            for(Pair<String,String> pair:pairs) {
                UserAccountTO to = new UserAccountTO()
                        .currentDate(System.currentTimeMillis())
                        .email(pair.element0())
                        .password(pair.element1());
                Call<UserAccountStatusTO> result = coinbleskService.changePassword(to);
                try {
                    Response<UserAccountStatusTO> res =result.execute();
                    if(res.body().isSuccess()) {
                        callback.onTaskCompleted(true, null);
                    } else {
                        String msg = ClientUtils.getMessageByType(activity, res.body());
                        callback.onTaskCompleted(false, msg);
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
