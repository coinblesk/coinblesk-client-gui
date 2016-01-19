package com.uzh.ckiller.coinblesk_client_gui;


/**
 * Created by ckiller on 12/01/16.
 */

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
 * Created by ckiller on 10/01/16.
 */

public class BalanceCurrentFragment extends Fragment {

    public static BalanceCurrentFragment newInstance(int page) {
        BalanceCurrentFragment fragment = new BalanceCurrentFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_balance_current, container, false);

        final TextView smallBalance = (TextView) view.findViewById(R.id.balance_small);
        final TextView largeBalance = (TextView) view.findViewById(R.id.balance_large);

        //TODO getBalance() and corresponding Exchange Rate to show in Balance Card
        String s1 = "655.01 CHF";
        String s2 = "2.4431 BTC";

        //TODO properly implement this ugly hack into something reusable also in the Send / Receive Amount Fragments
        SpannableString spannLarge = new SpannableString(s2);
        spannLarge.setSpan(new RelativeSizeSpan(2), 0, 6, 0); // set size
        spannLarge.setSpan(new ForegroundColorSpan(Color.WHITE), 0, 6, 0);// set color
        spannLarge.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorAccent)), 6, 10, 0);// set color

        smallBalance.setText(s1);
        largeBalance.setText(spannLarge);

        return view;
    }
}
