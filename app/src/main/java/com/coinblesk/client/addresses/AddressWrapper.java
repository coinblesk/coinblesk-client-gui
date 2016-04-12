package com.coinblesk.client.addresses;

import ch.papers.objectstorage.models.AbstractUuidObject;
import org.bitcoinj.core.Address;

import java.io.Serializable;

/**
 * Created by Andreas Albrecht on 12.04.16.
 */
public class AddressWrapper extends AbstractUuidObject
                            implements Serializable, Comparable<AddressWrapper> {

    private String addressTitle;
    private String address;

    public AddressWrapper(String addressTitle, String address) {
        super();
        this.addressTitle = addressTitle;
        this.address = address;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAddressTitle() {
        return addressTitle;
    }

    public void setAddressTitle(String addressTitle) {
        this.addressTitle = addressTitle;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) { return false; }
        if (obj == this) { return true; }
        if (!(obj instanceof AddressWrapper)) { return false; }
        AddressWrapper other = (AddressWrapper) obj;

        return com.google.common.base.Objects.equal(addressTitle, other.addressTitle) &&
                com.google.common.base.Objects.equal(address, other.address);
    }

    @Override
    public int hashCode() {
        return com.google.common.base.Objects.hashCode(addressTitle, address);
    }

    @Override
    public String toString() {
        return com.google.common.base.Objects.toStringHelper(this)
                .add("addressTitle", addressTitle)
                .add("address", address)
                .toString();
    }

    @Override
    public int compareTo(AddressWrapper another) {
        int title = addressTitle.compareToIgnoreCase(another.getAddressTitle());
        if (title != 0) { return title; }
        else { return address.compareTo(another.address); }
    }
}
