/*
 * Copyright (C) 2020 The Pixel Experience Project
 *               2021-2024 crDroid Android Project
 *               2024 RisingOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.util.crdroid;

import android.app.ActivityTaskManager;
import android.app.Application;
import android.app.TaskStackListener;
import android.content.ComponentName;
import android.content.Context;
import android.os.Binder;
import android.os.Build;
import android.os.Process;
import android.os.SystemProperties;
import android.util.Log;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class PixelPropsUtils {

    private static final String TAG = PixelPropsUtils.class.getSimpleName();
    private static final boolean DEBUG = true;

    private static final String SPOOF_PIXEL_GMS = "persist.sys.pixelprops.gms";
    private static final String SPOOF_PIXEL_GPHOTOS = "persist.sys.pixelprops.gphotos";
    private static final String SPOOF_PIXEL_NETFLIX = "persist.sys.pixelprops.netflix";
    private static final String ENABLE_PROP_OPTIONS = "persist.sys.pixelprops.all";
    private static final String SPOOF_PIXEL_GOOGLE_APPS = "persist.sys.pixelprops.google";

    private static final Map<String, Object> propsToChangePixel8Pro;
    private static final Map<String, Object> propsToChangePixelXL;

    private static final ComponentName GMS_ADD_ACCOUNT_ACTIVITY = ComponentName.unflattenFromString(
            "com.google.android.gms/.auth.uiflows.minutemaid.MinuteMaidActivity");

    static {
        propsToChangePixel8Pro = new HashMap<>();
        propsToChangePixel8Pro.put("BRAND", "google");
        propsToChangePixel8Pro.put("MANUFACTURER", "Google");
        propsToChangePixel8Pro.put("DEVICE", "husky");
        propsToChangePixel8Pro.put("PRODUCT", "husky");
        propsToChangePixel8Pro.put("MODEL", "Pixel 8 Pro");
        propsToChangePixel8Pro.put("FINGERPRINT", "google/husky/husky:14/AP2A.240705.005.A1/11944170:user/release-keys");
        propsToChangePixelXL = new HashMap<>();
        propsToChangePixelXL.put("BRAND", "google");
        propsToChangePixelXL.put("MANUFACTURER", "Google");
        propsToChangePixelXL.put("DEVICE", "marlin");
        propsToChangePixelXL.put("PRODUCT", "marlin");
        propsToChangePixelXL.put("MODEL", "Pixel XL");
        propsToChangePixelXL.put("FINGERPRINT", "google/marlin/marlin:10/QP1A.191005.007.A3/5972272:user/release-keys");
    }

    private static final int spoof_api_level = SystemProperties.getInt("persist.sys.pihooks.first_api_level", 0);

    public static void setProps(String packageName) {
        if (!SystemProperties.getBoolean(ENABLE_PROP_OPTIONS, true)) {
            return;
        }

        if (packageName == null || packageName.isEmpty()) {
            return;
        }

        boolean isPixelDevice = SystemProperties.get("ro.soc.manufacturer").equalsIgnoreCase("Google");

        Map<String, Object> propsToChange = new HashMap<>();

        final String processName = Application.getProcessName();
        boolean isExcludedProcess = processName != null && (processName.toLowerCase().contains("unstable"));

        String[] packagesToChangePixel8Pro = {
            "com.google.android.apps.aiwallpapers",
            "com.google.android.apps.bard",
            "com.google.android.apps.customization.pixel",
            "com.google.android.apps.emojiwallpaper",
            "com.google.android.apps.nexuslauncher",
            "com.google.android.apps.privacy.wildlife",
            "com.google.android.apps.wallpaper",
            "com.google.android.apps.wallpaper.pixel",
            "com.google.android.gms",
            "com.google.android.googlequicksearchbox",
            "com.google.android.inputmethod.latin",
            "com.google.android.tts",
            "com.google.android.wallpaper.effects"
        };

        if (Arrays.asList(packagesToChangePixel8Pro).contains(packageName) && !isExcludedProcess) {
            if (SystemProperties.getBoolean(SPOOF_PIXEL_GOOGLE_APPS, true)) {
                if (!isPixelDevice) {
                    propsToChange.putAll(propsToChangePixel8Pro);
                }
            }
        }

        if (packageName.equals("com.google.android.apps.photos")) {
            if (SystemProperties.getBoolean(SPOOF_PIXEL_GPHOTOS, true)) {
                propsToChange.putAll(propsToChangePixelXL);
            } else {
                if (!isPixelDevice) {
                    propsToChange.putAll(propsToChangePixel8Pro);
                }
            }
        }

        if (packageName.equals("com.google.android.gms")) {
            if (SystemProperties.getBoolean(SPOOF_PIXEL_GMS, true)) {
                if (shouldTryToCertifyDevice(Application.getProcessName())) {
                    return;
                }
            }
        }

        if (!propsToChange.isEmpty()) {
            if (DEBUG) Log.d(TAG, "Defining props for: " + packageName);
            for (Map.Entry<String, Object> prop : propsToChange.entrySet()) {
                String key = prop.getKey();
                Object value = prop.getValue();
                if (DEBUG) Log.d(TAG, "Defining " + key + " prop for: " + packageName);
                setPropValue(key, value);
            }
        }
    }

    private static void setPropValue(String key, Object value) {
        try {
            dlog("Defining prop " + key + " to " + value.toString());
            Field field = Build.class.getDeclaredField(key);
            field.setAccessible(true);
            field.set(null, value);
            field.setAccessible(false);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Log.e(TAG, "Failed to set prop " + key, e);
        }
    }

    private static void setVersionFieldString(String key, String value) {
        try {
            dlog("Defining version field " + key + " to " + value);
            Field field = Build.VERSION.class.getDeclaredField(key);
            field.setAccessible(true);
            field.set(null, value);
            field.setAccessible(false);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Log.e(TAG, "Failed to set version field " + key, e);
        }
    }

    private static void setVersionFieldInt(String key, int value) {
        try {
            dlog("Defining version field " + key + " to " + String.valueOf(value));
            Field field = Build.VERSION.class.getDeclaredField(key);
            field.setAccessible(true);
            field.set(null, value);
            field.setAccessible(false);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Log.e(TAG, "Failed to set version field int " + key, e);
        }
    }

    private static boolean isGmsAddAccountActivityOnTop() {
        try {
            final ActivityTaskManager.RootTaskInfo focusedTask =
                    ActivityTaskManager.getService().getFocusedRootTaskInfo();
            return focusedTask != null && focusedTask.topActivity != null
                    && focusedTask.topActivity.equals(GMS_ADD_ACCOUNT_ACTIVITY);
        } catch (Exception e) {
            Log.e(TAG, "Unable to get top activity!", e);
        }
        return false;
    }

    private static boolean shouldTryToCertifyDevice(String processName) {
        if (processName == null || !processName.toLowerCase().contains("unstable")) {
            return false;
        }

        setPropValue("TIME", System.currentTimeMillis());

        final boolean was = isGmsAddAccountActivityOnTop();
        final TaskStackListener taskStackListener = new TaskStackListener() {
            @Override
            public void onTaskStackChanged() {
                final boolean is = isGmsAddAccountActivityOnTop();
                if (is ^ was) {
                    dlog("GmsAddAccountActivityOnTop is:" + is + " was:" + was +
                            ", killing myself!"); // process will restart automatically later
                    Process.killProcess(Process.myPid());
                }
            }
        };
        if (!was) {
            dlog("Spoofing build for GMS");
            try {
                ActivityTaskManager.getService().registerTaskStackListener(taskStackListener);
            } catch (Exception e) {
                Log.e(TAG, "Failed to register task stack listener!", e);
            }
            if (spoof_api_level != 0) setVersionFieldInt("DEVICE_INITIAL_SDK_INT", spoof_api_level);
            spoofBuildGms();
            return true;
        } else {
            dlog("Skip spoofing build for GMS, because GmsAddAccountActivityOnTop");
            return false;
        }
    }

    public static boolean shouldBypassTaskPermission(Context context) {
        // GMS doesn't have MANAGE_ACTIVITY_TASKS permission
        final int callingUid = Binder.getCallingUid();
        final int gmsUid;
        try {
            gmsUid = context.getPackageManager().getApplicationInfo("com.google.android.gms", 0).uid;
            dlog("shouldBypassTaskPermission: gmsUid:" + gmsUid + " callingUid:" + callingUid);
        } catch (Exception e) {
            Log.e(TAG, "shouldBypassTaskPermission: unable to get gms uid", e);
            return false;
        }
        return gmsUid == callingUid;
    }

    private static void spoofBuildGms() {
        // Alter build parameters to avoid hardware attestation enforcement
        setPropValue("BRAND", "google");
        setPropValue("MANUFACTURER", "Google");
        setPropValue("DEVICE", "sailfish");
        setPropValue("FINGERPRINT", "google/sailfish/sailfish:8.1.0/OPM1.171019.011/4448085:user/release-keys");
        setPropValue("MODEL", "Pixel");
        setPropValue("PRODUCT", "sailfish");
    }

    private static boolean isCallerSafetyNet() {
        return Arrays.stream(Thread.currentThread().getStackTrace())
                        .anyMatch(elem -> elem.getClassName().toLowerCase()
                            .contains("droidguard"));
    }

    public static void onEngineGetCertificateChain() {
        // Check stack for SafetyNet or Play Integrity
        if (isCallerSafetyNet()) {
            Log.i(TAG, "Blocked key attestation");
            throw new UnsupportedOperationException();
        }
    }

    private static void dlog(String msg) {
        if (DEBUG) Log.d(TAG, msg);
    }
}
