/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.server.wifi.p2p;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.hardware.wifi.V1_0.IWifiIface;
import android.hardware.wifi.V1_0.IWifiP2pIface;
import android.hardware.wifi.V1_0.WifiStatus;
import android.hardware.wifi.V1_0.WifiStatusCode;
import android.net.wifi.nl80211.WifiNl80211Manager;
import android.os.Handler;
import android.os.WorkSource;

import androidx.test.filters.SmallTest;

import com.android.server.wifi.HalDeviceManager;
import com.android.server.wifi.HalDeviceManager.InterfaceDestroyedListener;
import com.android.server.wifi.HalDeviceManager.ManagerStatusListener;
import com.android.server.wifi.PropertyService;
import com.android.server.wifi.WifiBaseTest;
import com.android.server.wifi.WifiMetrics;
import com.android.server.wifi.WifiNative;
import com.android.server.wifi.WifiVendorHal;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for the interface management operations in
 * {@link com.android.server.wifi.p2p.WifiP2pNative}.
 */
@SmallTest
public class WifiP2pNativeInterfaceManagementTest extends WifiBaseTest {
    private static final String P2P_IFACE_NAME = "p2p0";
    private static final String P2P_INTERFACE_PROPERTY = "wifi.direct.interface";
    private static final WorkSource TEST_WS = new WorkSource();

    @Mock private SupplicantP2pIfaceHal mSupplicantP2pIfaceHal;
    @Mock private HalDeviceManager mHalDeviceManager;
    @Mock private PropertyService mPropertyService;
    @Mock private Handler mHandler;
    @Mock private InterfaceDestroyedListener mHalDeviceInterfaceDestroyedListener;
    @Mock private IWifiP2pIface mIWifiP2pIface;
    @Mock private IWifiIface mIWifiIface;
    @Mock private WifiVendorHal mWifiVendorHal;
    @Mock private WifiNl80211Manager mWifiNl80211Manager;
    @Mock private WifiNative mWifiNative;
    @Mock private WifiMetrics mWifiMetrics;
    private WifiP2pNative mWifiP2pNative;
    private WifiStatus mWifiStatusSuccess;
    private ManagerStatusListener mManagerStatusListener;

    /**
     * Sets up for unit test
     */
    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mWifiStatusSuccess = new WifiStatus();
        mWifiStatusSuccess.code = WifiStatusCode.SUCCESS;

        when(mHalDeviceManager.isSupported()).thenReturn(true);
        when(mHalDeviceManager.createP2pIface(any(InterfaceDestroyedListener.class),
                any(Handler.class), any(WorkSource.class))).thenReturn(P2P_IFACE_NAME);
        when(mSupplicantP2pIfaceHal.isInitializationStarted()).thenReturn(true);
        when(mSupplicantP2pIfaceHal.initialize()).thenReturn(true);
        when(mSupplicantP2pIfaceHal.isInitializationComplete()).thenReturn(true);
        when(mSupplicantP2pIfaceHal.setupIface(P2P_IFACE_NAME)).thenReturn(true);
        when(mPropertyService.getString(P2P_INTERFACE_PROPERTY, P2P_IFACE_NAME))
              .thenReturn(P2P_IFACE_NAME);

        mWifiP2pNative = new WifiP2pNative(mWifiNl80211Manager, mWifiNative, mWifiMetrics,
                mWifiVendorHal, mSupplicantP2pIfaceHal, mHalDeviceManager, mPropertyService);
    }

    /**
     * Verifies the setup of a p2p interface.
     */
    @Test
    public void testSetUpInterface() throws Exception {
        assertEquals(P2P_IFACE_NAME,
                mWifiP2pNative.setupInterface(
                        mHalDeviceInterfaceDestroyedListener, mHandler, TEST_WS));

        verify(mHalDeviceManager).createP2pIface(any(InterfaceDestroyedListener.class),
                eq(mHandler), eq(TEST_WS));
        verify(mSupplicantP2pIfaceHal).setupIface(eq(P2P_IFACE_NAME));
    }

    /**
     * Verifies the setup of a p2p interface with no HAL (HIDL) support.
     */
    @Test
    public void testSetUpInterfaceWithNoVendorHal() throws Exception {
        when(mHalDeviceManager.isSupported()).thenReturn(false);

        assertEquals(P2P_IFACE_NAME, mWifiP2pNative.setupInterface(
                mHalDeviceInterfaceDestroyedListener, mHandler, TEST_WS));

        verify(mHalDeviceManager, never())
                .createP2pIface(any(InterfaceDestroyedListener.class), any(Handler.class),
                        any(WorkSource.class));
        verify(mSupplicantP2pIfaceHal).setupIface(eq(P2P_IFACE_NAME));
    }

    /**
     * Verifies the teardown of a p2p interface.
     */
    @Test
    public void testTeardownInterface() throws Exception {
        assertEquals(P2P_IFACE_NAME,
                mWifiP2pNative.setupInterface(mHalDeviceInterfaceDestroyedListener,
                    mHandler, TEST_WS));

        mWifiP2pNative.teardownInterface();

        verify(mHalDeviceManager).removeP2pIface(anyString());
        verify(mSupplicantP2pIfaceHal).teardownIface(eq(P2P_IFACE_NAME));
    }

    /**
     * Verifies the teardown of a p2p interface with no HAL (HIDL) support.
     */
    @Test
    public void testTeardownInterfaceWithNoVendorHal() throws Exception {
        when(mHalDeviceManager.isSupported()).thenReturn(false);

        assertEquals(P2P_IFACE_NAME, mWifiP2pNative.setupInterface(
                mHalDeviceInterfaceDestroyedListener, mHandler, TEST_WS));

        mWifiP2pNative.teardownInterface();

        verify(mHalDeviceManager, never()).removeIface(any(IWifiIface.class));
        verify(mSupplicantP2pIfaceHal).teardownIface(eq(P2P_IFACE_NAME));
    }
}