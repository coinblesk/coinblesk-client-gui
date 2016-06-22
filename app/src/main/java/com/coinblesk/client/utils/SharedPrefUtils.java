/*
 * Copyright 2016 The Coinblesk team and the CSG Group at University of Zurich
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.coinblesk.client.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.ListPreference;
import android.preference.PreferenceManager;

import com.coinblesk.client.R;

import java.util.Arrays;
import java.util.HashSet;
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

    private static String string(Context context, String key, String defaultValue) {
        return preferences(context).getString(key, defaultValue);
    }

    private static Set<String> stringSet(Context context, String key, Set<String> defaultValues) {
        return preferences(context).getStringSet(key, defaultValues);
    }

    private static boolean getBoolean(Context context, String key) {
        return preferences(context).getBoolean(key, false);
    }

    private static void setBoolean(Context context, String key, boolean value) {
        SharedPreferences prefs = preferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(key, value);
        editor.commit();
    }

    public static String getNetwork(Context context) {
        return string(context, context.getResources().getString(R.string.pref_network_list),
                context.getResources().getString(R.string.pref_network_default_value));
    }

    public static boolean isNetworkTestnet(Context context) {
        return getNetwork(context).equals("test-net-3");
    }

    public static boolean isNetworkMainnet(Context context) {
        return getNetwork(context).equals("main-net");
    }

    public static String getCurrency(Context context) {
        return string(context, context.getResources().getString(R.string.pref_currency_list),
                context.getResources().getString(R.string.pref_currency_default_value));
    }

    public static Set<String> getConnectionSettings(Context context) {
        String [] connections = context.getResources().getStringArray(R.array.pref_connection_default);
        return stringSet(context, context.getResources().getString(R.string.pref_connection_settings),  new HashSet<String>(Arrays.asList(connections)));
    }

    public static String getPrimaryBalance(Context context) {
        return string(context, context.getResources().getString(R.string.pref_balance_list),
                context.getResources().getString(R.string.pref_balance_default_value));
    }

    public static String getBitcoinScalePrefix(Context context) {
        return string(context, context.getResources().getString(R.string.pref_bitcoin_rep_list), null);
    }

    public static int getLockTimePeriodMonths(Context context) {
        String months = string(context, context.getResources().getString(R.string.pref_wallet_locktime_period),
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
        return getBoolean(context, context.getResources().getString(R.string.pref_multisig_2of2_to_cltv));
    }

    public static void setJSessionID(Context context, String jsessionID) {
        SharedPreferences prefs = preferences(context, context.getResources().getString(R.string.jsessionid), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(context.getResources().getString(R.string.jsessionid), jsessionID);
        editor.commit();
    }

    public static String getJSessionID(Context context) {
        return string(context, context.getResources().getString(R.string.jsessionid), "");
    }
}