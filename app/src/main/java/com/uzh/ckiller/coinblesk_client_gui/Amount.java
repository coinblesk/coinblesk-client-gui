package com.uzh.ckiller.coinblesk_client_gui;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Currency;

/**
 * Created by ckiller on 24/01/16.
 */
public class Amount {

    private String mBitcoinAmount;
    private String mFiatAmount;
    private BigDecimal mExchangeRate;

    public static final String BTC = "BTC";
    public static final String CHF = "CHF";
    public String mLargeCurrency;
    public String mSmallCurrency;

    private static final Amount instance = new Amount();

    private Amount() {
        this.mBitcoinAmount = "";
        this.mFiatAmount = "";
        this.mLargeCurrency = BTC;
        this.mSmallCurrency = CHF;

        //TODO Currency Conversion Factory
        this.mExchangeRate = new BigDecimal(400);
    }

    public static Amount getInstance() {
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

    private void onChangedBitcoinAmount() {
        // If bitcoinAmount is not empty "", or bitcoinamount does not equal "."
        if (!getBitcoinAmount().equalsIgnoreCase("") | !getBitcoinAmount().equalsIgnoreCase(".") | !(getBitcoinAmount().length() == 0)) {
            BigDecimal newFiat = new BigDecimal(getBitcoinAmount()).multiply(getExchangeRate());
            String afterFiatString = String.valueOf(newFiat);
            setFiatAmount(afterFiatString);
        }
    }

    public void setExchangeRate(BigDecimal exchangeRate) {
        this.mExchangeRate = exchangeRate;
    }

    public BigDecimal getExchangeRate() {
        return mExchangeRate;
    }

    public void resetAmount() {
        setBitcoinAmount("");
        setFiatAmount("");
    }

    public void processInput(String input) {

        StringBuilder amount = new StringBuilder(getBitcoinAmount());

        if (getBitcoinAmount().length() > 6) {
            amount.setLength(0);
        }

        switch (input) {
            case "0":
            case "1":
            case "2":
            case "3":
            case "4":
            case "5":
            case "6":
            case "7":
            case "8":
            case "9":
                amount.append(input);
                setBitcoinAmount(amount.toString());
                break;
            case ".":
                if (amount.toString().contains(".") && amount.length() == 1) {
                    amount.setLength(0);
                    setBitcoinAmount(amount.toString());
                    break;
                } else if (getBitcoinAmount().contains(".")) {
                    amount.setLength(0);
                    break;
                } else {
                    amount.append(input);
                    setBitcoinAmount(amount.toString());
                    break;
                }
            case "backspace":
                if (amount.length() > 1) {
                    amount.setLength(amount.length() - 1);
                    setBitcoinAmount(amount.toString());
                } else if (getBitcoinAmount().length() == 1) {
                    amount.setLength(0);
                    resetAmount();
                }

                break;
            case "switch":
                break;
            default:
                break;
        }

    }

    public String getAmountOf(String currency) {
        String result = "";
        switch (currency) {
            case "BTC":
                result = getBitcoinAmount();
                break;
            case "CHF":
                result = getFiatAmount();
                break;
            default:
                break;
        }
        return result;
    }


    public void switchCurrencies() {
        String temp = getLargeCurrency();
        setLargeCurrency(getSmallCurrency());
        setSmallCurrency(temp);
    }

    public String getLargeCurrency() {
        return mLargeCurrency;
    }

    public String getSmallCurrency() {
        return mSmallCurrency;
    }

    private void setSmallCurrency(String value) {
        mSmallCurrency = value;
    }

    private void setLargeCurrency(String value) {
        mLargeCurrency = value;
    }
}