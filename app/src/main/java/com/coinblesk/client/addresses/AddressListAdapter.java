package com.coinblesk.client.addresses;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.coinblesk.client.R;
import com.coinblesk.client.ui.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Andreas Albrecht on 11.04.16.
 */
public class AddressListAdapter extends RecyclerView.Adapter<AddressListAdapter.ViewHolder> {
    private static final String TAG = AddressListAdapter.class.getName();
    private final List<AddressWrapper> addresses;

    private final AddressClickHandler itemClickListener;

    static class ViewHolder extends RecyclerView.ViewHolder
                                    implements View.OnLongClickListener {
        final View addressView;
        final TextView addressLabel;
        final TextView address;
        final AddressClickHandler clickListener;

        ViewHolder(View view, AddressClickHandler clickListener) {
            super(view);
            addressView = view;
            this.clickListener = clickListener;
            addressLabel = (TextView) view.findViewById(R.id.address_label);
            address = (TextView) view.findViewById(R.id.address);

            addressView.setOnLongClickListener(this);
            addressLabel.setOnLongClickListener(this);
            address.setOnLongClickListener(this);
        }

        @Override
        public boolean onLongClick(View v) {
            Log.d(TAG, "onLongClick - Address=" + address.getText().toString() + ", Label=" + addressLabel.getText().toString());
            if (clickListener != null) {
                return clickListener.onItemLongClick(getAdapterPosition());
            }
            return false;
        }

        void update(AddressWrapper addressItem) {
            String label = addressItem.getAddressLabel();
            String base58 = addressItem.getAddress().toString();
            addressLabel.setText(label);
            address.setText(base58);
        }
    }

    public AddressListAdapter(List<AddressWrapper> addresses, AddressClickHandler itemClickListener) {
        this.addresses = addresses != null ? addresses : new ArrayList<AddressWrapper>();
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

        ViewHolder vh = new ViewHolder(v, itemClickListener);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // - get element from the dataset at this position
        // - replace the contents of the view with that element
        AddressWrapper address = addresses.get(position);
        holder.update(address);
    }

    // Return the size of the dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return addresses.size();
    }

    public List<AddressWrapper> getItems() {
        return addresses;
    }

    interface AddressClickHandler {
        boolean onItemLongClick(int itemPosition);
    }
}