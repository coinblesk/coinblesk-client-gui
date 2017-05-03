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
import android.util.Base64;
import android.util.Log;

import com.coinblesk.client.R;
import com.coinblesk.client.config.Constants;
import com.coinblesk.client.models.AddressBookItem;
import com.coinblesk.client.models.LockTime;
import com.coinblesk.util.BitcoinUtils;
import com.coinblesk.util.SerializeUtils;
import com.google.gson.reflect.TypeToken;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.utils.ExchangeRate;
import org.bitcoinj.utils.Fiat;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * @author Andreas Albrecht
 * @author Thomas Bocek
 */
public final class SharedPrefUtils {

    private static final String TAG = SharedPrefUtils.class.getName();

    // prevent instances
    private SharedPrefUtils() {}

    public static String getSharedPreferencesName() {
        return Constants.SHARED_PREFERENCES_NAME;
    }

    public static int getSharedPreferencesMode() {
        return Context.MODE_PRIVATE;
    }

    public static File getSharedPreferencesFile(Context context) {
        String fileName = getSharedPreferencesName() + ".xml";
        File sharedPrefs = new File(context.getApplicationInfo().dataDir, "shared_prefs");
        return new File(sharedPrefs, fileName);
    }

    public static void initDefaults(Context context, int resId, boolean readAgain) {
        PreferenceManager.setDefaultValues(
                context,
                getSharedPreferencesName(),
                getSharedPreferencesMode(),
                resId,
                readAgain);
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(getSharedPreferencesName(), getSharedPreferencesMode());
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
        return new HashSet<>(preferences(context).getStringSet(key, defaultValues));
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

    private static int getInt(Context context, String key, int defaultValue) {
        return preferences(context).getInt(key, defaultValue);
    }

    private static void setInt(Context context, String key, int value) {
        SharedPreferences.Editor editor = preferences(context).edit();
        editor.putInt(key, value);
        editor.commit();
    }

    private static float getFloat(Context context, String key, float defaultValue) {
        return preferences(context).getFloat(key, defaultValue);
    }

    private static void setFloat(Context context, String key, float value) {
        SharedPreferences.Editor editor = preferences(context).edit();
        editor.putFloat(key, value);
        editor.commit();
    }

    private static byte[] getBytes(Context context, String key, byte[] defaultValue) {
        String valueBase64 = getString(context, key, null);
        return (valueBase64 == null) ? defaultValue : Base64.decode(valueBase64, Base64.NO_WRAP);
    }

    private static void setBytes(Context context, String key, byte[] value) {
        String valueBase64 = Base64.encodeToString(value, Base64.NO_WRAP);
        setString(context, key, valueBase64);
    }

    private static void saveArchivedCopy(Context context, final String prefKey) {
        final Object value = preferences(context).getAll().get(prefKey);
        if (value == null) {
            return;
        }

        String archiveKey = null;
        long currentTime = System.currentTimeMillis();
        for (int i = 0; ; ++i) {
            String postfix = i > 0 ? String.format("_%d", i) : "";
            archiveKey = String.format("%s_%d%s", prefKey, currentTime, postfix);
            if (!preferences(context).contains(archiveKey)) {
                break;
            }
        }

        if (value instanceof Integer) {
            setInt(context, archiveKey, (Integer) value);
        } else if (value instanceof Long) {
            setLong(context, archiveKey, (Long) value);
        } else if (value instanceof Float) {
            setFloat(context, archiveKey, (Float) value);
        } else if (value instanceof Boolean) {
            setBoolean(context, archiveKey, (Boolean) value);
        } else if (value instanceof String) {
            setString(context, archiveKey, (String) value);
        } else if (value instanceof Set<?> &&
                (!((Set<?>) value).isEmpty() && (((Set<?>)value).iterator().next() instanceof String))) {
            setStringSet(context, archiveKey, (Set<String>) value);
        } else {
            throw new IllegalStateException("Unknown type");
        }
    }

    public static String getAppVersion(Context context) {
        return getString(context, context.getString(R.string.pref_app_version), null);
    }

    public static void setAppVersion(Context context, String versionName) {
        String key = context.getString(R.string.pref_app_version);
        setString(context, key, versionName);
    }

    public static String getNetwork(Context context) {
        return getString(context, context.getString(R.string.pref_network_list),
                context.getString(R.string.pref_network_default_value));
    }

    public static void setNetwork(Context context, String network) {
        String key = context.getString(R.string.pref_network_list);
        setString(context, key, network);
    }

    public static boolean isNetworkTestnet(Context context) {
        return getNetwork(context).equals(context.getString(R.string.pref_network_testnet));
    }

    public static boolean isNetworkMainnet(Context context) {
        return getNetwork(context).equals(context.getString(R.string.pref_network_mainnet));
    }

    public static String getLocalTestNetServerUrl(Context context) {
        String key = context.getString(R.string.pref_network_localtestnet_server_url);
        String defaultValue = context.getString(R.string.pref_network_localtestnet_server_url_default);
        return getString(context, key, defaultValue);
    }

    public static void setLocalTestNetServerUrl(Context context, String serverUrl) {
        String key = context.getString(R.string.pref_network_localtestnet_server_url);
        setString(context, key, serverUrl);
    }

    public static String getCurrency(Context context) {
        return getString(context,
                context.getString(R.string.pref_currency_list),
                context.getString(R.string.pref_currency_default_value));
    }

    public static void setCurrency(Context context, String currency) {
        String key = context.getString(R.string.pref_currency_list);
        setString(context, key, currency);
    }

    public static Set<String> getConnectionSettings(Context context) {
        String [] connectionDefaults = context.getResources().getStringArray(R.array.pref_connection_default);
        String key = context.getString(R.string.pref_connection_settings);
        return getStringSet(context, key,  new HashSet<>(Arrays.asList(connectionDefaults)));
    }

    public static boolean isConnectionNfcEnabled(Context context) {
        return getConnectionSettings(context)
                .contains(context.getString(R.string.pref_connection_nfc));
    }

    public static boolean isConnectionBluetoothLeEnabled(Context context) {
        return getConnectionSettings(context)
                .contains(context.getString(R.string.pref_connection_bt_le));
    }

    public static boolean isConnectionWiFiDirectEnabled(Context context) {
        return getConnectionSettings(context)
                .contains(context.getString(R.string.pref_connection_wifi_direct));
    }

    public static String getPrimaryBalance(Context context) {
        return getString(context,
                context.getString(R.string.pref_balance_list),
                context.getString(R.string.pref_balance_default_value));
    }

    public static boolean isBitcoinPrimaryBalance(Context context) {
        return getPrimaryBalance(context).equals(context.getString(R.string.pref_balance_bitcoin));
    }

    public static boolean setBitcoinPrimaryBalance(Context context) {
        setString(context,
                context.getString(R.string.pref_balance_list),
                context.getString(R.string.pref_balance_bitcoin));
        return true;
    }



    public static boolean setFiatPrimaryBalance(Context context) {
        setString(context,
                context.getString(R.string.pref_balance_list),
                context.getString(R.string.pref_balance_fiat));
        return true;
    }

    public static boolean isFiatPrimaryBalance(Context context) {
        return !isBitcoinPrimaryBalance(context);
    }

    public static String getBitcoinScalePrefix(Context context) {
        String prefix = getString(context, context.getString(R.string.pref_bitcoin_rep_list), context.getResources().getStringArray(R.array.pref_bitcoin_rep_values)[1]);
        Log.d(TAG, "get prefix: "+prefix);
        return prefix;
    }

    public static void setBitcoinScalePrefix(Context context, String prefix) {
        setString(context, context.getString(R.string.pref_bitcoin_rep_list), prefix);
    }

    public static boolean isBitcoinScaleBTC(Context context) {
        return getBitcoinScalePrefix(context)
                .equals(context.getString(R.string.pref_bitcoin_rep_BTC));
    }

    public static boolean isBitcoinScaleMilliBTC(Context context) {
        return getBitcoinScalePrefix(context)
                .equals(context.getString(R.string.pref_bitcoin_rep_milliBTC));
    }

    public static boolean isBitcoinScaleMicroBTC(Context context) {
        return getBitcoinScalePrefix(context)
                .equals(context.getString(R.string.pref_bitcoin_rep_microBTC));
    }

    public static int getLockTimePeriodMonths(Context context) {
        String key = context.getString(R.string.pref_wallet_locktime_period);
        String defaultMonths = context.getString(R.string.pref_wallet_locktime_period_default);
        String months = getString(context, key, defaultMonths);
        return Integer.valueOf(months);
    }

    public static String getCustomButton(Context context, String buttonId) {
        String key = context.getString(R.string.pref_custom_buttons, buttonId);
        return getString(context, key, null);
    }

    public static void setCustomButton(Context context, String buttonId, String content) {
        String key = context.getString(R.string.pref_custom_buttons, buttonId);
        setString(context, key, content);
    }

    public static boolean isCustomButtonEmpty(Context context, String buttonKey) {
        return getCustomButton(context, buttonKey) == null;
    }

    public static String getJSessionID(Context context) {
        return getString(context, context.getString(R.string.jsessionid), "");
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

    public static void enableMultisig2of2ToCltvForwarder(Context context) {
        setBoolean(context, context.getString(R.string.pref_multisig_2of2_to_cltv), true);
    }

    public static boolean isMultisig2of2ToCltvForwardingEnabled(Context context) {
        return getBoolean(context, context.getString(R.string.pref_multisig_2of2_to_cltv), false);
    }

    public static List<AddressBookItem> getAddressBookItems(Context context, NetworkParameters params) {
        String key = context.getString(R.string.pref_address_book_items, params.getId());
        String json = getString(context, key, null);
        if (json == null || json.isEmpty()) {
            return new ArrayList<>(0);
        }

        Type arrayListType = new TypeToken<ArrayList<AddressBookItem>>(){}.getType();
        List<AddressBookItem> items = SerializeUtils.GSON.fromJson(json, arrayListType);
        return items;
    }

    private static void setAddressBookItems(Context context, NetworkParameters params, List<AddressBookItem> items) {
        String key = context.getString(R.string.pref_address_book_items, params.getId());
        String json = SerializeUtils.GSON.toJson(items);
        setString(context, key, json);
    }

    public static void addAddressBookItem(Context context, NetworkParameters params, AddressBookItem itemToAdd) {
        List<AddressBookItem> items = getAddressBookItems(context, params);
        if (items == null) {
            items = new ArrayList<>(1);
        }

        if (!items.contains(itemToAdd)) {
            items.add(itemToAdd);
            setAddressBookItems(context, params, items);
        }
    }

    public static void removeAddressBookItem(Context context, NetworkParameters params, AddressBookItem itemToRemove) {
        List<AddressBookItem> items = getAddressBookItems(context, params);
        if (items == null) {
            return;
        }
        items.remove(itemToRemove);
        setAddressBookItems(context, params, items);
    }

    public static ECKey getClientKey(Context context, NetworkParameters params) {
        String key = context.getString(R.string.pref_client_private_key, params.getId());
        byte[] privateKey = getBytes(context, key, null);
        return (privateKey != null) ? ECKey.fromPrivate(privateKey) : null;
    }

    public static void setClientKey(Context context, NetworkParameters params, ECKey clientKey) {
        String key = context.getString(R.string.pref_client_private_key, params.getId());
        // for safety, we store the old value (i.e. do not overwrite key data)
        saveArchivedCopy(context, key);

        if (clientKey == null) {
            preferences(context).edit().remove(key).commit();
            return;
        }

        // safety check - client key must include private key.
        if (!clientKey.hasPrivKey()) {
            throw new IllegalArgumentException("ClientKey does not have private key.");
        }

        setBytes(context, key, clientKey.getPrivKeyBytes());
    }

    public static ECKey getServerKey(Context context, NetworkParameters params) {
        String key = context.getString(R.string.pref_server_public_key, params.getId());
        byte[] publicKey = getBytes(context, key, null);
        return (publicKey != null) ? ECKey.fromPublicOnly(publicKey) : null;
    }

    public static String getServerUrl(Context context, NetworkParameters params) {
        String key = context.getString(R.string.pref_server_public_key_url, params.getId());
        return getString(context, key, "n/a");
    }

    public static void setServerKey(Context context, NetworkParameters params, ECKey serverKey, String serverUrl) {
        String key = context.getString(R.string.pref_server_public_key, params.getId());
        String key_url = context.getString(R.string.pref_server_public_key_url, params.getId());
        // for safety, we store the old value (i.e. do not overwrite key data)
        saveArchivedCopy(context, key);

        if (serverKey == null) {
            preferences(context).edit()
                    .remove(key)
                    .remove(key_url)
                    .commit();
            return;
        }

        // safety check - server key is public only.
        if (serverKey.hasPrivKey()) {
            throw new IllegalArgumentException("ServerKey should not have private key. Wrong key?");
        }

        setBytes(context, key, serverKey.getPubKey());
        setString(context, key_url, serverUrl);
    }

    public static List<LockTime> getLockTimes(Context context, NetworkParameters params) {
        String key = context.getString(R.string.pref_address_lock_times, params.getId());
        String json = getString(context, key, null);
        if (json == null || json.isEmpty()) {
            return null;
        }

        Type arrayListType = new TypeToken<ArrayList<LockTime>>(){}.getType();
        List<LockTime> lockTimes = SerializeUtils.GSON.fromJson(json, arrayListType);
        return lockTimes;
    }

    private static void setLockTimes(Context context, NetworkParameters params, List<LockTime> lockTimes) {
        String json = SerializeUtils.GSON.toJson(lockTimes);
        String key = context.getString(R.string.pref_address_lock_times, params.getId());
        setString(context, key, json);
    }

    public static void addLockTime(Context context, NetworkParameters params, LockTime lockTimeToAdd) {
        List<LockTime> lockTimes = getLockTimes(context, params);
        if (lockTimes == null) {
            lockTimes = new ArrayList<>(1);
        }
        if (!lockTimes.contains(lockTimeToAdd)) {
            lockTimes.add(lockTimeToAdd);
            setLockTimes(context, params, lockTimes);
        }
    }

    public static Set<String> getInstantTransactions(Context context, NetworkParameters params) {
        String key = context.getString(R.string.pref_instant_transactions, params.getId());
        return getStringSet(context, key, new HashSet<String>());
    }

    public static void addInstantTransaction(Context context, NetworkParameters params, String txHash) {
        String key = context.getString(R.string.pref_instant_transactions, params.getId());
        Set<String> instantTx = getStringSet(context, key, new HashSet<String>());
        instantTx.add(txHash);
        setStringSet(context, key, instantTx);
    }

    public static boolean isPaymentAutoAcceptEnabled(Context context) {
        String key = context.getString(R.string.pref_payment_auto_accept_enabled);
        boolean defaultValue = Boolean.valueOf(context.getString(R.string.pref_payment_auto_accept_enabled_default));
        return getBoolean(context, key, defaultValue);
    }

    public static Coin getPaymentAutoAcceptValue(Context context) {
        // Note: do not use getFloat because the value is stored as String (class cast exception)
        String key = context.getString(R.string.pref_payment_auto_accept_amount);
        String defaultValue = context.getString(R.string.pref_payment_auto_accept_amount_default_value);
        String valueBtc = getString(context, key, defaultValue);
        Coin valueCoin;
        try {
            valueCoin = Coin.parseCoin(valueBtc);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Could not parse coin: " + valueBtc, e);
            valueCoin = Coin.ZERO;
        }
        return valueCoin.isPositive() ? valueCoin : Coin.ZERO;
    }

}