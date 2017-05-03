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

import com.coinblesk.client.config.AppConfig;
import com.coinblesk.dto.ForexDTO;
import com.coinblesk.client.CoinbleskWebService;

import org.bitcoinj.utils.ExchangeRate;
import org.bitcoinj.utils.Fiat;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.util.Locale;

import retrofit2.Response;

/**
 * @author Andreas Albrecht
 */
class ExchangeRateFetcher implements Runnable {

    private static final String TAG = ExchangeRateFetcher.class.getName();

    private final WeakReference<WalletService.WalletServiceBinder> weakWalletService;
    private final AppConfig appConfig;
    private final String fiatCurrency;

    public ExchangeRateFetcher(String fiatCurrency, WalletService.WalletServiceBinder walletService) {
        this.fiatCurrency = fiatCurrency;
        this.appConfig = walletService.getAppConfig();
        this.weakWalletService = new WeakReference<>(walletService);
    }

    @Override
    public void run() {
        try {
            CoinbleskWebService service = appConfig.getCoinbleskService();
            Response<ForexDTO> response = service.exchangeRateBTC(fiatCurrency).execute();
            if(!response.isSuccessful()) {
                Log.w(TAG, "Could not fetch exchange rate, code: " + response.code());
                return;
            }
            long fiatValue = response.body().getRate().multiply(BigDecimal.valueOf(10000)).longValue();

            Log.d(TAG, String.format(Locale.US,
                    "Exchange rate - %s, fiatValue: %.2f %s", "USD", fiatValue / 10000.0, fiatCurrency));

            WalletService.WalletServiceBinder walletService = weakWalletService.get();
            if (walletService != null && walletService.getCurrency().equals(fiatCurrency)) {
                ExchangeRate exchangeRate = new ExchangeRate(Fiat.valueOf(fiatCurrency, fiatValue));
                walletService.setExchangeRate(exchangeRate);
            }
        } catch (IOException e) {
            Log.w(TAG, "Could not fetch exchange rate, client or server probably offline: " + e.getMessage());
        } catch (Exception e) {
            Log.w(TAG, "Exception - could not fetch exchange rate: ", e);
        }
    }
}