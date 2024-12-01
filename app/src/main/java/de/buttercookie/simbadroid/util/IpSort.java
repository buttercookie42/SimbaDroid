/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 * This file modified by Jan Henning, 2024
 */

// sortKey() usage based on https://github.com/elastic/elasticsearch/blob/9adfd25a5afdf543041011a2b50b6fc32ad757b0/server/src/main/java/org/elasticsearch/common/network/NetworkUtils.java

package de.buttercookie.simbadroid.util;

import static android.system.OsConstants.IFA_F_TEMPORARY;

import android.net.LinkAddress;

import java.net.InetAddress;
import java.util.Comparator;

public class IpSort {

    private IpSort() {}

    /**
     * Sort <code>LinkAddresses</code> first by type (IPv4 vs. IPv6), then by category (preferring
     * global over link-local addresses and the like), then by lifetime and finally numerically.
     */
    public static class LinkAddressComparator implements Comparator<LinkAddress> {
        private final boolean mPreferIPv6;

        public LinkAddressComparator(boolean preferIPv6) {
            mPreferIPv6 = preferIPv6;
        }

        @Override
        public int compare(LinkAddress left, LinkAddress right) {
            int cmp = Integer.compare(sortKey(left.getAddress(), mPreferIPv6),
                    sortKey(right.getAddress(), mPreferIPv6));
            if (cmp == 0) {
                // Move temporary addresses to the back
                boolean leftTemporary = (left.getFlags() & IFA_F_TEMPORARY) != 0;
                boolean rightTemporary = (right.getFlags() & IFA_F_TEMPORARY) != 0;
                if (leftTemporary != rightTemporary){
                    cmp = rightTemporary ? -1 : 1;
                }
            }
            if (cmp == 0) {
                cmp = compareInetAddress(left.getAddress(), right.getAddress());
            }
            return cmp;
        }
    }

    /**
     * Sort <code>InetAddresses</code> first by type (IPv4 vs. IPv6), then by category (preferring
     * global over link-local addresses and the like) and finally numerically.
     */
    public static class InetAddressComparator extends SimpleInetAddressComparator {
        private final boolean mPreferIPv6;

        public InetAddressComparator(boolean preferIPv6) {
            mPreferIPv6 = preferIPv6;
        }

        @Override
        public int compare(InetAddress left, InetAddress right) {
            int cmp = Integer.compare(sortKey(left, mPreferIPv6),
                    sortKey(right, mPreferIPv6));
            if (cmp == 0) {
                cmp = super.compare(left, right);
            }
            return cmp;
        }
    }

    /**
     * Sort <code>InetAddresses</code> in simple numeric order.
     */
    public static class SimpleInetAddressComparator implements Comparator<InetAddress> {

        @Override
        public int compare(InetAddress left, InetAddress right) {
            return compareInetAddress(left, right);
        }
    }

    private static int sortKey(InetAddress address, boolean prefer_v6) {
        int key = address.getAddress().length;
        if (prefer_v6) {
            key = -key;
        }

        if (address.isAnyLocalAddress()) {
            key += 5;
        }
        if (address.isMulticastAddress()) {
            key += 4;
        }
        if (address.isLoopbackAddress()) {
            key += 3;
        }
        if (address.isLinkLocalAddress()) {
            key += 2;
        }
        if (address.isSiteLocalAddress()) {
            key += 1;
        }

        return key;
    }

    private static int compareInetAddress(InetAddress left, InetAddress right) {
        // Based on https://stackoverflow.com/a/34441987
        byte[] leftOctets = left.getAddress(), rightOctets = right.getAddress();
        int length = Math.max(leftOctets.length, rightOctets.length);
        for (int i = 0; i < length; i++) {
            byte leftOctet = (i >= length - leftOctets.length) ?
                    leftOctets[i - (length - leftOctets.length)] : 0;
            byte rightOctet = (i >= length - rightOctets.length) ?
                    rightOctets[i - (length - rightOctets.length)] : 0;
            if (leftOctet != rightOctet) {
                return (0xff & leftOctet) - (0xff & rightOctet);
            }
        }
        return 0;
    }
}
