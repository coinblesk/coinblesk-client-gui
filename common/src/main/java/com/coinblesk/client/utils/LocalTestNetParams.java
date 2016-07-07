package com.coinblesk.client.utils;

import org.bitcoinj.params.TestNet3Params;

/**
 * @author Andreas Albrecht
 */
public class LocalTestNetParams extends TestNet3Params {
    public static final String ID_LOCAL_TESTNET = "com.coinblesk.localtest";
    private static LocalTestNetParams instance;

    public LocalTestNetParams() {
        super();
        id = ID_LOCAL_TESTNET;
    }

    public static synchronized LocalTestNetParams get() {
        if (instance == null) {
            instance = new LocalTestNetParams();
        }
        return instance;
    }
}
