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

package com.coinblesk.client.addresses;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.coinblesk.client.R;
import com.coinblesk.client.models.AddressBookItem;
import com.coinblesk.client.ui.widgets.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Andreas Albrecht
 */
public class AddressListAdapter extends RecyclerView.Adapter<AddressListAdapter.ViewHolder> {
    private static final String TAG = AddressListAdapter.class.getName();

    private final List<AddressBookItem> addresses;

    private AddressItemClickListener itemClickListener;

    public AddressListAdapter(List<AddressBookItem> addresses, AddressItemClickListener itemClickListener) {
        this.addresses = addresses != null ? addresses : new ArrayList<AddressBookItem>();
        this.itemClickListener = itemClickListener;
    }

    public AddressListAdapter() {
        this(null, null);
    }

    // Create new views (invoked by the layout manager)
    @Override
    public AddressListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        View v = LayoutInflater
                    .from(parent.getContext())
                    .inflate(R.layout.address_item, parent, false);

        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // - get element from the dataset at this position
        // - replace the contents of the view with that element
        AddressBookItem address = addresses.get(position);
        holder.update(address);
    }

    // Return the size of the dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return addresses.size();
    }

    public List<AddressBookItem> getItems() {
        return addresses;
    }

    public void setItemClickListener(AddressItemClickListener itemClickListener) {
        this.itemClickListener = itemClickListener;
    }

    public AddressItemClickListener getItemClickListener() {
        return itemClickListener;
    }

    public interface AddressItemClickListener {
        void onItemClick(AddressBookItem item, int itemPosition);
        boolean onItemLongClick(AddressBookItem item, int itemPosition);
    }

    class ViewHolder extends RecyclerView.ViewHolder
            implements View.OnLongClickListener,
            View.OnClickListener {
        final View addressView;
        final TextView addressLabel;
        final TextView address;

        ViewHolder(View view) {
            super(view);
            addressView = view;
            addressLabel = (TextView) view.findViewById(R.id.address_label);
            address = (TextView) view.findViewById(R.id.address);

            addressView.setOnLongClickListener(this);
            addressView.setOnClickListener(this);

            addressLabel.setOnLongClickListener(this);
            addressLabel.setOnClickListener(this);

            address.setOnLongClickListener(this);
            address.setOnClickListener(this);
        }

        @Override
        public boolean onLongClick(View v) {
            Log.d(TAG, "onLongClick - Address=" + address.getText().toString() + ", Label=" + addressLabel.getText().toString());
            if (itemClickListener != null) {
                int pos = getAdapterPosition();
                AddressBookItem item = getItems().get(pos);
                return itemClickListener.onItemLongClick(item, pos);
            }
            return false;
        }

        @Override
        public void onClick(View v) {
            Log.d(TAG, "onClick - Address=" + address.getText().toString() + ", Label=" + addressLabel.getText().toString());
            if (itemClickListener != null) {
                int pos = getAdapterPosition();
                AddressBookItem item = getItems().get(pos);
                itemClickListener.onItemClick(item, pos);
            }
        }

        void update(AddressBookItem addressBookItem) {
            String label = addressBookItem.getAddressLabel();
            String base58 = addressBookItem.getAddress();
            addressLabel.setText(label);
            address.setText(base58);
        }
    }
}