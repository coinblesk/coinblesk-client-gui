package com.coinblesk.client.additionalservices;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import com.coinblesk.client.R;
import com.coinblesk.payments.WalletService;

/**
 * Created by draft on 16.06.16.
 */
public class AdditionalServicesAdapter extends ArrayAdapter<String> {

    private final static String TAG = AdditionalServicesAdapter.class.getName();

    final private WalletService.WalletServiceBinder walletServiceBinder;
    final private AdditionalServicesActivity.AdditionalServiceGUIState listener;
    final private Activity activity;

    public AdditionalServicesAdapter(Activity activity, WalletService.WalletServiceBinder walletServiceBinder, AdditionalServicesActivity.AdditionalServiceGUIState listener) {
        super(activity, 0);
        this.activity = activity;
        this.walletServiceBinder = walletServiceBinder;
        this.listener = listener;

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
                    CheckBox checkBox = (CheckBox) convertView.findViewById(R.id.checkBox);
                    TextView textView = (TextView) convertView.findViewById(R.id.firstLine);
                    checkBox.setChecked(false);
                    checkBox.setEnabled(true);
                    listener.addCheckBox(checkBox);
                    listener.addTextView(textView);
                    convertView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            AdditionalServicesUsernameDialog a = new AdditionalServicesUsernameDialog();
                            a.setData(listener)
                                    .show(((Activity) getContext()).getFragmentManager(), TAG);
                        }
                    });
                }

                return convertView;
            case 1:
                if (convertView == null) {
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.additional_services_item2, parent, false);
                    TextView balance = (TextView) convertView.findViewById(R.id.additional_services_balance);
                    listener.addBalance(balance);
                    convertView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            new AlertDialog.Builder(getContext())
                                    .setMessage(R.string.additional_services_description_2)
                                    .setTitle(R.string.additional_services_description_3)
                                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            new AdditionalServicesTasks.TransferP2SHTask(walletServiceBinder.getMultisigClientKey(), listener).execute();
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
}