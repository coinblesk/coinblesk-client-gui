package com.uzh.ckiller.coinblesk_client_gui.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.widget.ImageView;

import com.google.gson.Gson;
import com.uzh.ckiller.coinblesk_client_gui.R;

import org.bitcoinj.core.Coin;
import org.bitcoinj.utils.BtcFixedFormat;
import org.bitcoinj.utils.BtcFormat;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ch.papers.payments.WalletService;
import ch.papers.payments.models.TransactionWrapper;

/**
 * Created by ckiller on 03/03/16.
 */

public class UIUtils implements IPreferenceStrings {


    public static SpannableString getLargeBalance(Context context, WalletService.WalletServiceBinder walletServiceBinder) {

        // Get all Preferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String coinDenomination = prefs.getString(BITCOIN_REPRESENTATION_PREF_KEY, null);
        String isLargeAmount = prefs.getString(PRIMARY_BALANCE_PREF_KEY, null);

        // TODO -> As of now, currency retrieved via getBalanceFiat().getCurrencyCode()
        // TODO -> Does this make sense? What it a user changes his primary currency?
//        String fiatCurrency = prefs.getString(FIAT_CURRENCY_PREF_KEY, null);

        SpannableString result = new SpannableString("");

        switch (isLargeAmount) {
            case BTC_AS_PRIMARY:
                result = toLargeSpannable(context, scaleCoin(walletServiceBinder.getBalance(), coinDenomination), coinDenomination);
                break;
            case FIAT_AS_PRIMARY:
                result = toLargeSpannable(context, walletServiceBinder.getBalanceFiat().toPlainString(), walletServiceBinder.getBalanceFiat().getCurrencyCode());
                break;
        }

        return result;
    }

    public static SpannableString getSmallBalance(Context context, WalletService.WalletServiceBinder walletServiceBinder) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String coinDenomination = prefs.getString(BITCOIN_REPRESENTATION_PREF_KEY, null);
        String isLargeAmount = prefs.getString(PRIMARY_BALANCE_PREF_KEY, null);
        SpannableString result = new SpannableString("");

