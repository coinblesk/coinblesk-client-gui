package ch.papers.payments.models;

import org.bitcoinj.utils.ExchangeRate;

import ch.papers.objectstorage.models.AbstractUuidObject;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 06/03/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class ExchangeRateWrapper extends AbstractUuidObject {

    private final ExchangeRate exchangeRate;

    public ExchangeRateWrapper(ExchangeRate exchangeRate) {
        this.exchangeRate = exchangeRate;
    }

    public ExchangeRate getExchangeRate() {
        return exchangeRate;
    }
}
