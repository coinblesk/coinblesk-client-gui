package com.coinblesk.payments.models.filters;

import com.coinblesk.payments.models.ECKeyWrapper;

import ch.papers.objectstorage.filters.Filter;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 14/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class ECKeyWrapperFilter implements Filter<ECKeyWrapper> {
    private final String matchName;

    public ECKeyWrapperFilter(String matchName) {
        this.matchName = matchName;
    }

    @Override
    public boolean matches(ECKeyWrapper object) {
        return object.getName().equals(this.matchName);
    }
}
