package com.uzh.ckiller.coinblesk_client_gui.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.Image;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.widget.ImageView;

import com.google.gson.Gson;
import com.uzh.ckiller.coinblesk_client_gui.R;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ch.papers.payments.models.TransactionWrapper;

/**
 * Created by ckiller on 03/03/16.
 */
public class UIUtils implements IPreferenceStrings {

    public static SpannableString toSmallSpannable(String amount, String currency) {
        StringBuffer stringBuffer = new StringBuffer(amount + " " + currency);
        SpannableString smallSpannable = new SpannableString(stringBuffer);
        return smallSpannable;
    }

    public static SpannableString toLargeSpannable(Context context, String amount, String currency) {
        final int amountLength = amount.length();
        SpannableString largeSpannable = new SpannableString(new StringBuffer(amount + " " + currency));
        largeSpannable.setSpan(new RelativeSizeSpan(2), 0, amountLength, 0); // set size
        largeSpannable.setSpan(new ForegroundColorSpan(Color.WHITE), 0, amountLength, 0);// set color
        largeSpannable.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.colorAccent)), amountLength, (amountLength + 4), 0);
        return largeSpannable;
    }

    public static SpannableString toFriendlyAmountString(Context context, TransactionWrapper transaction) {
        StringBuffer friendlyAmount = new StringBuffer(transaction.getAmount().toFriendlyString());
        final int coinLength = friendlyAmount.length() - 3;

        // TODO Calculate the Fiat Amount
        friendlyAmount.append(" ~ " + "4234.45" + " CHF");
        final int amountLength = friendlyAmount.length();

        SpannableString friendlySpannable = new SpannableString(friendlyAmount);
        friendlySpannable.setSpan(new RelativeSizeSpan(2), 0, coinLength, 0);
        friendlySpannable.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.colorAccent)), coinLength, (coinLength + 4), 0);
        friendlySpannable.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.main_color_400)), (coinLength + 4), amountLength, 0);
        return friendlySpannable;

    }

    public static String formatCustomButton(String description, String amount) {
        String result = amount + System.getProperty("line.separator") + description;
        return result;
    }

    public static SpannableString toFriendlySnackbarString(Context context, String input) {
        final ForegroundColorSpan whiteSpan = new ForegroundColorSpan(ContextCompat.getColor(context, R.color.colorAccent));
        final SpannableString snackbarText = new SpannableString(input);
        snackbarText.setSpan(whiteSpan, 0, snackbarText.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        return snackbarText;
    }


    public static String appendResult(String amounts) {
        String delims = "[+]";
        String result = "";
        String[] tokens = amounts.split(delims);
        if (tokens.length > 1) {
            BigDecimal sum = new BigDecimal(0);
            for (int i = 0; i < tokens.length; i++) {
                sum = sum.add(new BigDecimal(tokens[i]));
            }
            result = sum.toString();
        }
        return result;
    }

    public static boolean isCustomButtonEmpty(Context context, String customKey) {
        SharedPreferences prefs = context.getSharedPreferences(MERCHANT_CUSTOM_BUTTONS_PREF_KEY, Context.MODE_PRIVATE);
        String json = prefs.getString(customKey, null);
        if (json == null) {
            return true;
        } else {
            return false;
        }
    }

    public static List<String> getCustomButton(Context context, String customKey) {
        SharedPreferences prefs = context.getSharedPreferences(MERCHANT_CUSTOM_BUTTONS_PREF_KEY, Context.MODE_PRIVATE);

        if (UIUtils.isCustomButtonEmpty(context, customKey) == false) {
            String json = prefs.getString(customKey, null);
            Gson gson = new Gson();
            String[] contentArray = gson.fromJson(json, String[].class);
            List<String> contentList;
            try {
                contentList = Arrays.asList(contentArray);
                return contentList;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return null;

    }

    // TODO Dynamically resize textview textsize using this method:
    //    text.setTextSize(TypedValue.COMPLEX_UNIT_SP,14);
    //    http://stackoverflow.com/questions/6998938/textview-setting-the-text-size-programmatically-doesnt-seem-to-work
    //    This is the correct answer. More specifically:
    //    float myTextSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 18F, context.getResources().getDisplayMetrics());

    public static void formatConnectionIcon(Context context, ImageView imageView, String status) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> connectionSettings = prefs.getStringSet(CONNECTION_SETTINGS, new HashSet<String>());

        // Set the Icon Color and Visibility
        if (connectionSettings != null) {
            for (String s : connectionSettings) {
                switch (s) {
                    case NFC_ACTIVATED:
                        if (status.equals(NFC_ACTIVATED)) {
                            makeVisible(context, imageView);
                        }
                        break;
                    case BT_ACTIVATED:
                        if (status.equals(BT_ACTIVATED)) {
                            makeVisible(context, imageView);
                        }
                        break;
                    case WIFIDIRECT_ACTIVATED:
                        if (status.equals(WIFIDIRECT_ACTIVATED)) {
                            makeVisible(context, imageView);
                        }
                        break;
                }

            }

        }

    }

    private static void makeVisible(Context context, ImageView imageView) {
        imageView.setAlpha(ICON_VISIBLE);
        imageView.setColorFilter(ContextCompat.getColor(context, R.color.colorAccent));
    }


}
