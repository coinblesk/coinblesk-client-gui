package com.coinblesk.client.wallet;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.coinblesk.client.R;
import com.coinblesk.client.helpers.UIUtils;
import com.coinblesk.client.ui.widget.RecyclerView;
import com.coinblesk.payments.Constants;
import com.coinblesk.payments.models.TimeLockedAddressWrapper;
import com.coinblesk.util.BitcoinUtils;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
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

    private final List<TimeLockedAddressWrapper> addresses;
    private Map<Address, Coin> balanceByAddress;

    public WalletAddressListAdapter() {
        this(null);
    }

    public WalletAddressListAdapter(List<TimeLockedAddressWrapper> addresses) {
        this.addresses = (addresses != null)
                ? addresses
                : Collections.synchronizedList(new ArrayList<TimeLockedAddressWrapper>());
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
        final TimeLockedAddressWrapper item = addresses.get(position);
        final Address address = item.getTimeLockedAddress().getAddress(Constants.PARAMS);
        final long lockTime = item.getTimeLockedAddress().getLockTime();
        final long currentTime = Utils.currentTimeSeconds();
        final Context context = holder.addressView.getContext();

        holder.address.setText(address.toBase58());

        Coin balance;
        if (balanceByAddress.containsKey(address)) {
            balance = balanceByAddress.get(address);
        } else {
            balance = Coin.ZERO;
        }
        holder.addressBalance.setText(balance.toFriendlyString());

        String lockedUntil = UIUtils.lockedUntilText(lockTime);
        String info = holder.address.getContext().getString(R.string.time_locked_address_info, lockedUntil);
        holder.addressInfo.setText(info);

        Drawable icon;
        icon = (BitcoinUtils.isBeforeLockTime(currentTime, lockTime))
                ? context.getDrawable(R.drawable.ic_lock_24dp)
                : context.getDrawable(R.drawable.ic_lock_open_24dp);
        holder.icon.setImageDrawable(icon);

        holder.addressView.setEnabled(balance.isGreaterThan(Coin.ZERO));
    }

    @Override
    public int getItemCount() {
        return addresses.size();
    }

    public List<TimeLockedAddressWrapper> getItems() {
        return addresses;
    }

    public void setBalanceByAddress(Map<Address,Coin> balanceByAddress) {
        this.balanceByAddress = balanceByAddress;
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        final View addressView;
        final TextView address;
        final TextView addressBalance;
        final TextView addressInfo;
        final ImageView icon;

        ViewHolder(View view) {
            super(view);
            addressView = view;
            address = (TextView) addressView.findViewById(R.id.wallet_address_list_item_address);
            addressBalance = (TextView) addressView.findViewById(R.id.wallet_address_list_item_balance);
            addressInfo = (TextView) addressView.findViewById(R.id.wallet_address_list_item_info);
            icon = (ImageView) addressView.findViewById(R.id.wallet_address_list_item_icon);

            addressView.setOnClickListener(this);
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
        }
    }
}
