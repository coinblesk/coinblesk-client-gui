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

package com.coinblesk.client.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.HashSet;
import java.util.Set;


/**
 * @author Andreas Albrecht
 */
public final class SharedPrefUtils {

    /**
     * see: main/res/xml/settings_pref.xml
     */
    public final static class PreferenceKeys {
        public static final String CONNECTION_SETTINGS = "pref_connection_settings";
        public static final String NETWORK_SETTINGS = "pref_network_list";
        public static final String FIAT_CURRENCY = "pref_currency_list";
        public static final String LOCKTIME_PERIOD = "pref_wallet_locktime_period";
        public static final String PRIMARY_BALANCE = "pref_balance_list";
        public static final String CUSTOM_BUTTONS = "pref_custom_buttons";
        public static final String BITCOIN_SCALE_PREFIX = "pref_bitcoin_rep_list";
    }

    // TODO: default is mainnet
    private static final String DEFAULT_NETWORK = "test-net-3";
    private static final String DEFAULT_CURRENCY = "USD";
    private static final String DEFAULT_LOCKTIME_PERIOD = "3"; // months
    private static final Set<String> DEFAULT_CONNECTION_SETTINGS = new HashSet<>();
    private static final String DEFAULT_PRIMARY_BALANCE = "Bitcoin";

    private SharedPrefUtils() {
        // prevent instances
    }

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

    public static String getNetwork(Context context) {
        return string(context, PreferenceKeys.NETWORK_SETTINGS, DEFAULT_NETWORK);
    }

    public static boolean isNetworkTestnet(Context context) {
        return getNetwork(context).equals("test-net-3");
    }

    public static boolean isNetworkMainnet(Context context) {
        return getNetwork(context).equals("main-net");
    }

    public static String getCurrency(Context context) {
        return string(context, PreferenceKeys.FIAT_CURRENCY, DEFAULT_CURRENCY);
    }

    public static Set<String> getConnectionSettings(Context context) {
        return stringSet(context, PreferenceKeys.CONNECTION_SETTINGS, DEFAULT_CONNECTION_SETTINGS);
    }

    public static String getPrimaryBalance(Context context) {
        return string(context, PreferenceKeys.PRIMARY_BALANCE, DEFAULT_PRIMARY_BALANCE);
    }

    public static String getBitcoinScalePrefix(Context context) {
        return string(context, PreferenceKeys.BITCOIN_SCALE_PREFIX, null);
    }

    public static int getLockTimePeriodMonths(Context context) {
        String months = string(context, PreferenceKeys.LOCKTIME_PERIOD, DEFAULT_LOCKTIME_PERIOD);
        return Integer.valueOf(months);
    }

    public static String getCustomButton(Context context, String buttonKey) {
        SharedPreferences prefs = preferences(context, PreferenceKeys.CUSTOM_BUTTONS, Context.MODE_PRIVATE);
        return prefs.getString(buttonKey, null);
    }

    public static void putCustomButton(Context context, String buttonKey, String content) {
        SharedPreferences prefs = preferences(context, PreferenceKeys.CUSTOM_BUTTONS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(buttonKey, content);
        editor.apply();
    }

    public static boolean isCustomButtonEmpty(Context context, String buttonKey) {
        return getCustomButton(context, buttonKey) == null;
    }
}