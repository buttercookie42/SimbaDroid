/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

// Imported from https://hg.mozilla.org/releases/mozilla-esr68/file/bb3f440689f395ed15d6a6177650c0b0ad37146a/mobile/android/geckoview/src/main/java/org/mozilla/gecko/util/GeckoBackgroundThread.java

package de.buttercookie.simbadroid.util;

import android.os.Handler;
import android.os.Looper;

import java.util.Objects;

final class BackgroundThread extends Thread {
    private static final String LOOPER_NAME = "BackgroundThread";

    // Guarded by 'BackgroundThread.class'.
    private static Handler handler;
    private static Thread thread;

    // The initial Runnable to run on the new thread. Its purpose
    // is to avoid us having to wait for the new thread to start.
    private Runnable mInitialRunnable;

    // Singleton, so private constructor.
    private BackgroundThread(final Runnable initialRunnable) {
        mInitialRunnable = initialRunnable;
    }

    @Override
    public void run() {
        setName(LOOPER_NAME);
        Looper.prepare();

        synchronized (BackgroundThread.class) {
            handler = new Handler(Objects.requireNonNull(Looper.myLooper()));
            BackgroundThread.class.notifyAll();
        }

        if (mInitialRunnable != null) {
            mInitialRunnable.run();
            mInitialRunnable = null;
        }

        Looper.loop();
    }

    private static void startThread(final Runnable initialRunnable) {
        thread = new BackgroundThread(initialRunnable);
        ThreadUtils.setBackgroundThread(thread);

        thread.setDaemon(true);
        thread.start();
    }

    // Get a Handler for a looper thread, or create one if it doesn't yet exist.
    /*package*/ static synchronized Handler getHandler() {
        if (thread == null) {
            startThread(null);
        }

        while (handler == null) {
            try {
                BackgroundThread.class.wait();
            } catch (final InterruptedException e) {
            }
        }
        return handler;
    }

    /*package*/ static synchronized void post(final Runnable runnable) {
        if (thread == null) {
            startThread(runnable);
            return;
        }
        getHandler().post(runnable);
    }

    /*package*/ static void postDelayed(final Runnable runnable, final long timeout) {
        getHandler().postDelayed(runnable, timeout);
    }
}