package com.coinblesk.client.additionalservices;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.coinblesk.client.R;
import com.coinblesk.client.addresses.EditAddressFragment;
import com.coinblesk.json.BaseTO;
import com.coinblesk.json.UserAccountStatusTO;
import com.coinblesk.json.UserAccountTO;
import com.coinblesk.payments.WalletService;
import com.coinblesk.payments.communications.http.CoinbleskWebService;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Created by draft on 16.06.16.
 */
public class AdditionalServiceAdapter extends ArrayAdapter<String> {

    private final static String TAG = AdditionalServiceAdapter.class.getName();

    final private WalletService.WalletServiceBinder walletServiceBinder;
    final private Activity activity;

    public AdditionalServiceAdapter(Activity activity, WalletService.WalletServiceBinder walletServiceBinder) {
        super(activity, 0);
        add("Login"); //dummy entry to get the size right
        this.walletServiceBinder = walletServiceBinder;
        this.activity = activity;
    }

    @Override
    public View getView(int position, View convertView, final ViewGroup parent) {
        switch (position) {
            case 0:
                if (convertView == null) {
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.additional_services_item, parent, false);
                    //TextView firstLine = (TextView) convertView.findViewById(R.id.firstLine);
                    //firstLine.setText("Login");
                    //TextView secondLine = (TextView) convertView.findViewById(R.id.secondLine);
                    //secondLine.setText("blah blah blah");
                    final CheckBox checkBox = (CheckBox) convertView.findViewById(R.id.checkBox);
                    checkBox.setChecked(false);
                    checkBox.setEnabled(true);
                    new GetAccountTask(activity).execute(checkBox);

                    convertView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            showAddAddressDialog(checkBox);
                        }
                    });

                }

                return convertView;
            case 1:
                if (convertView == null) {
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.additional_services_item2, parent, false);
                    final TextView balance = (TextView) convertView.findViewById(R.id.additional_services_balance);

                    convertView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            new TransferP2SHTask(activity).execute(balance);
                        }
                    });

                }
                return convertView;
            default:
                throw new RuntimeException("expected a known position");
        }
    }



    private void showAddAddressDialog(final CheckBox checkBox) {
        AdditionalServicesUsernameDialog frag = new AdditionalServicesUsernameDialog();
        frag.setCloseRunner(new Runnable(){
            @Override
            public void run() {
                new GetAccountTask(activity).execute(checkBox);
            }
        });
        frag.show(((Activity) getContext()).getFragmentManager(), TAG);

    }

    private class TransferP2SHTask extends AsyncTask<TextView, Void, Boolean> {

        final private Activity activity;
        private TransferP2SHTask(Activity activity) {
            this.activity = activity;
        }

        @Override
        protected Boolean doInBackground(TextView... textViews) {
            CoinbleskWebService coinbleskWebService = AdditionalServiceUtils.coinbleskService(activity);
            BaseTO b = new BaseTO();
            b.publicKey(walletServiceBinder.getMultisigClientKey().getPubKey());
            Call<UserAccountStatusTO> res = coinbleskWebService.transferToP2SH(b);
            for(TextView textView:textViews) {
                try {
                    UserAccountStatusTO to = res.execute().body();
                    if (to.isSuccess()) {
                        updateBalance(textView, activity, "Balance tranfered");
                    } else {
                        updateBalance(textView, activity, "Balance not transfered");
                    }
                } catch (IOException e) {
                    updateBalance(textView, activity, "Balance not transfered");
                }
            }
            return true;
        }

        private void updateBalance(final TextView balance, final Context context, final String text)  {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity, text, Toast.LENGTH_LONG).show();
                    CoinbleskWebService coinbleskWebService = AdditionalServiceUtils.coinbleskService(context);
                    Call<UserAccountTO> res = coinbleskWebService.getAccount();
                    try {
                        UserAccountTO to = res.execute().body();
                        balance.setText(Long.toString(to.balance()));
                    } catch (IOException e) {
                        balance.setText(e.getMessage());
                    }
                }
            });
        }
    }



    private class GetAccountTask extends AsyncTask<CheckBox, Void, Boolean> {

        final private Activity activity;
        private GetAccountTask(Activity activity) {
            this.activity = activity;
        }

        @Override
        protected Boolean doInBackground(CheckBox... checkBoxs) {
            for(CheckBox checkBox:checkBoxs) {
                enable(checkBox, true, null);

                CoinbleskWebService coinbleskWebService = AdditionalServiceUtils.coinbleskService(activity);
                Call<UserAccountTO> res = coinbleskWebService.getAccount();
                try {

                    Response<UserAccountTO> resp = res.execute();
                    if(resp.isSuccessful()) {
                        UserAccountTO to = resp.body();
                        enable(checkBox, null, to.isSuccess());
                    } else {
                        enable(checkBox, null, false);
                    }

                } catch (IOException e) {
                    enable(checkBox, null, false);
                }
            }
            return true;
        }

        private void enable(final CheckBox checkBox, final Boolean enable, final Boolean checked) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    if(enable != null) {
                        checkBox.setEnabled(enable);
                    }
                    if(checked != null) {
                        checkBox.setChecked(checked);
                    }
                }
            });
        }
    }
}