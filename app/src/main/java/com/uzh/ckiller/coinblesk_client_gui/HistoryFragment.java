package com.uzh.ckiller.coinblesk_client_gui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by ckiller
 */

public class HistoryFragment extends android.support.v4.app.Fragment {

    private FragmentActivity faActivity;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        RecyclerView rv = (RecyclerView) inflater.inflate(
                R.layout.fragment_history, container, false);
        setupRecyclerView(rv);
        faActivity = (FragmentActivity) this.getActivity();
        return rv;
    }

    private void setupRecyclerView(RecyclerView recyclerView) {
        recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));
        recyclerView.setAdapter(new SimpleStringRecyclerViewAdapter(getActivity(),
                getRandomSublist(DummyData.sDummyDataStrings, 30)));
    }

    private List<Dummy> getRandomSublist(Dummy[] array, int amount) {
        ArrayList<Dummy> list = new ArrayList<>(amount);
        Random random = new Random();
        while (list.size() < amount) {
            list.add(array[random.nextInt(array.length)]);
        }
        return list;
    }

    public static class SimpleStringRecyclerViewAdapter
            extends RecyclerView.Adapter<SimpleStringRecyclerViewAdapter.ViewHolder> {

        private final TypedValue mTypedValue = new TypedValue();
        private int mBackground;
        private List<Dummy> mValues;

        public static class ViewHolder extends RecyclerView.ViewHolder {
            public Dummy mBoundDummy;

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

        public Dummy getValueAt(int position) {
            return mValues.get(position);
        }

        public SimpleStringRecyclerViewAdapter(Context context, List<Dummy> items) {
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
            holder.mBoundDummy = mValues.get(position);
            holder.mTextViewTitle.setText(mValues.get(position).getAmount());
            holder.mTextViewDescription.setText((mValues.get(position).getDate() + " " + mValues.get(position).getmUsername()));

            holder.mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Context context = v.getContext();

                    Intent intent = new Intent(context, DummyDataDetailActivity.class);
                    intent.putExtra(DummyDataDetailActivity.EXTRA_NAME, holder.mBoundDummy.getAmount());

                    context.startActivity(intent);
                }
            });

            Glide.with(holder.mImageView.getContext())
                    .load(DummyData.getRandomDummyDrawable())
                    .fitCenter()
                    .into(holder.mImageView);
        }

        @Override
        public int getItemCount() {
            return mValues.size();
        }
    }
}
