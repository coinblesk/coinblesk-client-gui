package ch.papers.payments;

import org.bitcoinj.core.BitcoinSerializer;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.TestNet3Params;

import java.util.UUID;

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

    public static final String BITCOIN_URI_KEY = "BITCOIN_URI_KEY";

    // communication via broadcast receiver, these are the actions
    public final static String WALLET_READY_ACTION = "WALLET_READY_ACTION";
    public final static String WALLET_PROGRESS_ACTION = "WALLET_PROGRESS_ACTION";
    public final static String WALLET_BALANCE_CHANGED_ACTION = "WALLET_BALANCE_CHANGED_ACTION";
    public final static String WALLET_TRANSACTIONS_CHANGED_ACTION = "WALLET_TRANSACTIONS_CHANGED_ACTION";
    public final static String WALLET_SCRIPTS_CHANGED_ACTION = "WALLET_SCRIPTS_CHANGED_ACTION";
    public static final String WALLET_INSUFFICIENT_BALANCE = "WALLET_INSUFFICIENT_BALANCE";
    public static final String WALLET_COINS_SENT = "WALLET_COINS_SENT";
    public static final String WALLET_COINS_RECEIVED = "WALLET_COINS_RECEIVED";

    //Crypto constants
    public final static String SYMMETRIC_CIPHER_ALGORITH = "AES";

    // coinblesk server communication
    public static final String COINBLESK_SERVER_BASE_URL = "http://192.168.1.36:8080/";
    //public static final String COINBLESK_SERVER_BASE_URL = "http://bitcoin2-test.csg.uzh.ch/coinblesk-server/";


    public static final UUID SERVICE_UUID = UUID.fromString("f36681f8-c73b-4a02-94a6-a87a8a351dc2");
    public static final int SERVICE_PORT = 60030;
    public final static int DISCOVERABLE_DURATION = 600; //this is unlimited
    public static final int BUFFER_SIZE=1024;

    public static final UUID WRITE_CHARACTERISTIC_UUID = UUID.fromString("f36681f8-c73b-4a02-94a6-a87a8a351dc3");
    public static final UUID READ_CHARACTERISTIC_UUID = UUID.fromString("f36681f8-c73b-4a02-94a6-a87a8a351dc4");
}
