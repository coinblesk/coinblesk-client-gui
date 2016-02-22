package ch.papers.payments.models;

import org.bitcoinj.core.ECKey;

import ch.papers.objectstorage.models.AbstractUuidObject;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 14/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class ECKeyWrapper extends AbstractUuidObject {
    private final byte[] keyPayload;
    private final boolean isPublicOnly;
    private final String name;

    public ECKeyWrapper(byte[] keyPayload, String name, boolean isPublicOnly) {
        this.isPublicOnly = isPublicOnly;
        this.keyPayload = keyPayload;
        this.name = name;
    }

    public ECKeyWrapper(byte[] keyPayload, String name) {
        this(keyPayload,name,false);
    }

    public ECKey getKey() {
        if(this.isPublicOnly){
            return ECKey.fromPublicOnly(this.keyPayload);
        } else {
            return ECKey.fromPrivate(this.keyPayload);
        }
    }

    public String getName() {
        return name;
    }
}
