/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package de.buttercookie.simbadroid.service;

import androidx.annotation.MainThread;
import androidx.lifecycle.MutableLiveData;

public class SmbServiceStatusLiveData extends MutableLiveData<SmbService.Status> {
    private static SmbServiceStatusLiveData sInstance;

    @MainThread
    public static SmbServiceStatusLiveData get() {
        if (sInstance == null) {
            sInstance = new SmbServiceStatusLiveData();
        }
        return sInstance;
    }

    private SmbServiceStatusLiveData() {}

}
