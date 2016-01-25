package com.uzh.ckiller.coinblesk_client_gui;

/**
 * Created by ckiller on 24/01/16.
 */
public class AmountSingleton {
    private String mBitcoinAmount;
    private String mFiatAmount;
    private float mExchangeRate;
    private boolean mDisplayBitcoinMode;

    private static final AmountSingleton instance = new AmountSingleton();

    protected AmountSingleton() {
        this.mBitcoinAmount = "";
        this.mFiatAmount = "";
        this.mDisplayBitcoinMode = true;
        this.mExchangeRate = 400;
    }

    public static AmountSingleton getInstance() {
        return instance;
    }

    public String getBitcoinAmount() {
        return mBitcoinAmount;
    }

    public void setBitcoinAmount(String bitcoinAmount) {
        this.mBitcoinAmount = bitcoinAmount;
        if (!bitcoinAmount.equalsIgnoreCase("")) {
            onChangedBitcoinAmount();
        }
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

    private void onChangedBitcoinAmount() {
        if (!getBitcoinAmount().equalsIgnoreCase("") | !getBitcoinAmount().equalsIgnoreCase(".") | !(getBitcoinAmount().length() == 0)) {
            float btcAmount = Float.parseFloat((getBitcoinAmount()));
            float newFiat = btcAmount * getExchangeRate();
            String afterFiatString = String.valueOf(newFiat);
            setFiatAmount(afterFiatString);
        }
    }

    public void setExchangeRate(int exchangeRate) {
        this.mExchangeRate = exchangeRate;
    }

    public float getExchangeRate() {
        return mExchangeRate;
    }

    public void resetAmount() {
        setBitcoinAmount("");
        setFiatAmount("");
    }


}
