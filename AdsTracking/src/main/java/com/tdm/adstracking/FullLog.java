package com.tdm.adstracking;

import android.annotation.SuppressLint;
import android.util.Log;

@SuppressLint("LongLogTag")
public class FullLog {
    private final static boolean DEBUG = false;
    private final static boolean INFOR = true;
    private final static boolean ERROR = true;
    private final static boolean WARNING = true;
    private final static boolean VERBOSE = true;
    private final static String TAG = "<<<--ADS_TRACKING_SDK-->>>";

    public static void d(String message) {
        if (DEBUG) {
            String fullClassName = Thread.currentThread().getStackTrace()[3].getClassName();
            String className = fullClassName.substring(fullClassName.lastIndexOf(".") + 1);
            String methodName = Thread.currentThread().getStackTrace()[3].getMethodName();
            int lineNumber = Thread.currentThread().getStackTrace()[3].getLineNumber();
            Log.d(TAG + ":" + className + "." + methodName + "():" + lineNumber, message);
        }
    }

    public static void i(String message) {
        if (INFOR) {
            String fullClassName = Thread.currentThread().getStackTrace()[3].getClassName();
            String className = fullClassName.substring(fullClassName.lastIndexOf(".") + 1);
            String methodName = Thread.currentThread().getStackTrace()[3].getMethodName();
            int lineNumber = Thread.currentThread().getStackTrace()[3].getLineNumber();
            Log.i(TAG + ":" + className + "." + methodName + "():" + lineNumber, message);
        }
    }

    public static void e(String message) {
        if (ERROR) {
            String fullClassName = Thread.currentThread().getStackTrace()[3].getClassName();
            String className = fullClassName.substring(fullClassName.lastIndexOf(".") + 1);
            String methodName = Thread.currentThread().getStackTrace()[3].getMethodName();
            int lineNumber = Thread.currentThread().getStackTrace()[3].getLineNumber();
            Log.e(TAG + ":" + className + "." + methodName + "():" + lineNumber, message);
        }
    }

    public static void w(String message) {
        if (WARNING) {
            String fullClassName = Thread.currentThread().getStackTrace()[3].getClassName();
            String className = fullClassName.substring(fullClassName.lastIndexOf(".") + 1);
            String methodName = Thread.currentThread().getStackTrace()[3].getMethodName();
            int lineNumber = Thread.currentThread().getStackTrace()[3].getLineNumber();
            Log.w(TAG + ":" + className + "." + methodName + "():" + lineNumber, message);
        }
    }

    public static void v(String message) {
        if (VERBOSE) {
            String fullClassName = Thread.currentThread().getStackTrace()[3].getClassName();
            String className = fullClassName.substring(fullClassName.lastIndexOf(".") + 1);
            String methodName = Thread.currentThread().getStackTrace()[3].getMethodName();
            int lineNumber = Thread.currentThread().getStackTrace()[3].getLineNumber();
            Log.v(TAG + ":" + className + "." + methodName + "():" + lineNumber, message);
        }
    }
}

