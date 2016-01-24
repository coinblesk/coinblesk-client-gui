package com.uzh.ckiller.coinblesk_client_gui;

/**
 * Created by ckiller on 24/01/16.
 */
public class AmountSingleton {

    private static final Object instance = new Object();

    protected AmountSingleton() {
    }

    public static Object getInstance() {
        return instance;
    }



}
