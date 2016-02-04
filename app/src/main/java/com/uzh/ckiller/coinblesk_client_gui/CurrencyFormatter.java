package com.uzh.ckiller.coinblesk_client_gui;

import android.content.Context;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;

/**
 * Created by ckiller on 04/02/16.
 */
public class CurrencyFormatter {

    private Context mContext;
    private String mCurrencyString;

    public CurrencyFormatter(Context context) {
        this.mContext = context;
    }

    public SpannableString formatSmall(String amount, String currency) {

        // Append Currency
        mCurrencyString = " " + currency;
        StringBuffer stringBuffer = new StringBuffer(amount);
        stringBuffer.append(mCurrencyString);

        // Create spannable String
        SpannableString spannableString = new SpannableString(stringBuffer);
        return spannableString;

    }

    public SpannableString formatLarge(String amount, String currency) {

        final int start = 0;
        final int end = amount.length();
        mCurrencyString = " " + currency;

        // Append Currency
        StringBuffer stringBuffer = new StringBuffer(amount);
        stringBuffer.append(mCurrencyString);

        // Create spannable String
        SpannableString spannableString = new SpannableString(stringBuffer);

        // Set size and colors
        spannableString.setSpan(new RelativeSizeSpan(2), start, end, 0); // set size
        spannableString.setSpan(new ForegroundColorSpan(Color.WHITE), start, end, 0);// set color
        spannableString.setSpan(new ForegroundColorSpan(mContext.getApplicationContext().getResources().getColor(R.color.colorAccent)), end, (end + 4), 0); // set color

        return spannableString;

    }
}
