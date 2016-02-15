package ch.papers.payments.models;

import org.bitcoinj.core.ECKey;

import ch.papers.objectstorage.models.AbstractUuidObject;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 14/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class ECKeyWrapper extends AbstractUuidObject {
    private final byte[] privateKey;
    private final String name;

    public ECKeyWrapper(byte[] privateKey, String name) {
        this.privateKey = privateKey;
        this.name = name;
    }

    public ECKey getKey() {
        return ECKey.fromPrivate(this.privateKey);
    }

    public String getName() {
        return name;
    }
}
