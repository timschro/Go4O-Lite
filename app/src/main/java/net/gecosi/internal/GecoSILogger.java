/**
 * Copyright (c) 2013 Simon Denier
 * Modified for Android: uses android.util.Log instead of file/stdout writers.
 */
package net.gecosi.internal;

import android.util.Log;

public class GecoSILogger {

    private static final String TAG = "GecoSI";

    public static void open() { }

    public static void open(String header) {
        Log.d(TAG, header);
    }

    public static void log(String header, String message) {
        Log.d(TAG, header + " " + message);
    }

    public static void logTime(String message) {
        Log.d(TAG, message);
    }

    public static void stateChanged(String message) {
        Log.d(TAG, "--> " + message);
    }

    public static void info(String message) {
        Log.i(TAG, message);
    }

    public static void debug(String message) {
        Log.d(TAG, message);
    }

    public static void warning(String message) {
        Log.w(TAG, message);
    }

    public static void error(String message) {
        Log.e(TAG, message);
    }

    public static void close() { }
}
