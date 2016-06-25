/*
 * Copyright 2016 The Coinblesk team and the CSG Group at University of Zurich
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package com.coinblesk.client.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.coinblesk.client.common.R;
import com.coinblesk.client.models.AddressBookItem;
import com.coinblesk.util.SerializeUtils;
import com.google.gson.reflect.TypeToken;

import org.bitcoinj.utils.ExchangeRate;
import org.bitcoinj.utils.Fiat;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * @author Andreas Albrecht
 * @author Thomas Bocek
 */
public final class SharedPrefUtils {

    // prevent instances
    private SharedPrefUtils() {}

    private static SharedPreferences preferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    private static SharedPreferences preferences(Context context, String name, int mode) {
        return context.getSharedPreferences(name, mode);
    }

    private static String getString(Context context, String key, String defaultValue) {
        return preferences(context).getString(key, defaultValue);
    }

    private static void setString(Context context, String key, String value) {
        SharedPreferences.Editor editor = preferences(context).edit();
        editor.putString(key, value);
        editor.commit();
    }

    private static Set<String> getStringSet(Context context, String key, Set<String> defaultValues) {
        return preferences(context).getStringSet(key, defaultValues);
    }

    private static void setStringSet(Context context, String key, Set<String> values) {
        SharedPreferences.Editor editor = preferences(context).edit();
        editor.putStringSet(key, values);
        editor.commit();
    }

    private static boolean getBoolean(Context context, String key, boolean defaultValue) {
        return preferences(context).getBoolean(key, defaultValue);
    }

    private static void setBoolean(Context context, String key, boolean value) {
        SharedPreferences.Editor editor = preferences(context).edit();
        editor.putBoolean(key, value);
        editor.commit();
    }

    private static long getLong(Context context, String key, long defaultValue) {
        return preferences(context).getLong(key, defaultValue);
    }

    private static void setLong(Context context, String key, long value) {
        SharedPreferences.Editor editor = preferences(context).edit();
        editor.putLong(key, value);
        editor.commit();
    }

    public static String getNetwork(Context context) {
        return getString(context, context.getString(R.string.pref_network_list),
                context.getString(R.string.pref_network_default_value));
    }

    public static boolean isNetworkTestnet(Context context) {
        return getNetwork(context).equals("test-net-3");
    }

    public static boolean isNetworkMainnet(Context context) {
        return getNetwork(context).equals("main-net");
    }

    public static String getCurrency(Context context) {
        return getString(context, context.getResources().getString(R.string.pref_currency_list),
                context.getResources().getString(R.string.pref_currency_default_value));
    }

    public static Set<String> getConnectionSettings(Context context) {
        String [] connections = context.getResources().getStringArray(R.array.pref_connection_default);
        return getStringSet(context, context.getResources().getString(R.string.pref_connection_settings),  new HashSet<String>(Arrays.asList(connections)));
    }

    public static String getPrimaryBalance(Context context) {
        return getString(context, context.getResources().getString(R.string.pref_balance_list),
                context.getResources().getString(R.string.pref_balance_default_value));
    }

    public static String getBitcoinScalePrefix(Context context) {
        return getString(context, context.getResources().getString(R.string.pref_bitcoin_rep_list), null);
    }

    public static int getLockTimePeriodMonths(Context context) {
        String months = getString(context, context.getResources().getString(R.string.pref_wallet_locktime_period),
                context.getResources().getString(R.string.pref_wallet_locktime_period_default));
        return Integer.valueOf(months);
    }

    public static String getCustomButton(Context context, String buttonKey) {
        SharedPreferences prefs = preferences(context, context.getResources().getString(R.string.pref_custom_buttons), Context.MODE_PRIVATE);
        return prefs.getString(buttonKey, null);
    }

    public static void putCustomButton(Context context, String buttonKey, String content) {
        SharedPreferences prefs = preferences(context, context.getResources().getString(R.string.pref_custom_buttons), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(buttonKey, content);
        editor.commit();
    }

    public static boolean isCustomButtonEmpty(Context context, String buttonKey) {
        return getCustomButton(context, buttonKey) == null;
    }

    public static void enableMultisig2of2ToCltvForwarder(Context context) {
        setBoolean(context, context.getResources().getString(R.string.pref_multisig_2of2_to_cltv), true);
    }

    public static boolean isMultisig2of2ToCltvForwardingEnabled(Context context) {
        return getBoolean(context, context.getResources().getString(R.string.pref_multisig_2of2_to_cltv), false);
    }

    public static String getJSessionID(Context context) {
        return getString(context, context.getResources().getString(R.string.jsessionid), "");
    }

    public static void setJSessionID(Context context, String jsessionID) {
        setString(context, context.getString(R.string.jsessionid), jsessionID);
    }

    public static ExchangeRate getExchangeRate(Context context, String currencySymbol) {
        String key = context.getString(R.string.pref_exchange_rate, currencySymbol);
        long defaultValue = 0;
        if (!preferences(context).contains(key)) {
            String defaultResourceName = context.getString(R.string.pref_exchange_rate_default_key, currencySymbol);
            int defaultResourceId = context.getResources()
                    .getIdentifier(defaultResourceName, "string", context.getPackageName());
            if (defaultResourceId != 0) {
                defaultValue = 10000 * Long.valueOf(context.getString(defaultResourceId));
            } else {
                defaultValue = 10000; // result: 1 coin = 1 amount of unknown currency
            }
        }
        long oneCoinInFiat = getLong(context, key, defaultValue);
        return new ExchangeRate(Fiat.valueOf(currencySymbol, oneCoinInFiat));
    }

    public static void setExchangeRate(Context context, Fiat oneCoinInFiat) {
        String key = context.getString(R.string.pref_exchange_rate, oneCoinInFiat.getCurrencyCode());
        setLong(context, key, oneCoinInFiat.getValue());
    }

    public static List<AddressBookItem> getAddressBookItems(Context context) {
        String json = getString(context, context.getString(R.string.pref_address_book_items), null);
        if (json == null || json.isEmpty()) {
            return new ArrayList<>(0);
        }

        Type arrayListType = new TypeToken<ArrayList<AddressBookItem>>(){}.getType();
        List<AddressBookItem> items = SerializeUtils.GSON.fromJson(json, arrayListType);
        return items;
    }

    public static void setAddressBookItems(Context context, List<AddressBookItem> items) {
        String json = SerializeUtils.GSON.toJson(items);
        setString(context, context.getString(R.string.pref_address_book_items), json);
    }

}