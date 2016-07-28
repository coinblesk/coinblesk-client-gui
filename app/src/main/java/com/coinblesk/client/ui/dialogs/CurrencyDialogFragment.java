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


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.coinblesk.client.R;
import com.coinblesk.client.config.Constants;
import com.coinblesk.client.utils.ClientUtils;
import com.coinblesk.client.utils.SharedPrefUtils;
import com.xeiam.xchange.currency.Currencies;

import org.bitcoinj.uri.BitcoinURI;
import org.bitcoinj.uri.BitcoinURIParseException;
import org.w3c.dom.Text;

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

        final Spinner s = (Spinner) view.findViewById(R.id.currency_spinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, Constants.CURRENCIES);
        s.setAdapter(adapter);
        final TextView usd = (TextView) view.findViewById(R.id.usd);
        final TextView eur = (TextView) view.findViewById(R.id.eur);

        s.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                        @Override
                                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                            activateSpinner(s, usd, eur);
                                        }

                                        @Override
                                        public void onNothingSelected(AdapterView<?> parent) {
                                            //activateSpinner(s, usd, eur);
                                        }
                                    });

                loadSettings();

        final AlertDialog d = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.change_currency)
                .setView(view)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        saveSettings();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .create();
        return d;
    }

    private void activateSpinner(Spinner s, TextView usd, TextView eur) {
        s.setBackgroundResource(R.drawable.cell_shape_currency);
        usd.setBackgroundResource(R.drawable.cell_shape);
        eur.setBackgroundResource(R.drawable.cell_shape);
    }

    private void loadSettings() {

    }

    private void saveSettings() {

    }

    /*@Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_currency_dialog, null);

        return view;
    }*/

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        //activity.getResources().getStringArray(com.coinblesk.client.common.R.array.pref_bitcoin_rep_values)

        //SharedPrefUtils.getBitcoinScalePrefix()
    }
}

