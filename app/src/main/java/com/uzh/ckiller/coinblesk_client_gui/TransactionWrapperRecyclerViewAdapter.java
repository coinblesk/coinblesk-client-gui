package com.uzh.ckiller.coinblesk_client_gui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.uzh.ckiller.coinblesk_client_gui.helpers.UIUtils;

import org.ocpsoft.pretty.time.PrettyTime;

import java.util.List;

import ch.papers.payments.models.TransactionWrapper;

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
        public final TextView textViewDescription;
        public final ImageView imageViewStatus;

        public ViewHolder(View view) {
            super(view);
            this.view = view;
            this.imageViewTxIcon = (ImageView) view.findViewById(R.id.transaction_icon);
            this.textViewTitle = (TextView) view.findViewById(R.id.transaction_title);
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
        holder.textViewTitle.setText(transaction.getAmount().toFriendlyString());
        holder.textViewDescription.setText(prettyTime.format(transaction.getTransaction().getUpdateTime()));

        // ColorFilters for Pending
//        <color name="material_light_yellow_900">#f47f1f</color>
//        lineColorCode.setColorFilter(color);

        // TODO this is not the way it works my friend.
        holder.imageViewTxIcon.setImageResource(transaction.getAmount().isNegative() ? R.drawable.ic_send_arrow_48px : R.drawable.ic_receive_arrow_48px);

        holder.imageViewStatus.setImageResource(transaction.getTransaction().getConfidence().getDepthInBlocks() > 0 ? R.drawable.ic_checkbox_marked_circle_outline_white_18dp : R.drawable.ic_clock_white_18dp);
        int colorFilter = UIUtils.getStatusColorFilter(transaction.getTransaction().getConfidence().getDepthInBlocks(), false);
        holder.imageViewStatus.setColorFilter(colorFilter);

        holder.view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context = v.getContext();
                Intent intent = new Intent(context, TransactionDetailActivity.class);
                intent.putExtra(TransactionDetailActivity.EXTRA_NAME, transaction.getTransaction().getHashAsString());
                context.startActivity(intent);
            }
        });

    }

    @Override
    public int getItemCount() {
        return transactionWrappers.size();
    }
}
