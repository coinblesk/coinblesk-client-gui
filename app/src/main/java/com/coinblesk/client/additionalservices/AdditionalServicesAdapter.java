package com.coinblesk.client.additionalservices;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import com.coinblesk.client.R;
import com.coinblesk.json.v1.UserAccountTO;
import com.coinblesk.payments.WalletService;

import org.bitcoinj.core.Coin;

/**
 * Created by draft on 16.06.16.
 */
public class AdditionalServicesAdapter extends ArrayAdapter<String> {

    private final static String TAG = AdditionalServicesAdapter.class.getName();

    final private WalletService.WalletServiceBinder walletServiceBinder;

    public final static String BROADCAST_UI = "AdditionalServicesAdapter";

    private BroadcastReceiver receiverCheckBox1;
    private BroadcastReceiver receiverCheckBox2;

    public AdditionalServicesAdapter(Activity activity, WalletService.WalletServiceBinder walletServiceBinder) {
        super(activity, 0);
        this.walletServiceBinder = walletServiceBinder;

    }

    @Override
    public int getCount(){
        return 2;
    }

    @Override
    public View getView(int position, View convertView, final ViewGroup parent) {
        switch (position) {
            case 0:
                if (convertView == null) {
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.additional_services_item, parent, false);
                    final CheckBox checkBox = (CheckBox) convertView.findViewById(R.id.checkBox);
                    final TextView textView = (TextView) convertView.findViewById(R.id.firstLine);
                    checkBox.setChecked(false);
                    checkBox.setEnabled(true);
                    final Bundle b = new Bundle();
                    receiverCheckBox1 = new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            UserAccountTO userAccountTO = (UserAccountTO) intent.getSerializableExtra("");
                            if (userAccountTO != null) {
                                b.putSerializable("",userAccountTO);
                                checkBox.setEnabled(true);
                                checkBox.setChecked(userAccountTO.isSuccess());
                                textView.setText(R.string.additional_services_titel_logout);
                            } else {
                                checkBox.setEnabled(false);
                                checkBox.setChecked(false);
                                textView.setText(R.string.additional_services_titel);
                            }
                        }
                    };
                    LocalBroadcastManager.getInstance(getContext()).registerReceiver(receiverCheckBox1, new IntentFilter(BROADCAST_UI));
                    convertView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            AdditionalServicesUsernameDialog a = new AdditionalServicesUsernameDialog();
                            a.setArguments(b);
                            a.show(((Activity) getContext()).getFragmentManager(), TAG);
                        }
                    });
                }

                return convertView;
            case 1:
                if (convertView == null) {
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.additional_services_item2, parent, false);
                    final TextView balance = (TextView) convertView.findViewById(R.id.additional_services_balance);

                    receiverCheckBox2 = new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            UserAccountTO userAccountTO = (UserAccountTO) intent.getSerializableExtra("");
                            if (userAccountTO != null && balance != null) {
                                Coin coin = Coin.valueOf(userAccountTO.balance());
                                balance.setText(coin.toFriendlyString());
                            } else if (balance != null) {
                                balance.setText( R.string.additional_services_no_balance);
                            }
                        }
                    };
                    LocalBroadcastManager.getInstance(getContext()).registerReceiver(receiverCheckBox2, new IntentFilter(BROADCAST_UI));

                    convertView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            new AlertDialog.Builder(getContext())
                                    .setMessage(R.string.additional_services_description_2)
                                    .setTitle(R.string.additional_services_description_3)
                                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            new AdditionalServicesTasks.TransferP2SHTask(walletServiceBinder.getMultisigClientKey(), getContext()).execute();
                                        }
                                    }).setNegativeButton(R.string.cancel, null).create().show();
                        }
                    });
                }
                return convertView;
            default:
                throw new RuntimeException("expected a known position");
        }
    }

    public void onStop() {
        if(receiverCheckBox1 != null) {
            LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(receiverCheckBox1);
        }
        if(receiverCheckBox2 != null) {
            LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(receiverCheckBox2);
        }
    }
}