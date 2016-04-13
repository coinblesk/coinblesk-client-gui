package com.coinblesk.client.addresses;

import ch.papers.objectstorage.models.AbstractUuidObject;

import java.io.Serializable;

/**
 * Created by Andreas Albrecht on 12.04.16.
 */
public class AddressWrapper extends AbstractUuidObject
                            implements Serializable, Comparable<AddressWrapper> {

    private String addressLabel;
    private String address;

    public AddressWrapper(String addressLabel, String address) {
        super();
        this.addressLabel = addressLabel;
        this.address = address;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAddressLabel() {
        return addressLabel;
    }

    public void setAddressLabel(String addressLabel) {
        this.addressLabel = addressLabel;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) { return false; }
        if (obj == this) { return true; }
        if (!(obj instanceof AddressWrapper)) { return false; }
        AddressWrapper other = (AddressWrapper) obj;

        return com.google.common.base.Objects.equal(addressLabel, other.addressLabel) &&
                com.google.common.base.Objects.equal(address, other.address);
    }

    @Override
    public int hashCode() {
        return com.google.common.base.Objects.hashCode(addressLabel, address);
    }

    @Override
    public String toString() {
        return com.google.common.base.Objects.toStringHelper(this)
                .add("addressLabel", addressLabel)
                .add("address", address)
                .toString();
    }

    @Override
    public int compareTo(AddressWrapper another) {
        int label = addressLabel.compareToIgnoreCase(another.getAddressLabel());
        if (label != 0) { return label; }
        else { return address.compareTo(another.address); }
    }
}
