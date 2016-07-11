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

package com.coinblesk.client.config;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Transaction;

import java.util.UUID;

import retrofit2.Retrofit;


/**
 * @author ckiller
 * @author Alessandro De Carli
 * @author Andreas Albrecht
 */
public final class Constants {

    private Constants() { /* prevent instances */ }

    public static final java.util.logging.Level JAVA_LOGGER_LEVEL = java.util.logging.Level.INFO;
    public static final ch.qos.logback.classic.Level LOGBACK_LOGGER_LEVEL = ch.qos.logback.classic.Level.INFO;

    public static final int QR_ACTIVITY_RESULT_REQUEST_CODE = 0x0000c0de; // bottom 16 bits

    public static final String BACKUP_FILE_PREFIX = "coinblesk_wallet_backup";

    public static final String FILE_PROVIDER_AUTHORITY = "com.coinblesk.fileprovider";

    public static final Coin MIN_PAYMENT_REQUEST_AMOUNT = Transaction.MIN_NONDUST_OUTPUT;

    public static final long MIN_CONF_BLOCKS = 1;

    /* do not accept addresses that have lock time higher than (now + timespan) */
    public static final long MAX_LOCKTIME_SPAN_SECONDS = 60*60*24 * 365 + 3600;
    /* create new address if current expires in less than this timespan */
    public static final long MIN_LOCKTIME_SPAN_SECONDS = 60*60*24 * 14;

    public static final String SHARED_PREFERENCES_NAME = "coinblesk_preferences";

    protected static final String WALLET_FILES_PREFIX_TESTNET = "coinblesk_testnet";
    protected static final String WALLET_FILES_PREFIX_MAINNET = "coinblesk_mainnet";
    protected static final String WALLET_FILES_PREFIX_LOCALTESTNET = "coinblesk_localtestnet";

    protected static final String CHECKPOINTS_FILE_NAME_MAINNET = "checkpoints.txt";
    protected static final String CHECKPOINTS_FILE_NAME_TESTNET = "checkpoints-testnet.txt";
    protected static final String CHECKPOINTS_FILE_NAME_LOCALTESTNET = CHECKPOINTS_FILE_NAME_TESTNET;

    // argument: tx hash
    protected static final String URL_BLOCKTRAIL_EXPLORER_MAINNET = "https://www.blocktrail.com/BTC/tx/";
    protected static final String URL_BLOCKTRAIL_EXPLORER_TESTNET = "https://www.blocktrail.com/tBTC/tx/";
    protected static final String URL_BLOCKTRAIL_EXPLORER_LOCALTESTNET = URL_BLOCKTRAIL_EXPLORER_TESTNET;

    public static final String BITCOIN_URI_KEY = "BITCOIN_URI_KEY";
    public static final String ERROR_MESSAGE_KEY = "ERROR_MESSAGE_KEY";

    // communication via broadcast receiver, these are the actions
    public static final String WALLET_DOWNLOAD_DONE_ACTION = "WALLET_DOWNLOAD_DONE_ACTION";
    public static final String WALLET_ERROR_ACTION = "WALLET_ERROR_ACTION";
    public static final String WALLET_INIT_DONE_ACTION = "WALLET_INIT_DONE_ACTION";
    public static final String WALLET_DOWNLOAD_PROGRESS_ACTION = "WALLET_DOWNLOAD_PROGRESS_ACTION";
    public static final String WALLET_BALANCE_CHANGED_ACTION = "WALLET_BALANCE_CHANGED_ACTION";
    public static final String WALLET_CHANGED_ACTION = "WALLET_CHANGED_ACTION";
    public static final String WALLET_TRANSACTION_CONFIDENCE_CHANGED_ACTION = "WALLET_TRANSACTION_CONFIDENCE_CHANGED_ACTION";
    public static final String WALLET_SCRIPTS_CHANGED_ACTION = "WALLET_SCRIPTS_CHANGED_ACTION";
    public static final String WALLET_INSUFFICIENT_BALANCE_ACTION = "WALLET_INSUFFICIENT_BALANCE_ACTION";
    public static final String WALLET_COINS_SENT_ACTION = "WALLET_COINS_SENT_ACTION";
    public static final String WALLET_COINS_RECEIVED_ACTION = "WALLET_COINS_RECEIVED_ACTION";

    public static final String EXCHANGE_RATE_CHANGED_ACTION = "EXCHANGE_RATE_CHANGED_ACTION";
    public static final String START_CLIENTS_ACTION = "START_CLIENTS_ACTION";
    public static final String STOP_CLIENTS_ACTION = "STOP_CLIENTS_ACTION";
    public static final String START_SERVERS_ACTION = "START_SERVERS_ACTION";
    public static final String CLIENT_STARTED_KEY = "CLIENT_STARTED_KEY";

    public static final String INSTANT_PAYMENT_SUCCESSFUL_ACTION = "INSTANT_PAYMENT_SUCCESSFUL_ACTION";
    public static final String INSTANT_PAYMENT_FAILED_ACTION = "INSTANT_PAYMENT_FAILED_ACTION";

    // Crypto constants
    public static final String SYMMETRIC_CIPHER_ALGORITH = "AES";
    public static final String SYMMETRIC_CIPHER_MODE = "AES/CFB8/NoPadding";
    public static final int SYMMETRIC_KEY_SIZE = 128 / 8;


    /* coinblesk server communication */

    //public final static String COINBLESK_SERVER_BASE_URL_PROD = "https://bitcoin.csg.uzh.ch/coinblesk-server/";
    protected static final String COINBLESK_SERVER_BASE_URL_PROD = "https://bitcoin.csg.uzh.ch/coinblesk-server/";
    protected static final String COINBLESK_SERVER_BASE_URL_TEST = "http://bitcoin2-test.csg.uzh.ch/coinblesk-server/";
    //public final static String COINBLESK_SERVER_BASE_URL_TEST = "http://bitcoin2-test.csg.uzh.ch/coinblesk-server/";

    /* client to client communication */
    // version of the communication protocol for client to client communication.
    public static final int CLIENT_COMMUNICATION_PROTOCOL_VERSION = 3;

    public static final UUID BLUETOOTH_SERVICE_UUID = UUID.fromString("f36681f8-c73b-4a02-94a6-a87a8a351dc2");
    public static final UUID BLUETOOTH_WRITE_CHARACTERISTIC_UUID = UUID.fromString("f36681f8-c73b-4a02-94a6-a87a8a351dc3");
    public static final UUID BLUETOOTH_READ_CHARACTERISTIC_UUID = UUID.fromString("f36681f8-c73b-4a02-94a6-a87a8a351dc4");

    public static final int BUFFER_SIZE = 1024;

    public static final int WIFI_SERVICE_PORT = 60030;

    public static final String PAYMENT_REQUEST = "com.coinblesk.client.PAYMENT_REQUEST";
    public static final String START_COINBLESK = "com.coinblesk.client.MAIN";
    public static final String PAYMENT_REQUEST_ADDRESS = "com.coinblesk.client.PAYMENT_REQUEST_ADDRESS";
    public static final String PAYMENT_REQUEST_AMOUNT = "com.coinblesk.client.PAYMENT_REQUEST_AMOUNT";
    public static final String PAYMENT_REQUEST_APPROVED = "com.coinblesk.client.PAYMENT_REQUEST_APPROVED";
}
