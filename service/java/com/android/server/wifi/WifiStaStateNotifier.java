/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server.wifi;

import android.annotation.NonNull;
import android.net.wifi.IStaStateCallback;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.os.RemoteCallbackList;
import android.util.Log;


public class WifiStaStateNotifier {
    private final RemoteCallbackList<IStaStateCallback> mRegisteredCallbacks;
    private static WifiInjector mWifiInjector;
    private static final String TAG = "WifiStaStateNotifier";
    private static final boolean DEBUG = false;

    WifiStaStateNotifier(WifiInjector wifiInjector) {
	mRegisteredCallbacks = new RemoteCallbackList<>();
        mWifiInjector = wifiInjector;
    }

    public void addCallback(IStaStateCallback callback) {
        if (DEBUG) Log.d(TAG, "addCallback");
        if (mRegisteredCallbacks.getRegisteredCallbackCount() > 0) {
            if (DEBUG) Log.e(TAG, "Failed to add callback, only support single request!");
            return;
        }
        if (!mRegisteredCallbacks.register(callback)) {
            if (DEBUG) Log.e(TAG, "Failed to add callback");
            return;
        }
        mWifiInjector.getActiveModeWarden().registerStaEventCallback();
    }

    public void removeCallback(IStaStateCallback callback) {
        if (DEBUG) Log.d(TAG, "removeCallback");
        mRegisteredCallbacks.unregister(callback);
        mWifiInjector.getActiveModeWarden().unregisterStaEventCallback();
    }

    public void onStaToBeOff() {
        if (DEBUG) Log.d(TAG, "onStaToBeOff");
	int itemCount = mRegisteredCallbacks.beginBroadcast();
        for (int i = 0; i < itemCount; i++) {
            IStaStateCallback callback = mRegisteredCallbacks.getBroadcastItem(i);
            try {
                if (DEBUG) Log.d(TAG, "callback onStaToBeOff");
                callback.onStaToBeOff();
            } catch (RemoteException e) {
                // do nothing
            }
        }
	mRegisteredCallbacks.finishBroadcast();
    }
}
