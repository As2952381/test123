package com.wp.redpacket.util;

import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.content.Context;
import android.os.Build;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * NetworkUtil 单元测试
 *
 * 测试 WiFi 检测、网络条件判断
 */
@RunWith(MockitoJUnitRunner.class)
public class NetworkUtilTest {

    @Mock
    private Context context;

    @Mock
    private ConnectivityManager connectivityManager;

    @Mock
    private Network network;

    @Mock
    private NetworkCapabilities networkCapabilities;

    @Mock
    private ConfigHelper configHelper;

    @Before
    public void setUp() {
        when(context.getSystemService(Context.CONNECTIVITY_SERVICE))
                .thenReturn(connectivityManager);
    }

    // ==================== isWifiConnected 测试 ====================

    @Test
    public void testIsWifiConnected_NullContext() {
        assertTrue("Context 为空时应默认放行", NetworkUtil.isWifiConnected(null));
    }

    @Test
    public void testIsWifiConnected_NoActiveNetwork() {
        when(connectivityManager.getActiveNetwork()).thenReturn(null);
        assertFalse("无活跃网络应返回 false", NetworkUtil.isWifiConnected(context));
    }

    @Test
    public void testIsWifiConnected_NoCapabilities() {
        when(connectivityManager.getActiveNetwork()).thenReturn(network);
        when(connectivityManager.getNetworkCapabilities(network)).thenReturn(null);
        assertFalse("无网络能力信息应返回 false", NetworkUtil.isWifiConnected(context));
    }

    @Test
    public void testIsWifiConnected_WiFiAvailable() {
        when(connectivityManager.getActiveNetwork()).thenReturn(network);
        when(connectivityManager.getNetworkCapabilities(network)).thenReturn(networkCapabilities);
        when(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)).thenReturn(true);

        assertTrue("WiFi 连接应返回 true", NetworkUtil.isWifiConnected(context));
    }

    @Test
    public void testIsWifiConnected_CellularOnly() {
        when(connectivityManager.getActiveNetwork()).thenReturn(network);
        when(connectivityManager.getNetworkCapabilities(network)).thenReturn(networkCapabilities);
        when(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)).thenReturn(false);
        when(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)).thenReturn(false);

        assertFalse("仅移动数据应返回 false", NetworkUtil.isWifiConnected(context));
    }

    @Test
    public void testIsWifiConnected_SecurityException() {
        when(connectivityManager.getActiveNetwork()).thenThrow(new SecurityException("无权限"));

        // 权限不足时默认放行
        assertTrue("权限不足时应默认放行", NetworkUtil.isWifiConnected(context));
    }

    // ==================== shouldProceed 测试 ====================

    @Test
    public void testShouldProceed_WifiOnlyDisabled() {
        when(configHelper.isWifiOnly()).thenReturn(false);

        assertTrue("未开启仅WiFi模式应直接放行",
                NetworkUtil.shouldProceed(context, configHelper));
        // 不应访问网络状态
        verify(configHelper, never()).isEnabled();
    }

    @Test
    public void testShouldProceed_WifiOnlyEnabled_WifiConnected() {
        when(configHelper.isWifiOnly()).thenReturn(true);
        when(connectivityManager.getActiveNetwork()).thenReturn(network);
        when(connectivityManager.getNetworkCapabilities(network)).thenReturn(networkCapabilities);
        when(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)).thenReturn(true);

        assertTrue("仅WiFi模式+WiFi连接应放行",
                NetworkUtil.shouldProceed(context, configHelper));
    }

    @Test
    public void testShouldProceed_WifiOnlyEnabled_NoWifi() {
        when(configHelper.isWifiOnly()).thenReturn(true);
        when(connectivityManager.getActiveNetwork()).thenReturn(network);
        when(connectivityManager.getNetworkCapabilities(network)).thenReturn(networkCapabilities);
        when(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)).thenReturn(false);
        when(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)).thenReturn(false);

        assertFalse("仅WiFi模式+移动数据应阻止",
                NetworkUtil.shouldProceed(context, configHelper));
    }
}
