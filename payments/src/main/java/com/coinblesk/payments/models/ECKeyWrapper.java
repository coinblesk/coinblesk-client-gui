package com.coinblesk.payments.models;

import org.bitcoinj.core.ECKey;

import ch.papers.objectstorage.models.AbstractUuidObject;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 14/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class ECKeyWrapper extends AbstractUuidObject {
    private final byte[] keyPayload;
    private final String name;
    private final boolean isPublicOnly;

    public ECKeyWrapper(byte[] keyPayload, String name, boolean isPublicOnly) {
        this.keyPayload = keyPayload;
        this.name = name;
        this.isPublicOnly = isPublicOnly;
    }

    public ECKeyWrapper(byte[] keyPayload, String name) {
        this(keyPayload, name, false);
    }

    public ECKey getKey() {
        if (isPublicOnly){
            return ECKey.fromPublicOnly(keyPayload);
        } else {
            return ECKey.fromPrivate(keyPayload);
        }
    }

    public String getName() {
        return name;
    }
}
