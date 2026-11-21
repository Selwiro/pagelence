package com.pagelence

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Wi-Fi/ethernet detection for the dictionary download prompt.
 *
 * This only reads the active transport, never traffic contents: the app needs
 * `ACCESS_NETWORK_STATE` for it. Unknown or missing state counts as metered, so
 * the reader gets the "当前非 Wi-Fi" question instead of a silent mobile-data
 * download.
 */
object DictionaryNetwork {
    fun isUnmetered(context: Context): Boolean {
        val manager = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }
}
