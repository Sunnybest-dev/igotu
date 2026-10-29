package com.igotu.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

data class OpenNetwork(val ssid: String, val signalLevel: Int, val bssid: String)

sealed class WifiScanResult {
    data class Found(val networks: List<OpenNetwork>) : WifiScanResult()
    object NoNetworksFound : WifiScanResult()
    object ScanFailed : WifiScanResult()
    /**
     * On Android 10+ apps cannot enable WiFi silently.
     * We can only open the system panel and let the user connect.
     */
    object CannotEnableSilently : WifiScanResult()
}

@Singleton
class WifiConnectivityManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val wifiManager = context.applicationContext
        .getSystemService(WifiManager::class.java)

    private val _openNetworks = MutableStateFlow<List<OpenNetwork>>(emptyList())
    val openNetworks: StateFlow<List<OpenNetwork>> = _openNetworks

    /**
     * Scan for open (no password) WiFi networks nearby.
     * Returns results within 10 seconds or times out.
     *
     * NOTE: Android 10+ throttles WiFi scans to 4 per 2 minutes in the background.
     * This is an OS restriction — IGot-U cannot bypass it.
     */
    suspend fun scanForOpenNetworks(): WifiScanResult {
        if (!wifiManager.isWifiEnabled) {
            // Cannot enable WiFi silently on Android 10+
            // Open system WiFi settings panel instead
            return WifiScanResult.CannotEnableSilently
        }

        return withTimeoutOrNull(10_000) {
            suspendCancellableCoroutine { cont ->
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(ctx: Context, intent: Intent) {
                        context.unregisterReceiver(this)
                        val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
                        if (!success && wifiManager.scanResults.isEmpty()) {
                            cont.resume(WifiScanResult.ScanFailed)
                            return
                        }
                        val open = wifiManager.scanResults
                            .filter { it.capabilities.contains("[ESS]") && !it.capabilities.contains("WPA") && !it.capabilities.contains("WEP") }
                            .map { OpenNetwork(it.SSID, it.level, it.BSSID) }
                            .sortedByDescending { it.signalLevel }
                        _openNetworks.value = open
                        cont.resume(
                            if (open.isEmpty()) WifiScanResult.NoNetworksFound
                            else WifiScanResult.Found(open)
                        )
                    }
                }
                context.registerReceiver(receiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
                cont.invokeOnCancellation { runCatching { context.unregisterReceiver(receiver) } }

                @Suppress("DEPRECATION")
                wifiManager.startScan() // deprecated API 28 but still functional; replacement requires user interaction
            }
        } ?: WifiScanResult.ScanFailed
    }

    /**
     * Opens the system WiFi settings panel.
     * On Android 10+ this is the only way to let the user enable WiFi or
     * connect to a network — apps cannot do it silently.
     */
    fun openSystemWifiPanel(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= 29) {
            // Android 10+: opens the WiFi settings panel directly
            Intent(Settings.Panel.ACTION_WIFI)
        } else {
            Intent(Settings.ACTION_WIFI_SETTINGS)
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    fun isWifiEnabled(): Boolean = wifiManager.isWifiEnabled
}
