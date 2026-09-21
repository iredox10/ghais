package com.ghais.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Android live implementation of the `NetworkMonitor` expect object declared
 * in commonMain.
 *
 * The owner must call [init] once (e.g. `MainActivity.onCreate`) so the
 * singleton can read the initial state and register the callback. Until
 * [init] runs, [isOnline] stays fail-open (`true`).
 *
 * - Initial value: `activeNetworkInfo?.isConnected` at [init] time.
 * - Updates: [ConnectivityManager.NetworkCallback] (`onAvailable` → online;
 *   `onLost`/`onUnavailable` → re-check, so losing one transport while
 *   another remains does not flap to offline).
 */
actual object NetworkMonitor {

    private val _isOnline = MutableStateFlow(true)
    actual val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private var callback: ConnectivityManager.NetworkCallback? = null

    fun init(context: Context) {
        if (callback != null) return
        val cm = context.applicationContext
            .getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return
        _isOnline.value = snapshot(cm)
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _isOnline.value = true
            }

            override fun onLost(network: Network) {
                _isOnline.value = snapshot(cm)
            }

            override fun onUnavailable() {
                _isOnline.value = false
            }
        }
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            cm.registerNetworkCallback(request, cb)
            callback = cb
        } catch (_: Exception) {
            // Callback registration failed; keep the snapshot value.
        }
    }

    @Suppress("DEPRECATION")
    private fun snapshot(cm: ConnectivityManager): Boolean {
        return try {
            cm.activeNetworkInfo?.isConnected == true
        } catch (_: Exception) {
            true
        }
    }
}
