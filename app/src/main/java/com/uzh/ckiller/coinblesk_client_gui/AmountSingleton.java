package com.uzh.ckiller.coinblesk_client_gui;

/**
 * Created by ckiller on 24/01/16.
 */
public class AmountSingleton {
    private String mBitcoinAmount;
    private String mFiatAmount;
    private boolean mDisplayBitcoinMode;

    private static final AmountSingleton instance = new AmountSingleton();

    protected AmountSingleton() {
        this.mBitcoinAmount = "0";
        this.mFiatAmount = "0";
        this.mDisplayBitcoinMode = true;

    }

    public static AmountSingleton getInstance() {
        return instance;
    }

    public String getBitcoinAmount() {
        return mBitcoinAmount;
    }

    public void setBitcoinAmount(String bitcoinAmount) {
        this.mBitcoinAmount = bitcoinAmount;
    }

    public String getFiatAmount() {
        return mFiatAmount;
    }

    public void setFiatAmount(String fiatAmount) {
        this.mFiatAmount = fiatAmount;
    }

    public void setDisplayBitcoinMode(boolean value) {
        this.mDisplayBitcoinMode = value;
    }

    public boolean getDisplayBitcoinMode() {
        return mDisplayBitcoinMode;
    }


}
