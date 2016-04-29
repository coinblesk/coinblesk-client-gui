package com.coinblesk.payments;

import com.coinblesk.util.SerializeUtils;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.TestNet3Params;

import java.util.UUID;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 21/01/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class Constants {

    public static NetworkParameters PARAMS = TestNet3Params.get();

    public final static long UNIX_TIME_MONTH = 60 * 60 * 24 * 30;
    public final static int LOCK_TIME_MONTHS = 3;
    public final static int LOCK_THRESHOLD = 60 * 4;//https://bitcoin.org/en/developer-reference#block-headers
    public static String WALLET_FILES_PREFIX = "mainnet_wallet_";
    public final static String WALLET_KEY_NAME = "remote_wallet_key";

    public static final String MULTISIG_CLIENT_KEY_NAME = "remote_client_public_key";
    public static final String MULTISIG_SERVER_KEY_NAME = "remote_server_public_key";

    public static final String BITCOIN_URI_KEY = "BITCOIN_URI_KEY";
    public static final String ERROR_MESSAGE_KEY = "ERROR_MESSAGE_KEY";
    public static final String CLIENT_STARTED_KEY = "CLIENT_STARTED_KEY";

    // communication via broadcast receiver, these are the actions
    public final static String WALLET_READY_ACTION = "WALLET_READY_ACTION";
    public final static String WALLET_PROGRESS_ACTION = "WALLET_PROGRESS_ACTION";
    public final static String WALLET_BALANCE_CHANGED_ACTION = "WALLET_BALANCE_CHANGED_ACTION";
    public final static String WALLET_TRANSACTIONS_CHANGED_ACTION = "WALLET_TRANSACTIONS_CHANGED_ACTION";
    public final static String WALLET_SCRIPTS_CHANGED_ACTION = "WALLET_SCRIPTS_CHANGED_ACTION";
    public static final String WALLET_INSUFFICIENT_BALANCE_ACTION = "WALLET_INSUFFICIENT_BALANCE_ACTION";
    public static final String WALLET_COINS_SENT_ACTION = "WALLET_COINS_SENT_ACTION";
    public static final String WALLET_COINS_RECEIVED_ACTION = "WALLET_COINS_RECEIVED_ACTION";

    public static final String EXCHANGE_RATE_CHANGED_ACTION = "EXCHANGE_RATE_CHANGED_ACTION";
    public static final String START_CLIENTS_ACTION = "START_CLIENTS_ACTION";
    public static final String STOP_CLIENTS_ACTION = "STOP_CLIENTS_ACTION";
    public static final String START_SERVERS_ACTION = "START_SERVERS_ACTION";
    public static final String INSTANT_PAYMENT_SUCCESSFUL_ACTION = "INSTANT_PAYMENT_SUCCESSFUL_ACTION";
    public static final String INSTANT_PAYMENT_FAILED_ACTION = "INSTANT_PAYMENT_FAILED_ACTION";

    //Crypto constants
    public final static String SYMMETRIC_CIPHER_ALGORITH = "AES";
    public final static String SYMMETRIC_CIPHER_MODE = "AES/CFB8/NoPadding";

    // coinblesk server communication
    //public static final String COINBLESK_SERVER_BASE_URL = "http://192.168.1.176:8080/";
    public static String COINBLESK_SERVER_BASE_URL = "http://bitcoin2-test.csg.uzh.ch/coinblesk-server/";
    public static Retrofit RETROFIT = new Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create(SerializeUtils.GSON))
            .baseUrl(Constants.COINBLESK_SERVER_BASE_URL)
            .build();


    public static final UUID SERVICE_UUID = UUID.fromString("f36681f8-c73b-4a02-94a6-a87a8a351dc2");
    public static final int SERVICE_PORT = 60030;
    public static final int BUFFER_SIZE = 1024;

    public static final UUID WRITE_CHARACTERISTIC_UUID = UUID.fromString("f36681f8-c73b-4a02-94a6-a87a8a351dc3");
    public static final UUID READ_CHARACTERISTIC_UUID = UUID.fromString("f36681f8-c73b-4a02-94a6-a87a8a351dc4");
    public static final int SYMMETRIC_KEY_SIZE = 128 / 8;


    public static final int PROTOCOL_VERSION = 3;
}
