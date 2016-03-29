package com.coinblesk.client;

/**
 * Created by ckiller on 10/03/16.
 */
public class AppConstants {

    public static final String URL_GITHUB = "http://www.github.com/coinblesk";
    public static final String URL_BITCOIN_CSG_WEBSITE = "http://www.bitcoin.csg.uzh.ch";
    public static final String URL_UZH_WEBSITE = "http://www.uzh.ch";
    public static final String URL_IFI_WEBSITE = "http://www.ifi.uzh.ch";
    public static final String URL_COINBLESK_TWITTER = "https://twitter.com/coinblesk";

    public static final String CONNECTION_SETTINGS_PREF_KEY = "pref_connection_settings";
    public static final String MERCHANT_CUSTOM_BUTTONS_PREF_KEY = "MERCHANT_CUSTOM_BUTTONS";

    public static final String PRIMARY_BALANCE_PREF_KEY = "pref_balance_list";
    public static final String FIAT_AS_PRIMARY = "Fiat Currency";
    public static final String BTC_AS_PRIMARY = "Bitcoin";

    public static final String BITCOIN_REPRESENTATION_PREF_KEY = "pref_bitcoin_rep_list";
    public static final String COIN = "BTC";
    public static final String MILLICOIN = "mBTC";
    public static final String MICROCOIN = "μBTC";
    public static final int MAXIMUM_COIN_AMOUNT_LENGTH = 7;
    public static final int MAXIMUM_FIAT_AMOUNT_LENGTH = 6;
    public static final int FIAT_DECIMAL_THRESHOLD = 2;

    // Preference Status Strings
    public static final String NFC_ACTIVATED = "nfc-checked";
    public static final String BT_ACTIVATED = "bt-checked";
    public static final String WIFIDIRECT_ACTIVATED = "wifi-checked";

    // Icon Visibility if a connection Icon is active.
    public static final Float ICON_VISIBLE = new Float(0.8);

    public static final String COLOR_MATERIAL_LIGHT_YELLOW_900 = "#F47F1F";
    public static final String COLOR_COLOR_ACCENT = "#AEEA00";
    public static final String COLOR_WHITE = "#FFFFFF";

    public static final String BACKUP_FILE_PREFIX = "coinblesk_wallet_backup";

}
