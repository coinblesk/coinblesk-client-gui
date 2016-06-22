package com.coinblesk.client.additionalservices;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
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

/**
 * Created by draft on 16.06.16.
 */
public class AdditionalServiceAdapter extends ArrayAdapter<String> {

    private final static String TAG = AdditionalServiceAdapter.class.getName();

    final Handler handler = new Handler();

    final private WalletService.WalletServiceBinder walletServiceBinder;

    public AdditionalServiceAdapter(Activity activity, WalletService.WalletServiceBinder walletServiceBinder) {
        super(activity, 0);
        add("Login"); //dummy entry to get the size right
        this.walletServiceBinder = walletServiceBinder;
    }

    @Override
    public View getView(int position, View convertView, final ViewGroup parent) {
        switch(position) {
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
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            checkBox.setEnabled(true);
                            CoinbleskWebService coinbleskWebService = AdditionalServiceUtils.coinbleskService(parent.getContext());
                            Call<UserAccountTO> res = coinbleskWebService.getAccount();
                            try {
                                UserAccountTO to = res.execute().body();
                                checkBox.setChecked(to.isSuccess());
                            } catch (IOException e) {
                                checkBox.setChecked(false);
                            }
                        }
                    });


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
                    final TextView balance = (TextView) convertView.findViewById(R.id.additional_services_balance);

                    convertView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            CoinbleskWebService coinbleskWebService = AdditionalServiceUtils.coinbleskService(parent.getContext());
                            BaseTO b = new BaseTO();
                            b.publicKey(walletServiceBinder.getMultisigClientKey().getPubKey());
                            Call<UserAccountStatusTO> res = coinbleskWebService.transferToP2SH(b);
                            try {
                                UserAccountStatusTO to = res.execute().body();
                                if(to.isSuccess()) {
                                    Toast.makeText(parent.getContext(), "Balance transfered", Toast.LENGTH_SHORT).show();
                                    updateBalance(balance, parent);
                                } else {
                                    Toast.makeText(parent.getContext(), "Balance not transfered", Toast.LENGTH_SHORT).show();
                                    updateBalance(balance, parent);
                                }
                            } catch (IOException e) {
                                Toast.makeText(parent.getContext(), "Balance not transfered", Toast.LENGTH_SHORT).show();
                                updateBalance(balance, parent);
                            }
                        }
                    });

                }
                return convertView;
            default: throw new RuntimeException("expected a known position");
        }
    }

    private void updateBalance(final TextView balance, final ViewGroup parent) {
        handler.post(new Runnable() {
            @Override
            public void run() {

                CoinbleskWebService coinbleskWebService = AdditionalServiceUtils.coinbleskService(parent.getContext());
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

    private void showAddAddressDialog() {
        DialogFragment frag = new AdditionalServicesUsernameDialog();
        frag.show(((Activity)getContext()).getFragmentManager(), TAG);
    }
}
