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

package com.coinblesk.client.ui.dialogs;


import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.coinblesk.client.R;
import com.coinblesk.client.config.Constants;
import com.coinblesk.client.utils.SharedPrefUtils;


import java.util.Arrays;

/**
 * Created by ckiller
 */

public class CurrencyDialogFragment extends DialogFragment {
    private final static String TAG = CurrencyDialogFragment.class.getName();

    public static DialogFragment newInstance() {
        DialogFragment fragment = new CurrencyDialogFragment();
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View view = inflater.inflate(R.layout.fragment_currency_dialog, null);

        final Spinner spinner = (Spinner) view.findViewById(R.id.currency_spinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(),
                R.layout.spinner_item, Constants.CURRENCIES);
        spinner.setSelection(0, false);
        spinner.setAdapter(adapter);

        final TextView usd = (TextView) view.findViewById(R.id.usd);
        final TextView eur = (TextView) view.findViewById(R.id.eur);



        final TextView btc = (TextView) view.findViewById(R.id.btc);
        final TextView mbtc = (TextView) view.findViewById(R.id.mbtc);
        final TextView ubtc = (TextView) view.findViewById(R.id.ubtc);

        final View[] viewsBTC = new View[]{btc, mbtc, ubtc};
        final View[] viewsFiat = new View[]{usd, eur, spinner};



        final AlertDialog d = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.change_currency)
                .setView(view)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        broadcastExchangeRateChanged(getActivity());
                    }
                })
                .create();


        btc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                for(View v:viewsBTC) {
                    v.setBackgroundResource(R.drawable.cell_shape);
                }
                view.setBackgroundResource(R.drawable.cell_shape_currency);
                SharedPrefUtils.setBitcoinScalePrefix(getActivity(), "BTC");
            }
        });

        mbtc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                for(View v:viewsBTC) {
                    v.setBackgroundResource(R.drawable.cell_shape);
                }
                view.setBackgroundResource(R.drawable.cell_shape_currency);
                SharedPrefUtils.setBitcoinScalePrefix(getActivity(), "mBTC");
            }
        });

        ubtc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                for(View v:viewsBTC) {
                    v.setBackgroundResource(R.drawable.cell_shape);
                }
                view.setBackgroundResource(R.drawable.cell_shape_currency);
                SharedPrefUtils.setBitcoinScalePrefix(getActivity(), "μBTC");
            }
        });

        usd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                for(View v:viewsFiat) {
                    v.setBackgroundResource(R.drawable.cell_shape);
                }
                view.setBackgroundResource(R.drawable.cell_shape_currency);
                SharedPrefUtils.setCurrency(getActivity(), "USD");
            }
        });

        eur.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                for(View v:viewsFiat) {
                    v.setBackgroundResource(R.drawable.cell_shape);
                }
                view.setBackgroundResource(R.drawable.cell_shape_currency);
                SharedPrefUtils.setCurrency(getActivity(), "EUR");
            }
        });

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(position == 0) {
                    return;
                }
                for(View v:viewsFiat) {
                    v.setBackgroundResource(R.drawable.cell_shape);
                }
                spinner.setBackgroundResource(R.drawable.cell_shape_currency);
                SharedPrefUtils.setCurrency(getActivity(), Constants.CURRENCIES[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //activateSpinner(s, usd, eur);
            }
        });

        loadSettings(getActivity(), btc, mbtc, ubtc, usd, eur, spinner);

        return d;
    }

    private void loadSettings(Context context, View... views) {
        for(View view:views) {
            view.setBackgroundResource(R.drawable.cell_shape);
        }

        String bitcoinPrefix = SharedPrefUtils.getBitcoinScalePrefix(context);
        switch (bitcoinPrefix) {
            case "BTC":
                views[0].setBackgroundResource(R.drawable.cell_shape_currency);
                break;
            case "mBTC":
                views[1].setBackgroundResource(R.drawable.cell_shape_currency);
                break;
            case "μBTC":
                views[2].setBackgroundResource(R.drawable.cell_shape_currency);
                break;
        }

        String fiat = SharedPrefUtils.getCurrency(context);
        Spinner spinner = (Spinner) views[5];
        switch (fiat) {
            case "USD":
                views[3].setBackgroundResource(R.drawable.cell_shape_currency);
                spinner.setSelection(0, false);
                break;
            case "EUR":
                views[4].setBackgroundResource(R.drawable.cell_shape_currency);
                spinner.setSelection(0, false);
                break;
            default:
                views[5].setBackgroundResource(R.drawable.cell_shape_currency);
                int pos = Arrays.asList(Constants.CURRENCIES).indexOf(fiat);
                spinner.setSelection(pos);
                break;
        }

    }

    private void broadcastExchangeRateChanged(Context context) {
        Intent exchangeRateChanged = new Intent(Constants.EXCHANGE_RATE_CHANGED_ACTION);
        exchangeRateChanged.putExtra(Constants.EXCHANGE_RATE_SYMBOL,SharedPrefUtils.getCurrency(context));
        LocalBroadcastManager.getInstance(context).sendBroadcast(exchangeRateChanged);
    }
}

