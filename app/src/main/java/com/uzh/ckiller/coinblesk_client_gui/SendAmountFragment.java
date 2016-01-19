package com.uzh.ckiller.coinblesk_client_gui;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by ckiller
 */

public class SendAmountFragment extends Fragment {

    // This should be replaced by something smarter -> Use Observer pattern
    private String bitcoinSendAmount;
    private String fiatSendAmount;

    public static SendAmountFragment newInstance(int page) {
        SendAmountFragment fragment = new SendAmountFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_send_amount, container, false);
        setInitBalance(view);

        return view;
    }

    private void setInitBalance(View view) {

        // 1 Get view references
        final TextView tvSmallBalance = (TextView) view.findViewById(R.id.send_amount_small);
        final TextView tvLargeBalance = (TextView) view.findViewById(R.id.send_amount_large);

        // 2 Get Balance
        // TODO instead of dummy data, get the real balance here.
        setBitcoinSendAmount("0.00");
        setFiatSendAmount("0.00");

        // 3 Set the small balance & Format the SpannableStrings for the large one
        // TODO Feed the Balance into a Method to format properly
        tvLargeBalance.setText(formatInitBalance(bitcoinSendAmount));
        tvSmallBalance.setText(fiatSendAmount);

    }

    public void updateSendAmount(Bundle bundle) {
        String text = "-1";
        for (String key : bundle.keySet()) {
            if (bundle != null && bundle.containsKey(key)) {
                text = bundle.getString(key);
            }
        }

        StringBuffer updatedAmount = new StringBuffer(getBitcoinSendAmount());
        updatedAmount.append(text);
        setBitcoinSendAmount(text.toString());

        TextView tvBalance = (TextView) getActivity().findViewById(R.id.send_amount_large);
        tvBalance.setText(formatSendAmount(updatedAmount));
    }

    private SpannableString formatSendAmount(StringBuffer updatedAmount) {
        final int end = updatedAmount.length();
        updatedAmount.append(" BTC");

        SpannableString spannableString = new SpannableString(updatedAmount);
        spannableString.setSpan(new RelativeSizeSpan(2), 0, end, 0); // set size
        spannableString.setSpan(new ForegroundColorSpan(Color.WHITE), 0, end, 0);// set color
        spannableString.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorAccent)), end, (end + 4), 0);// set color

        return spannableString;

    }


    private SpannableString formatInitBalance(String string) {

        // Get variables for RelativeSizeSpan
        final int start = 0;
        final int end = string.length();

        // Append BTC
        StringBuffer sb = new StringBuffer(string);
        sb.append(" BTC");

        // Create spannable String
        SpannableString spannableString = new SpannableString(sb);

        // Set size and colors
        spannableString.setSpan(new RelativeSizeSpan(2), start, end, 0); // set size
        spannableString.setSpan(new ForegroundColorSpan(Color.WHITE), start, end, 0);// set color
        spannableString.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorAccent)), end, (end + 4), 0);// set color

        return spannableString;
    }


    public String getBitcoinSendAmount() {
        return bitcoinSendAmount;
    }

    public void setBitcoinSendAmount(String bitcoinSendAmount) {this.bitcoinSendAmount = bitcoinSendAmount;}

    public String getFiatSendAmount() {
        return fiatSendAmount;
    }

    public void setFiatSendAmount(String fiatSendAmount) {
        this.fiatSendAmount = fiatSendAmount;
    }


}

