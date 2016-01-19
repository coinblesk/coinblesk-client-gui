package com.uzh.ckiller.coinblesk_client_gui;

/**
 * Created by ckiller on 13/01/16.
 */
public class Dummy {

    private String mAmount;
    private String mDate;
    private String mConfirmations;
    private String mAddress;
    private String mTx;
    private String mUsername;

    public Dummy(String Amount, String Date, String Username, String Confirmations, String Address, String Tx) {
        this.mAmount = Amount;
        this.mDate = Date;
        this.mConfirmations = Confirmations;
        this.mAddress = Address;
        this.mTx = Tx;
        this.mUsername = Username;
    }

    public Dummy(){
        this.mAmount = "InitValue";
        this.mDate = "InitValue";
        this.mConfirmations = "InitValue";
        this.mAddress = "InitValue";
        this.mTx = "InitValue";
        this.mUsername = "InitValue";

    }

    public String getTx() {
        return mTx;
    }

    public String getAmount() {
        return mAmount;
    }

    public String getAddress() {
        return mAddress;
    }

    public String getConfirmations() {
        return mConfirmations;
    }

    public String getDate() {
        return mDate;
    }

    public String getmUsername() {
        return mUsername;
    }
}
