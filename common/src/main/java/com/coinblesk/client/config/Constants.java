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

import com.coinblesk.util.SerializeUtils;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.TestNet3Params;

import java.util.UUID;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public final class Constants {

    private Constants() {
        // prevent instances
    }

    public static NetworkParameters PARAMS = TestNet3Params.get();

    public final static long UNIX_TIME_MONTH = 60 * 60 * 24 * 30;
    public final static int LOCK_TIME_MONTHS = 3;

    /* do not accept addresses that have lock time higher than (now + timespan) */
    public final static long MAX_LOCKTIME_SPAN_SECONDS = 60*60*24 * 365 + 3600;
    /* create new address if current expires in less than this timespan */
    public final static long MIN_LOCKTIME_SPAN_SECONDS = 60*60*24 * 14;

    public final static int LOCK_THRESHOLD = 60 * 4; //https://bitcoin.org/en/developer-reference#block-headers
    public static String WALLET_FILES_PREFIX = "mainnet_wallet_";
    public final static String WALLET_KEY_NAME = "remote_wallet_key";

    public static final String MULTISIG_CLIENT_KEY_NAME = "remote_client_public_key";
    public static final String MULTISIG_SERVER_KEY_NAME = "remote_server_public_key";

    public static final String BITCOIN_URI_KEY = "BITCOIN_URI_KEY";
    public static final String ERROR_MESSAGE_KEY = "ERROR_MESSAGE_KEY";

    // communication via broadcast receiver, these are the actions
    public final static String WALLET_READY_ACTION = "WALLET_READY_ACTION";
    public final static String WALLET_INIT_DONE_ACTION = "WALLET_INIT_DONE_ACTION";
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
    public static final String CLIENT_STARTED_KEY = "CLIENT_STARTED_KEY";

    public static final String INSTANT_PAYMENT_SUCCESSFUL_ACTION = "INSTANT_PAYMENT_SUCCESSFUL_ACTION";
    public static final String INSTANT_PAYMENT_FAILED_ACTION = "INSTANT_PAYMENT_FAILED_ACTION";

    //public static final String PAYMENT_SUCCESSFUL_ACTION = "PAYMENT_SUCCESSFUL_ACTION";
    //public static final String PAYMENT_FAILED_ACTION = "PAYMENT_FAILED_ACTION";

    // Crypto constants
    public final static String SYMMETRIC_CIPHER_ALGORITH = "AES";
    public final static String SYMMETRIC_CIPHER_MODE = "AES/CFB8/NoPadding";
    public final static int SYMMETRIC_KEY_SIZE = 128 / 8;

    // coinblesk server communication
    public static String COINBLESK_SERVER_BASE_URL = "https://bitcoin.csg.uzh.ch/coinblesk-server/";
    public static Retrofit RETROFIT = new Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create(SerializeUtils.GSON))
            .baseUrl(Constants.COINBLESK_SERVER_BASE_URL)
            .build();


    public static final UUID BLUETOOTH_SERVICE_UUID = UUID.fromString("f36681f8-c73b-4a02-94a6-a87a8a351dc2");
    public static final UUID BLUETOOTH_WRITE_CHARACTERISTIC_UUID = UUID.fromString("f36681f8-c73b-4a02-94a6-a87a8a351dc3");
    public static final UUID BLUETOOTH_READ_CHARACTERISTIC_UUID = UUID.fromString("f36681f8-c73b-4a02-94a6-a87a8a351dc4");

    public static final int BUFFER_SIZE = 1024;

    public static final int WIFI_SERVICE_PORT = 60030;

    // version of the communication protocol for client to client communication.
    public static final int CLIENT_COMMUNICATION_PROTOCOL_VERSION = 3;
}
