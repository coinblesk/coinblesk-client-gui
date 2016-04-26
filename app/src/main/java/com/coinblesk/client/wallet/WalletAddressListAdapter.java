package com.coinblesk.client.wallet;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.coinblesk.client.R;
import com.coinblesk.client.ui.widget.RecyclerView;
import com.coinblesk.payments.Constants;
import com.coinblesk.payments.models.AddressWrapper;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by albrecht on 23.04.16.
 */
public class WalletAddressListAdapter extends RecyclerView.Adapter<WalletAddressListAdapter.ViewHolder> {

    private static final String TAG = WalletAddressListAdapter.class.getName();

    private final List<AddressWrapper> addresses;
    private Map<Address, Coin> balanceByAddress;

    public WalletAddressListAdapter() {
        this(null);
    }

    public WalletAddressListAdapter(List<AddressWrapper> addresses) {
        this.addresses = addresses != null ? addresses : new ArrayList<AddressWrapper>();
    }

    public void setBalanceByAddress(Map<Address,Coin> balanceByAddress) {
        this.balanceByAddress = balanceByAddress;
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
        final AddressWrapper item = addresses.get(position);
        final Address address = item.getAddress(Constants.PARAMS);
        holder.address.setText(address.toBase58());
        Coin balance;
        if (balanceByAddress.containsKey(address)) {
            balance = balanceByAddress.get(address);
        } else {
            balance = Coin.ZERO;
        }
        holder.addressBalance.setText(balance.toFriendlyString());
        holder.addressInfo.setText("info");
    }

    @Override
    public int getItemCount() {
        return addresses.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder
                        implements View.OnClickListener {
        final View addressView;
        final TextView address;
        final TextView addressBalance;
        final TextView addressInfo;

        ViewHolder(View view) {
            super(view);
            addressView = view;
            address = (TextView) addressView.findViewById(R.id.wallet_address_list_item_address);
            addressBalance = (TextView) addressView.findViewById(R.id.wallet_address_list_item_balance);
            addressInfo = (TextView) addressView.findViewById(R.id.wallet_address_list_item_info);

            addressView.setOnClickListener(this);
            address.setOnClickListener(this);
            addressBalance.setOnClickListener(this);
            addressInfo.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            Log.d(TAG, "onClick - Address=" + address.getText().toString()
                    + ", Balance=" + addressBalance.getText().toString()
                    + ", Info=" + addressInfo.getText().toString());
        }
    }

    public List<AddressWrapper> getItems() {
        return addresses;
    }
}
