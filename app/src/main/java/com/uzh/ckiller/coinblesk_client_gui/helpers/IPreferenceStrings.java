package com.uzh.ckiller.coinblesk_client_gui.helpers;

/**
 * Created by ckiller on 12/02/16.
 */
public interface IPreferenceStrings {

    // Preference Strings
    String CONNECTION_SETTINGS_PREF_KEY = "pref_connection_settings";
    String MERCHANT_CUSTOM_BUTTONS_PREF_KEY = "MERCHANT_CUSTOM_BUTTONS";

    String PRIMARY_BALANCE_PREF_KEY = "pref_balance_list";
    String FIAT_AS_PRIMARY = "Fiat Currency";
    String BTC_AS_PRIMARY = "Bitcoin";

    String FIAT_CURRENCY_PREF_KEY = "pref_currency_list";
    String EURO_AS_CURRENCY = "EUR";
    String CHF_AS_CURRENCY = "CHF";
    String USD_AS_CURRENCY = "USD";

    String BITCOIN_REPRESENTATION_PREF_KEY = "pref_bitcoin_rep_list";
    String COIN = "BTC";
    String MILLICOIN = "mBTC";
    String MICROCOIN = "μBTC";

    // Preference Status Strings
    String NFC_ACTIVATED = "nfc-checked";
    String BT_ACTIVATED = "bt-checked";
    String WIFIDIRECT_ACTIVATED = "wifi-checked";

    // Icon Visibility if a connection Icon is active.
    Float ICON_VISIBLE = new Float(0.8);

}
