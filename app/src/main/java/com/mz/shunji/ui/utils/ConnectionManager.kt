package com.mz.shunji.ui.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.core.content.getSystemService
import com.mz.shunji.preferences.CloudService
import com.mz.shunji.preferences.SyncMode

class ConnectionManager(private val context: Context) {

    fun isConnectionAvailable(syncMode: SyncMode?, cloudService: CloudService?): Boolean {
        if (cloudService != CloudService.NEXTCLOUD) return true
        val connectivityManager = context.getSystemService<ConnectivityManager>() ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> syncMode == SyncMode.ALWAYS
            else -> true
        }
    }
}