        switch (isLargeAmount) {
            case BTC_AS_PRIMARY:
                result = toSmallSpannable(walletServiceBinder.getBalanceFiat().toPlainString(), walletServiceBinder.getBalanceFiat().getCurrencyCode());
                break;
            case FIAT_AS_PRIMARY:
                result = toSmallSpannable(scaleCoin(walletServiceBinder.getBalance(), coinDenomination), coinDenomination);
                break;
        }
        return result;

    }

    public static String scaleCoin(Coin coin, String coinDenomination) {
        String result = "";
        // Dont try to use the Builder,"You cannot invoke both scale() and style()"... Add Symbol (Style) Manually
        switch (coinDenomination) {
            case COIN:
                result = BtcFormat.getInstance(BtcFormat.COIN_SCALE).format(coin);
                break;
            case MILLICOIN:
                result = BtcFormat.getInstance(BtcFormat.MILLICOIN_SCALE).format(coin);
                break;
            case MICROCOIN:
                result = BtcFormat.getInstance(BtcFormat.MICROCOIN_SCALE).format(coin);
                break;
        }

        return result;
    }

    public static String scaleCoinForDialogs(Coin coin, Context context) {
        String result = "";
        String coinDenomination = UIUtils.getCoinDenomination(context);
        // Dont try to use the Builder,"You cannot invoke both scale() and style()"... Add Symbol (Style) Manually
        switch (coinDenomination) {
            case COIN:
                result = BtcFormat.getInstance(BtcFormat.COIN_SCALE).format(coin, 0, BtcFixedFormat.REPEATING_PLACES);
                break;
            case MILLICOIN:
                result = BtcFormat.getInstance(BtcFormat.MILLICOIN_SCALE).format(coin,0,BtcFixedFormat.REPEATING_PLACES);
                break;
            case MICROCOIN:
                result = BtcFormat.getInstance(BtcFormat.MICROCOIN_SCALE).format(coin,0,BtcFixedFormat.REPEATING_PLACES);
                break;
        }

        return result + " " + coinDenomination;
    }


    public static Coin getValue(String amount, Context context) {
        BigDecimal bdAmount = new BigDecimal(amount);

        BigDecimal multiplicand = new BigDecimal(Coin.COIN.getValue());
        switch (getCoinDenomination(context)) {
            case MILLICOIN:
                multiplicand = new BigDecimal((Coin.MILLICOIN.getValue()));
                break;
            case MICROCOIN:
                multiplicand = new BigDecimal((Coin.MICROCOIN.getValue()));
                break;
        }

        return Coin.valueOf(bdAmount.multiply(multiplicand).longValue());

    }


    public static String getCoinDenomination(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(BITCOIN_REPRESENTATION_PREF_KEY, null);
    }

    public static SpannableString toSmallSpannable(String amount, String currency) {
        StringBuffer stringBuffer = new StringBuffer(amount + " " + currency);
        SpannableString spannableString = new SpannableString(stringBuffer);
        return spannableString;
    }

    public static SpannableString toLargeSpannable(Context context, String amount, String currency) {
        final int amountLength = amount.length();
        SpannableString result = new SpannableString(new StringBuffer(amount + " " + currency.toString()));
        result.setSpan(new RelativeSizeSpan(2), 0, amountLength, 0);
        result.setSpan(new ForegroundColorSpan(Color.WHITE), 0, amountLength, 0);
        result.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.colorAccent)), amountLength, result.length(), 0);
        return result;
    }

    public static int getLargeTextSize(Context context, int amountLength) {

            /*final int screenLayout = context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
        switch (screenLayout) {
            case Configuration.SCREENLAYOUT_SIZE_LARGE:
            case Configuration.SCREENLAYOUT_SIZE_XLARGE:
                // TABLETS
                break;
            default:
                // PHONES
                break;
        }*/


        int textSize = context.getResources().getInteger(R.integer.text_size_xxlarge);
        final int orientation = context.getResources().getConfiguration().orientation;
        switch (orientation) {
            case Configuration.ORIENTATION_LANDSCAPE:
                textSize = context.getResources().getInteger(R.integer.text_size_large_landscape);
                if (amountLength > 6)
                    textSize = context.getResources().getInteger(R.integer.text_size_medium_landscape);
                if (amountLength > 7)
                    textSize = context.getResources().getInteger(R.integer.text_size_small_landscape);
                break;
            case Configuration.ORIENTATION_PORTRAIT:
                if (amountLength > 6)
                    textSize = context.getResources().getInteger(R.integer.text_size_xlarge);
                if (amountLength > 7)
                    textSize = context.getResources().getInteger(R.integer.text_size_large);
                if (amountLength > 8)
                    textSize = context.getResources().getInteger(R.integer.text_size_medium);
                break;
        }

        return textSize;
    }

    public static SpannableString toFriendlyAmountString(Context context, TransactionWrapper transaction) {
        StringBuffer friendlyAmount = new StringBuffer(transaction.getAmount().toFriendlyString());
        final int coinLength = friendlyAmount.length() - 3;

        friendlyAmount.append(" ~ " + transaction.getTransaction().getExchangeRate().coinToFiat(transaction.getAmount()));
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


    public static void formatConnectionIcon(Context context, ImageView imageView, String status) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> connectionSettings = prefs.getStringSet(CONNECTION_SETTINGS_PREF_KEY, new HashSet<String>());

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

    public static int getFractionalLengthFromString(String amount) {
        // Escape '.' otherwise won't work
        String delims = "\\.";
        int length = -1;
        String[] tokens = amount.split(delims);
        if (tokens.length == 2)
            length = tokens[1].length();
        return length;
    }

    public static int getIntegerLengthFromString(String amount) {
        // Escape '.' otherwise won't work
        String delims = "\\.";
        int length = -1;
        String[] tokens = amount.split(delims);
        if (tokens.length == 1)
            length = tokens[0].length();
        return length;
    }

    public static boolean isDecimal(String amount) {
        return ((amount.contains(".")) ? true : false);

    }

    public static int getDecimalThreshold(String coinDenomination) {
        int threshold = 2;
        switch (coinDenomination) {
            case COIN:
                threshold = 8;
                break;
            case MILLICOIN:
                threshold = 5;
                break;
            case MICROCOIN:
                threshold = 2;
                break;
        }
        return threshold;
    }


}
