package ch.papers.payments;

import org.bitcoinj.core.BitcoinSerializer;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.TestNet3Params;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 21/01/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class Constants {
    public final static NetworkParameters PARAMS = TestNet3Params.get();
    public final static BitcoinSerializer BITCOIN_SERIALIZER = new BitcoinSerializer(Constants.PARAMS);
    public final static long UNIX_TIME_MONTH = 60*60*24*30;
    public final static int LOCK_TIME_MONTHS = 3;
    public final static int LOCK_THRESHOLD = 60*4;//https://bitcoin.org/en/developer-reference#block-headers
    public final static String WALLET_FILES_PREFIX = "cb_wallet_";
    public final static String WALLET_KEY_NAME = "wallet_key";

    public static final String MULTISIG_CLIENT_KEY_NAME = "client_public_key";
    public static final String MULTISIG_SERVER_KEY_NAME = "server_public_key";

    // communication via broadcast receiver, these are the actions
    public final static String WALLET_READY_ACTION = "WALLET_READY_ACTION";
    public final static String WALLET_PROGRESS_ACTION = "WALLET_PROGRESS_ACTION";
    public final static String WALLET_BALANCE_CHANGED_ACTION = "WALLET_BALANCE_CHANGED_ACTION";
    public final static String WALLET_TRANSACTIONS_CHANGED_ACTION = "WALLET_TRANSACTIONS_CHANGED_ACTION";
    public final static String WALLET_SCRIPTS_CHANGED_ACTION = "WALLET_SCRIPTS_CHANGED_ACTION";
    public static final String WALLET_INSUFFICIENT_BALANCE = "WALLET_INSUFFICIENT_BALANCE";
    public static final String WALLET_COINS_SENT = "WALLET_COINS_SENT";
    public static final String WALLET_COINS_RECEIVED = "WALLET_COINS_RECEIVED";

    // coinblesk server communication
    public static final String COINBLESK_SERVER_BASE_URL = "http://192.168.0.101:8080";
}
