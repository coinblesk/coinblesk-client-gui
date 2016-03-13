package com.coinblesk.payments.models;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 13/03/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class ExchangeTO {
    private final Map<String,String> exchangeRates = new HashMap<String,String>();

    public ExchangeTO() {
    }

    public Map<String, String> getExchangeRates() {
        return exchangeRates;
    }
}
