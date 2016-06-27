/*
 * Copyright 2016 The Coinblesk team and the CSG Group at University of Zurich
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package com.coinblesk.payments;

import android.util.Log;

import com.coinblesk.client.config.Constants;
import com.coinblesk.json.ExchangeRateTO;
import com.coinblesk.payments.communications.http.CoinbleskWebService;
import com.coinblesk.util.CoinbleskException;
import com.xeiam.xchange.Exchange;
import com.xeiam.xchange.ExchangeFactory;
import com.xeiam.xchange.bitstamp.BitstampExchange;
import com.xeiam.xchange.currency.CurrencyPair;
import com.xeiam.xchange.dto.marketdata.Ticker;

import org.bitcoinj.utils.ExchangeRate;
import org.bitcoinj.utils.Fiat;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Locale;

import retrofit2.Response;

/**
 * @author Andreas Albrecht
 */
class ExchangeRateFetcher implements Runnable {

    private static final String TAG = ExchangeRateFetcher.class.getName();

    private final WeakReference<WalletService.WalletServiceBinder> weakWalletService;
    private final String fiatCurrency;

    public ExchangeRateFetcher(String fiatCurrency, WalletService.WalletServiceBinder walletService) {
        this.fiatCurrency = fiatCurrency;
        this.weakWalletService = new WeakReference<>(walletService);
    }

    @Override
    public void run() {
        try {
            double conversionRate = fetchConversionRate();
            Ticker ticker = createTicker();
            long currentAsk = ticker.getAsk().longValue();
            long fiatValue = (long) (currentAsk * 10000.0 * (1.0 / conversionRate));
            Log.d(TAG, String.format(Locale.US,
                    "Exchange rate - currentAsk: %d %s, conversionRate: %f, fiatValue: %.2f %s",
                    currentAsk, "USD", conversionRate, fiatValue/10000.0, fiatCurrency));

            WalletService.WalletServiceBinder walletService = weakWalletService.get();
            if (walletService != null && walletService.getCurrency().equals(fiatCurrency)) {
                ExchangeRate exchangeRate = new ExchangeRate(Fiat.valueOf(fiatCurrency, fiatValue));
                walletService.setExchangeRate(exchangeRate);
            }

        } catch (Exception e) {
            Log.w(TAG, "Exception - could not fetch exchange rate: ", e);
        }
    }

    private double fetchConversionRate() throws IOException, CoinbleskException {
        CoinbleskWebService service = Constants.RETROFIT.create(CoinbleskWebService.class);
        Response<ExchangeRateTO> response;
        double conversionRate;
        switch (fiatCurrency) {
            case "USD":
                // usd: get directly
                conversionRate = 1.0;
                break;
            case "CHF":
                response = service.chfToUsd().execute();
                conversionRate = getRate(response);
                break;
            case "EUR":
                response = service.eurToUsd().execute();
                conversionRate = getRate(response);
                break;
            default:
                throw new CoinbleskException("Cannot fetch exchange rate (unknown currency symbol): " + fiatCurrency);
        }

        return conversionRate;
    }

    private double getRate(Response<ExchangeRateTO> response) throws CoinbleskException, IOException {
        if (!response.isSuccessful()) {
            throw new IOException("Could not fetch exchange rate: HTTP " + response.code());
        }

        ExchangeRateTO exchangeTO = response.body();
        if (!exchangeTO.isSuccess()) {
            throw new CoinbleskException("Could not fetch exchange rate, server responded with error type: "
                    + exchangeTO.type().toString());
        }
        return Double.parseDouble(exchangeTO.rate());
    }

    private Ticker createTicker() throws IOException {
        Exchange exchange = ExchangeFactory.INSTANCE.createExchange(BitstampExchange.class.getName());
        Ticker ticker = exchange.getPollingMarketDataService().getTicker(CurrencyPair.BTC_USD);
        return ticker;
    }

}