package com.coinblesk.client.additionalservices;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import com.coinblesk.client.R;
import com.coinblesk.client.addresses.EditAddressFragment;

/**
 * Created by draft on 16.06.16.
 */
public class AdditionalServiceAdapter extends ArrayAdapter<String> {

    private final static String TAG = AdditionalServiceAdapter.class.getName();

    public AdditionalServiceAdapter(Activity activity) {
        super(activity, 0);
        add("Login"); //dummy entry to get the size right
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        switch(position) {
            case 0:
                if (convertView == null) {
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.additional_services_item, parent, false);
                    //TextView firstLine = (TextView) convertView.findViewById(R.id.firstLine);
                    //firstLine.setText("Login");
                    //TextView secondLine = (TextView) convertView.findViewById(R.id.secondLine);
                    //secondLine.setText("blah blah blah");
                    CheckBox checkBox = (CheckBox) convertView.findViewById(R.id.checkBox);
                    checkBox.setChecked(true);

                    convertView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            showAddAddressDialog();
                        }
                    });

                }

                return convertView;
            default: throw new RuntimeException("expected a known position");
        }
    }

    private void showAddAddressDialog() {
        DialogFragment frag = new UsernameDialog();
        frag.show(((Activity)getContext()).getFragmentManager(), TAG);
    }
}
