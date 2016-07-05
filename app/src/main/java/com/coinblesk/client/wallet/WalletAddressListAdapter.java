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

package com.coinblesk.client.wallet;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.coinblesk.bitcoin.TimeLockedAddress;
import com.coinblesk.client.R;
import com.coinblesk.client.utils.UIUtils;
import com.coinblesk.client.ui.widgets.RecyclerView;
import com.coinblesk.client.config.Constants;
import com.coinblesk.util.BitcoinUtils;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Andreas Albrecht
 */
class WalletAddressListAdapter extends RecyclerView.Adapter<WalletAddressListAdapter.ViewHolder> {

    private static final String TAG = WalletAddressListAdapter.class.getName();

    private final NetworkParameters params;
    private final List<TimeLockedAddress> addresses;
    private Map<Address, Coin> balanceByAddress;

    private ItemClickListener listener;

    public WalletAddressListAdapter(NetworkParameters params) {
        this(params, null);
    }

    public WalletAddressListAdapter(NetworkParameters params, List<TimeLockedAddress> addresses) {
        this.params = params;
        this.addresses = (addresses != null)
                ? addresses
                : Collections.synchronizedList(new ArrayList<TimeLockedAddress>());
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.wallet_address_list_item, parent, false);

        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final TimeLockedAddress item = addresses.get(position);
        final Address address = item.getAddress(params);
        final long currentTime = Utils.currentTimeSeconds();
        final Context context = holder.root.getContext();

        holder.address.setText(address.toBase58());

        Coin balance;
        if (balanceByAddress.containsKey(address)) {
            balance = balanceByAddress.get(address);
        } else {
            balance = Coin.ZERO;
        }
        holder.addressBalance.setText(balance.toFriendlyString());

        String lockedUntil = UIUtils.lockedUntilText(item.getLockTime());
        String info = holder.address.getContext().getString(R.string.time_locked_address_info, lockedUntil);
        holder.addressInfo.setText(info);

        Drawable icon;
        icon = (BitcoinUtils.isBeforeLockTime(currentTime, item.getLockTime()))
                ? context.getDrawable(R.drawable.ic_lock_24dp)
                : context.getDrawable(R.drawable.ic_lock_open_24dp);
        holder.icon.setImageDrawable(icon);

        holder.root.setEnabled(balance.isGreaterThan(Coin.ZERO));
    }

    @Override
    public int getItemCount() {
        return addresses.size();
    }

    public List<TimeLockedAddress> getItems() {
        return addresses;
    }

    public void setBalanceByAddress(Map<Address,Coin> balanceByAddress) {
        this.balanceByAddress = balanceByAddress;
    }

    public void setItemClickListener(ItemClickListener listener) {
        this.listener = listener;
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        final View root;
        final TextView address;
        final TextView addressBalance;
        final TextView addressInfo;
        final ImageView icon;

        ViewHolder(View view) {
            super(view);
            root = view;
            address = (TextView) root.findViewById(R.id.wallet_address_list_item_address);
            addressBalance = (TextView) root.findViewById(R.id.wallet_address_list_item_balance);
            addressInfo = (TextView) root.findViewById(R.id.wallet_address_list_item_info);
            icon = (ImageView) root.findViewById(R.id.wallet_address_list_item_icon);

            root.setOnClickListener(this);
            ((ViewGroup) address.getParent()).setOnClickListener(this);
            address.setOnClickListener(this);
            addressBalance.setOnClickListener(this);
            addressInfo.setOnClickListener(this);
            icon.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            Log.d(TAG, "onClick - Address=" + address.getText().toString()
                    + ", Balance=" + addressBalance.getText().toString()
                    + ", Info=" + addressInfo.getText().toString());

            if (listener != null) {
                int pos = getAdapterPosition();
                TimeLockedAddress item = getItems().get(pos);
                listener.onItemClick(item, pos);
            }
        }
    }

    public interface ItemClickListener {
        void onItemClick(TimeLockedAddress item, int position);
    }
}
