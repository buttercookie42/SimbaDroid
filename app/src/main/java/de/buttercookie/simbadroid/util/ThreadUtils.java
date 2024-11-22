/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

// Imported from https://hg.mozilla.org/releases/mozilla-esr68/file/bb3f440689f395ed15d6a6177650c0b0ad37146a/mobile/android/geckoview/src/main/java/org/mozilla/gecko/util/ThreadUtils.java

package de.buttercookie.simbadroid.util;

import java.util.Map;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public final class ThreadUtils {
    private static final String LOGTAG = "ThreadUtils";

    /**
     * Controls the action taken when a method like
     * {@link ThreadUtils#assertOnUiThread(AssertBehavior)} detects a problem.
     */
    public enum AssertBehavior {
        NONE,
        THROW,
    }

    private static final Thread sUiThread = Looper.getMainLooper().getThread();
    private static final Handler sUiHandler = new Handler(Looper.getMainLooper());

    private static volatile Thread sBackgroundThread;

    public static void dumpAllStackTraces() {
        Log.w(LOGTAG, "Dumping ALL the threads!");
        Map<Thread, StackTraceElement[]> allStacks = Thread.getAllStackTraces();
        for (Thread t : allStacks.keySet()) {
            Log.w(LOGTAG, t.toString());
            for (StackTraceElement ste : allStacks.get(t)) {
                Log.w(LOGTAG, ste.toString());
            }
            Log.w(LOGTAG, "----");
        }
    }

    public static void setBackgroundThread(final Thread thread) {
        sBackgroundThread = thread;
    }

    public static Thread getUiThread() {
        return sUiThread;
    }

    public static Handler getUiHandler() {
        return sUiHandler;
    }

    public static void postToUiThread(final Runnable runnable) {
        sUiHandler.post(runnable);
    }

    public static void postDelayedToUiThread(final Runnable runnable, final long timeout) {
        sUiHandler.postDelayed(runnable, timeout);
    }

    public static void removeCallbacksFromUiThread(final Runnable runnable) {
        sUiHandler.removeCallbacks(runnable);
    }

    public static Thread getBackgroundThread() {
        return sBackgroundThread;
    }

    public static Handler getBackgroundHandler() {
        return BackgroundThread.getHandler();
    }

    public static void postToBackgroundThread(final Runnable runnable) {
        BackgroundThread.post(runnable);
    }

    public static void postDelayedToBackgroundThread(final Runnable runnable, final long timeout) {
        BackgroundThread.postDelayed(runnable, timeout);
    }

    public static void assertOnUiThread(final AssertBehavior assertBehavior) {
        assertOnThread(getUiThread(), assertBehavior);
    }

    public static void assertOnUiThread() {
        assertOnThread(getUiThread(), AssertBehavior.THROW);
    }

    public static void assertNotOnUiThread() {
        assertNotOnThread(getUiThread(), AssertBehavior.THROW);
    }

    public static void assertOnBackgroundThread() {
        assertOnThread(getBackgroundThread(), AssertBehavior.THROW);
    }

    public static void assertOnThread(final Thread expectedThread) {
        assertOnThread(expectedThread, AssertBehavior.THROW);
    }

    public static void assertOnThread(final Thread expectedThread, final AssertBehavior behavior) {
        assertOnThreadComparison(expectedThread, behavior, true);
    }

    public static void assertNotOnThread(final Thread expectedThread,
                                         final AssertBehavior behavior) {
        assertOnThreadComparison(expectedThread, behavior, false);
    }

    private static void assertOnThreadComparison(final Thread expectedThread,
                                                 final AssertBehavior behavior,
                                                 final boolean expected) {
        final Thread currentThread = Thread.currentThread();
        final long currentThreadId = currentThread.getId();
        final long expectedThreadId = expectedThread.getId();

        if ((currentThreadId == expectedThreadId) == expected) {
            return;
        }

        final String message;
        if (expected) {
            message = "Expected thread " + expectedThreadId +
                    " (\"" + expectedThread.getName() + "\"), but running on thread " +
                    currentThreadId + " (\"" + currentThread.getName() + "\")";
        } else {
            message = "Expected anything but " + expectedThreadId +
                    " (\"" + expectedThread.getName() + "\"), but running there.";
        }

        final IllegalThreadStateException e = new IllegalThreadStateException(message);

        switch (behavior) {
            case THROW:
                throw e;
            default:
                Log.e(LOGTAG, "Method called on wrong thread!", e);
        }
    }

    public static boolean isOnUiThread() {
        return isOnThread(getUiThread());
    }

    public static boolean isOnBackgroundThread() {
        if (sBackgroundThread == null) {
            return false;
        }

        return isOnThread(sBackgroundThread);
    }

    public static boolean isOnThread(final Thread thread) {
        return (Thread.currentThread().getId() == thread.getId());
    }
}