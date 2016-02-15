package com.uzh.ckiller.coinblesk_client_gui;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.List;

import ch.papers.payments.models.TransactionWrapper;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 15/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class TransactionWrapperRecyclerViewAdapter extends RecyclerView.Adapter<TransactionWrapperRecyclerViewAdapter.ViewHolder> {

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final ImageView mImageView;
        public final TextView mTextViewTitle;
        public final TextView mTextViewDescription;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mImageView = (ImageView) view.findViewById(R.id.transaction_icon);
            mTextViewTitle = (TextView) view.findViewById(R.id.transaction_title);
            mTextViewDescription = (TextView) view.findViewById(R.id.transaction_description);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mTextViewTitle.getText();
        }
    }


    private final TypedValue mTypedValue = new TypedValue();
    private int mBackground;
    private List<TransactionWrapper> mValues;

    public TransactionWrapperRecyclerViewAdapter(Context context, List<TransactionWrapper> items) {
        context.getTheme().resolveAttribute(R.attr.selectableItemBackground, mTypedValue, true);
        mBackground = mTypedValue.resourceId;
        mValues = items;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item, parent, false);
        view.setBackgroundResource(mBackground);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final TransactionWrapper transaction = mValues.get(position);
        holder.mTextViewTitle.setText(transaction.getAmount().toFriendlyString());
        holder.mTextViewDescription.setText(transaction.getTransaction().getUpdateTime()+", conf:"+transaction.getTransaction().getConfidence().getDepthInBlocks());

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context = v.getContext();
                Intent intent = new Intent(context, DummyDataDetailActivity.class);
                intent.putExtra(DummyDataDetailActivity.EXTRA_NAME, transaction.getAmount());
                context.startActivity(intent);
            }
        });

        Glide.with(holder.mImageView.getContext())
                .load(transaction.getAmount().isNegative()?R.drawable.ic_send_arrow_48px:R.drawable.ic_receive_arrow_48px)
                .fitCenter()
                .into(holder.mImageView);
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }
}
