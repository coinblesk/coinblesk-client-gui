/*
 * Copyright 2016 The Coinblesk team and the CSG Group at University of Zurich
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.coinblesk.client;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.coinblesk.client.utils.ClientUtils;
import com.coinblesk.client.utils.UIUtils;
import com.coinblesk.client.ui.widgets.RecyclerView;
import com.coinblesk.client.models.TransactionWrapper;

import org.bitcoinj.utils.ExchangeRate;
import org.bitcoinj.utils.Fiat;
import org.bitcoinj.utils.MonetaryFormat;
import org.ocpsoft.prettytime.PrettyTime;

import java.util.List;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 15/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class TransactionWrapperRecyclerViewAdapter extends RecyclerView.Adapter<TransactionWrapperRecyclerViewAdapter.ViewHolder> {
    final PrettyTime prettyTime = new PrettyTime();

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final View view;
        public final ImageView imageViewTxIcon;
        public final TextView textViewTitle;
        public final TextView textViewAmountFiat;
        public final TextView textViewDescription;
        public final ImageView imageViewStatus;

        public ViewHolder(View view) {
            super(view);
            this.view = view;
            this.imageViewTxIcon = (ImageView) view.findViewById(R.id.transaction_icon);
            this.textViewTitle = (TextView) view.findViewById(R.id.transaction_title);
            this.textViewAmountFiat = (TextView) view.findViewById(R.id.transaction_amount_fiat);
            this.textViewDescription = (TextView) view.findViewById(R.id.transaction_description);
            this.imageViewStatus = (ImageView) view.findViewById(R.id.transaction_title_icon_status);
        }
    }

    private final List<TransactionWrapper> transactionWrappers;

    public TransactionWrapperRecyclerViewAdapter(List<TransactionWrapper> items) {
        transactionWrappers = items;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final TransactionWrapper transaction = transactionWrappers.get(position);

        String amount = UIUtils.formatCoin(holder.view.getContext(), transaction.getAmount());
        holder.textViewTitle.setText(amount);
        String amountFiat = amountFiat(transaction);
        holder.textViewAmountFiat.setText(amountFiat);
        holder.textViewDescription.setText(prettyTime.format(transaction.getTransaction().getUpdateTime()));
        holder.imageViewTxIcon.setImageResource(transaction.getAmount().isNegative()
                ? R.drawable.ic_send_arrow_48px
                : R.drawable.ic_receive_arrow_48px);
        holder.imageViewStatus.setImageResource(
                        ClientUtils.isConfidenceReached(transaction)
                        ? R.drawable.ic_checkbox_marked_circle_outline_white_18dp
                        : R.drawable.ic_clock_white_18dp);
        holder.imageViewStatus.setColorFilter(UIUtils.getStatusColorFilter(transaction));
        holder.view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String hash =  transaction.getTransaction().getHashAsString();
                Context context = v.getContext();
                TransactionDetailActivity.openTransaction(context, hash);
            }
        });

    }

    private String amountFiat(TransactionWrapper transaction) {
        ExchangeRate rate = transaction.getTransaction().getExchangeRate();
        if (rate == null) {
            return null;
        }

        Fiat fiat = rate.coinToFiat(transaction.getAmount());
        return MonetaryFormat.FIAT
                .minDecimals(2)
                .repeatOptionalDecimals(0, 0)
                .code(0, fiat.currencyCode)
                .postfixCode()
                .format(fiat)
                .toString();
    }

    @Override
    public int getItemCount() {
        return transactionWrappers.size();
    }

    public List<TransactionWrapper> getItems() {
        return transactionWrappers;
    }

}
