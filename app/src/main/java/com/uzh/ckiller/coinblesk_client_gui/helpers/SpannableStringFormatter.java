package com.uzh.ckiller.coinblesk_client_gui.helpers;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;

import com.uzh.ckiller.coinblesk_client_gui.R;

import org.bitcoinj.core.Transaction;

import ch.papers.payments.models.TransactionWrapper;

/**
 * Created by ckiller on 04/02/16.
 */
public class SpannableStringFormatter {

    private Context context;
    private String currencyString;

    public SpannableStringFormatter(Context context) {
        this.context = context;
    }

    public SpannableString toSmallSpannable(String amount, String currency) {

        // Append Currency
        this.currencyString = " " + currency;
        StringBuffer stringBuffer = new StringBuffer(amount);
        stringBuffer.append(currencyString);

        // Create spannable String
        SpannableString smallSpannable = new SpannableString(stringBuffer);
        return smallSpannable;

    }

    public SpannableString toLargeSpannable(String amount, String currency) {

        final int start = 0;
        final int end = amount.length();
        this.currencyString = " " + currency;

        // Append Currency
        StringBuffer stringBuffer = new StringBuffer(amount);
        stringBuffer.append(this.currencyString);

        // Create spannable String
        SpannableString largeSpannable = new SpannableString(stringBuffer);

        // Set size and colors
        largeSpannable.setSpan(new RelativeSizeSpan(2), start, end, 0); // set size
        largeSpannable.setSpan(new ForegroundColorSpan(Color.WHITE), start, end, 0);// set color
        largeSpannable.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.colorAccent)), end, (end + 4), 0); // set color

        return largeSpannable;

    }

    public SpannableString toFriendlyAmountString(TransactionWrapper transaction) {

        StringBuffer friendlyAmount = new StringBuffer(transaction.getAmount().toFriendlyString());
        final int endCoin = friendlyAmount.length() - 3;

        // TODO Calculate the Fiat Amount
        friendlyAmount.append(" ~ " + "4234.45" + " CHF");
        final int endFiat = friendlyAmount.length();


        SpannableString friendlySpannable = new SpannableString(friendlyAmount);

        friendlySpannable.setSpan(new RelativeSizeSpan(2), 0, endCoin, 0);
        friendlySpannable.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.colorAccent)), endCoin, (endCoin + 4), 0);
        friendlySpannable.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.main_color_400)), (endCoin + 4), endFiat, 0);


        return friendlySpannable;

    }

    public SpannableString toFriendlySnackbarString(String input) {
        final ForegroundColorSpan whiteSpan = new ForegroundColorSpan(ContextCompat.getColor(context, R.color.colorAccent));
        final SpannableString snackbarText = new SpannableString(input);
        snackbarText.setSpan(whiteSpan, 0, snackbarText.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        return snackbarText;
    }

    public String toFriendlyCustomButtonString(String description, String amount){
        String result = description + System.getProperty("line.separator") + amount;
        return result;
    }


}
