package com.uzh.ckiller.coinblesk_client_gui;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import ch.papers.payments.models.TransactionWrapper;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 15/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class TransactionWrapperRecyclerViewAdapter extends RecyclerView.Adapter<TransactionWrapperRecyclerViewAdapter.ViewHolder> {

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final View view;
        public final ImageView imageView;
        public final TextView textViewTitle;
        public final TextView textViewDescription;

        public ViewHolder(View view) {
            super(view);
            this.view = view;
            this.imageView = (ImageView) view.findViewById(R.id.transaction_icon);
            this.textViewTitle = (TextView) view.findViewById(R.id.transaction_title);
            this.textViewDescription = (TextView) view.findViewById(R.id.transaction_description);
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
        holder.textViewTitle.setText(transaction.getAmount().toFriendlyString());
        holder.textViewDescription.setText(transaction.getTransaction().getUpdateTime()+"");
        holder.imageView.setImageResource(transaction.getAmount().isNegative()?R.drawable.ic_send_arrow_48px:R.drawable.ic_receive_arrow_48px);

        holder.view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context = v.getContext();
                Intent intent = new Intent(context, TransactionDetailActivity.class);
                intent.putExtra(TransactionDetailActivity.EXTRA_NAME, transaction.getUuid());
                context.startActivity(intent);
            }
        });

    }

    @Override
    public int getItemCount() {
        return transactionWrappers.size();
    }
}
