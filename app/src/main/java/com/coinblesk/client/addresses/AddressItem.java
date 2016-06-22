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

import ch.papers.objectstorage.models.AbstractUuidObject;

import java.io.Serializable;

/**
 * @author Andreas Albrecht
 */
public class AddressItem extends AbstractUuidObject
                            implements Serializable, Comparable<AddressItem> {

    private String addressLabel;
    private String address;

    public AddressItem(String addressLabel, String address) {
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
        if (!(obj instanceof AddressItem)) { return false; }
        AddressItem other = (AddressItem) obj;

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
    public int compareTo(AddressItem another) {
        int label = addressLabel.compareToIgnoreCase(another.getAddressLabel());
        if (label != 0) { return label; }
        else { return address.compareTo(another.address); }
    }
}
