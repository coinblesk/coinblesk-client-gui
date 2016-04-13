package com.coinblesk.client.addresses;

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

    private final List<AddressWrapper> addresses;

    private final AddressClickHandler itemClickListener;

    public static class ViewHolder extends RecyclerView.ViewHolder
                                    implements View.OnLongClickListener {
        public final View addressItem;
        public final TextView addressLabel;
        public final TextView address;
        public final AddressClickHandler clickListener;

        public ViewHolder(View view, AddressClickHandler clickListener) {
            super(view);
            addressItem = view;
            this.clickListener = clickListener;
            addressLabel = (TextView) view.findViewById(R.id.address_label);
            address = (TextView) view.findViewById(R.id.address);

            addressItem.setOnLongClickListener(this);
            addressLabel.setOnLongClickListener(this);
            address.setOnLongClickListener(this);
        }

        @Override
        public boolean onLongClick(View v) {
            if (clickListener != null) {
                return clickListener.onItemLongClick(getAdapterPosition());
            }
            return false;
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
        String label = address.getAddressLabel();
        String base58 = address.getAddress().toString();
        holder.addressLabel.setText(label);
        holder.address.setText(base58);
    }

    // Return the size of the dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return addresses.size();
    }

    public List<AddressWrapper> getItems() {
        return addresses;
    }

    public interface AddressClickHandler {
        boolean onItemLongClick(int itemPosition);
    }
}