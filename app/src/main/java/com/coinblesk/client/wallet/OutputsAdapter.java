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

package com.coinblesk.client.wallet;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.coinblesk.client.R;
import com.coinblesk.client.ui.widgets.RecyclerView;

import org.bitcoinj.core.TransactionOutput;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * @author Andreas Albrecht
 */
public class OutputsAdapter extends RecyclerView.Adapter<OutputsAdapter.ViewHolder> {

    private static final String TAG = OutputsAdapter.class.getName();

    private final List<TransactionOutput> outputs;
    private final OutputItemClickListener itemClickListener;

    public OutputsAdapter(List<TransactionOutput> outputs, OutputItemClickListener itemClickListener) {
        this.outputs = outputs != null ? outputs : new ArrayList<TransactionOutput>();
        this.itemClickListener = itemClickListener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.output_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        TransactionOutput item = outputs.get(position);
        holder.update(item);
    }

    @Override
    public int getItemCount() {
        return outputs.size();
    }

    public List<TransactionOutput> getItems() {
        return outputs;
    }

    class ViewHolder extends RecyclerView.ViewHolder
                    implements View.OnClickListener {
        TextView txtOutpoint;
        TextView txtAmount;

        ViewHolder(View itemView) {
            super(itemView);
            txtOutpoint = (TextView) itemView.findViewById(R.id.txtOutpoint);
            txtAmount = (TextView) itemView.findViewById(R.id.txtAmount);

            itemView.setOnClickListener(this);
            ((View) txtOutpoint.getParent()).setOnClickListener(this);
            txtOutpoint.setOnClickListener(this);
            txtAmount.setOnClickListener(this);
        }

        public void update(TransactionOutput item) {
            String outpoint = String.format(Locale.US, "%s [%d]",
                    item.getParentTransactionHash().toString(), item.getIndex());
            String amount = item.getValue().toFriendlyString();
            txtOutpoint.setText(outpoint);
            txtAmount.setText(amount);
        }

        @Override
        public void onClick(View v) {
            if (itemClickListener != null) {
                int pos = getAdapterPosition();
                TransactionOutput item = getItems().get(pos);
                itemClickListener.onItemClick(item, pos);
            }
        }
    }

    public interface OutputItemClickListener {
        void onItemClick(TransactionOutput item, int itemPosition);
    }
}
