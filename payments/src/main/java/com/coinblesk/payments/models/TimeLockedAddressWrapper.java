package com.coinblesk.payments.models;

import ch.papers.objectstorage.models.AbstractUuidObject;
import com.coinblesk.bitcoin.TimeLockedAddress;
import com.coinblesk.payments.Constants;
import org.bitcoinj.core.Utils;

import java.util.Comparator;

/**
 * @author Andreas Albrecht
 */
public class TimeLockedAddressWrapper extends AbstractUuidObject {
    private long timeCreatedSeconds;
    private ECKeyWrapper clientKey;
    private ECKeyWrapper serverKey;
    private TimeLockedAddress timeLockedAddress;

    private TimeLockedAddressWrapper() {
        // use static create() instead
    }

    public TimeLockedAddress getTimeLockedAddress() {
        return timeLockedAddress;
    }

    public ECKeyWrapper getClientKey() {
        return clientKey;
    }

    public ECKeyWrapper getServerKey() {
        return serverKey;
    }

    public long getTimeCreatedSeconds() {
        return timeCreatedSeconds;
    }

    public static TimeLockedAddressWrapper create(TimeLockedAddress address,
                                                  ECKeyWrapper clientKey,
                                                  ECKeyWrapper serverKey) {

        TimeLockedAddressWrapper addressWrapper = new TimeLockedAddressWrapper();
        addressWrapper.timeCreatedSeconds = Utils.currentTimeSeconds();
        addressWrapper.clientKey = clientKey;
        addressWrapper.serverKey = serverKey;
        addressWrapper.timeLockedAddress = address;
        return addressWrapper;
    }

    public static class TimeCreatedComparator implements Comparator<TimeLockedAddressWrapper> {
        private final boolean ascending;

        public TimeCreatedComparator(boolean ascending) {
            this.ascending = ascending;
        }

        @Override
        public int compare(TimeLockedAddressWrapper lhs, TimeLockedAddressWrapper rhs) {
            if (lhs.timeCreatedSeconds < rhs.timeCreatedSeconds) {
                return ascending ? -1 : 1;
            } else if (lhs.timeCreatedSeconds > rhs.timeCreatedSeconds) {
                return ascending ? 1 : -1;
            } else {
                return 0;
            }
        }
    }
}
