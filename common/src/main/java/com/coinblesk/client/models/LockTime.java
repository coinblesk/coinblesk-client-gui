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

import org.bitcoinj.core.Utils;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.locks.Lock;

/**
 * @author Andreas Albrecht
 */
public class LockTime implements Serializable {
    private long timeCreatedSeconds;
    private long lockTime;

    private LockTime() {
        // use static create() instead
    }

    public long getLockTime() {
        return lockTime;
    }

    public long getTimeCreatedSeconds() {
        return timeCreatedSeconds;
    }

    public static LockTime create(long lockTime) {

        LockTime lt = new LockTime();
        lt.timeCreatedSeconds = Utils.currentTimeSeconds();
        lt.lockTime = lockTime;
        return lt;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) return false;
        if (this == other) return true;
        if (!(other instanceof LockTime)) return false;
        return this.lockTime == ((LockTime)other).lockTime;
    }

    @Override
    public int hashCode() {
        return Long.valueOf(lockTime).hashCode();
    }

    public static class TimeCreatedComparator implements Comparator<LockTime> {
        private final boolean ascending;

        public TimeCreatedComparator(boolean ascending) {
            this.ascending = ascending;
        }

        @Override
        public int compare(LockTime lhs, LockTime rhs) {
            if (lhs.timeCreatedSeconds < rhs.timeCreatedSeconds) {
                return ascending ? -1 : 1;
            } else if (lhs.timeCreatedSeconds > rhs.timeCreatedSeconds) {
                return ascending ? 1 : -1;
            } else {
                return 0;
            }
        }
    }

    @Override
    public String toString() {
        return String.format(Locale.US,
                "%s - lockTime: %d, timeCreatedSeconds: %d",
                LockTime.class.getSimpleName(), lockTime, timeCreatedSeconds);
    }
}
