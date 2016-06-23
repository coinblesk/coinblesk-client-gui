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
import java.util.concurrent.atomic.AtomicReference;

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
        add("Transfer"); //dummy entry to get the size right
        this.walletServiceBinder = walletServiceBinder;
        this.activity = activity;
    }

    private CheckBox checkBox;
    private UserAccountTO userAccountTO;
    private TextView balance;

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
                    checkBox = (CheckBox) convertView.findViewById(R.id.checkBox);
                    checkBox.setChecked(false);
                    checkBox.setEnabled(true);
                    updateState();
                    new GetAccountTask().execute();
                    convertView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            showAddAddressDialog();
                        }
                    });

                }

                return convertView;
            case 1:
                if (convertView == null) {
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.additional_services_item2, parent, false);
                    balance = (TextView) convertView.findViewById(R.id.additional_services_balance);
                    updateState();
                    convertView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            new TransferP2SHTask().execute();
                        }
                    });

                }
                return convertView;
            default:
                throw new RuntimeException("expected a known position");
        }
    }

    private void showAddAddressDialog() {
        AdditionalServicesUsernameDialog frag = new AdditionalServicesUsernameDialog();
        frag.setCloseRunner(new Runnable(){
            @Override
            public void run() {
                new GetAccountTask().execute();
            }
        });
        frag.show(((Activity) getContext()).getFragmentManager(), TAG);

    }

    private class TransferP2SHTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {
            CoinbleskWebService coinbleskWebService = AdditionalServiceUtils.coinbleskService();
            BaseTO b = new BaseTO();
            b.publicKey(walletServiceBinder.getMultisigClientKey().getPubKey());
            Call<UserAccountTO> res = coinbleskWebService.transferToP2SH(b);

                try {
                    UserAccountTO to = res.execute().body();
                    if (to.isSuccess()) {
                        userAccountTO = to;
                    } else {
                        userAccountTO = null;
                    }
                } catch (IOException e) {
                    userAccountTO = null;
                }
                updateState();

            return true;
        }
    }

    private void updateState() {
        activity.runOnUiThread(new Runnable() {
            public void run() {
                if(userAccountTO != null && checkBox != null) {
                    checkBox.setEnabled(true);
                    checkBox.setEnabled(userAccountTO.isSuccess());
                } else if (checkBox != null){
                    checkBox.setEnabled(false);
                    checkBox.setChecked(false);
                }

                if(userAccountTO != null && balance != null) {
                    balance.setText(Long.toString(userAccountTO.balance()));
                } else if (balance != null){
                    balance.setText("no balance");
                }
            }
        });
    }



    private class GetAccountTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... params) {


                CoinbleskWebService coinbleskWebService = AdditionalServiceUtils.coinbleskService();
                Call<UserAccountTO> res = coinbleskWebService.getAccount();
                try {
                    Response<UserAccountTO> resp = res.execute();
                    if(resp.isSuccessful()) {
                        UserAccountTO to = resp.body();
                        userAccountTO = to;
                    } else {
                        userAccountTO = null;
                    }
                } catch (IOException e) {
                    userAccountTO = null;
                }
                updateState();

            return true;
        }
    }
}