package com.uzh.ckiller.coinblesk_client_gui;

import com.bumptech.glide.load.resource.transcode.BitmapToGlideDrawableTranscoder;

import java.math.BigDecimal;

/**
 * Created by ckiller
 */
public class Amount {

    private String mBitcoinAmount;
    private String mFiatAmount;

    public static final String BTC = "BTC";
    public static final String CHF = "CHF";
    public static final String DEFAULT_AMOUNT = "";
    public static final BigDecimal DEFAULT_EXCHANGE_RATE = new BigDecimal(400);

    public String mLargeCurrency;
    public String mSmallCurrency;

    private static final Amount instance = new Amount();

    private Amount() {
        this.mBitcoinAmount = DEFAULT_AMOUNT;
        this.mFiatAmount = DEFAULT_AMOUNT;
        this.mLargeCurrency = BTC;
        this.mSmallCurrency = CHF;

        //TODO Currency Conversion Factory
    }

    public static Amount getInstance() {
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

    private void onChangedAmount(String code) {
        switch (code) {
            case CHF:
                // TODO Conversion Factory
                BigDecimal bigDecimalBtc = new BigDecimal(getFiatAmount()).divide(DEFAULT_EXCHANGE_RATE);
                setBitcoinAmount(String.valueOf(bigDecimalBtc));
                break;

            case BTC:
                // TODO Conversion Factory
                BigDecimal bigDecimalFiat = new BigDecimal(getBitcoinAmount()).multiply(DEFAULT_EXCHANGE_RATE);
                setFiatAmount(String.valueOf(bigDecimalFiat));
                break;
            default:
                break;

        }

    }


    public void processInput(String value) {

        StringBuilder currentAmount = new StringBuilder(getAmountOf(getLargeCurrencyId()));

        switch (value) {
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
                currentAmount.append(value);
                break;
            case ".":
                if (!currentAmount.toString().contains(".")) {
                    currentAmount.append(value);
                }
                break;
            case "backspace":
                if (currentAmount.length() > 0) {
                    currentAmount.setLength(currentAmount.length() - 1);
                } else if (currentAmount.length() == 0) {
                    currentAmount.setLength(0);
                }
                break;
            case "switch":
                switchCurrencies();
                break;
            default:
                break;
        }

        setAmountOf(currentAmount.toString(), getLargeCurrencyId());
        onChangedAmount(getLargeCurrencyId());
    }

    public String getAmountOf(String currency) {
        String result = "";
        switch (currency) {
            case BTC:
                result = getBitcoinAmount();
                break;
            case CHF:
                result = getFiatAmount();
                break;
            default:
                break;
        }
        return result;
    }

    private void setAmountOf(String value, String currency) {
        switch (currency) {
            case BTC:
                setBitcoinAmount(value);
                break;
            case CHF:
                setFiatAmount(value);
                break;
            default:
                break;
        }
    }


    public void switchCurrencies() {
        // Switch values
        String formerSmall = getAmountOf(getSmallCurrencyId());
        String formerLarge = getAmountOf(getLargeCurrencyId());

        String formerSmallId = getSmallCurrencyId();
        String formerLargeId = getLargeCurrencyId();

        setAmountOf(formerSmall, formerLargeId);
        setAmountOf(formerLarge, formerSmallId);

        // Switch Identifiers
        String temp = getLargeCurrencyId();
        setLargeCurrencyId(getSmallCurrencyId());
        setSmallCurrencyId(temp);
    }

    public String getLargeCurrencyId() {
        return mLargeCurrency;
    }

    public String getSmallCurrencyId() {
        return mSmallCurrency;
    }

    private void setSmallCurrencyId(String value) {
        mSmallCurrency = value;
    }

    private void setLargeCurrencyId(String value) {
        mLargeCurrency = value;
    }
}