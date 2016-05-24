/*
 * Copyright 2016 The Coinblesk team and the CSG Group at University of Zurich
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.coinblesk.client.models;

import ch.papers.objectstorage.models.AbstractUuidObject;
import com.coinblesk.bitcoin.TimeLockedAddress;
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
