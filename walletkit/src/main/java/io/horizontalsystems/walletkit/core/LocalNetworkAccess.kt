package io.horizontalsystems.walletkit.core

import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.Inet6Address
import java.net.InetAddress
import java.net.URI

/**
 * Access to servers in the device's local network, such as a self-hosted node.
 *
 * From Android 17 the system drops an app's connections to local network addresses until the user
 * grants it [PERMISSION]. Nothing fails visibly: the connection just times out, so a screen that
 * lets the user point the app at a server of their own has to ask.
 */
object LocalNetworkAccess {

    const val PERMISSION = "android.permission.ACCESS_LOCAL_NETWORK"

    // Android 17, the first version that has the permission.
    const val MIN_SDK = 37

    // The system dialog is shown once per app run however many screens ask: a user who has just
    // declined on one screen must not meet the same dialog on the next.
    @Volatile
    internal var requested = false

    /** True when the host of [url] points into a local network, see [classifyUrl]. */
    suspend fun isLocalUrl(url: String): Boolean = classifyUrl(url) == true

    /** True when [host] points into a local network, see [classifyHost]. */
    suspend fun isLocalHost(host: String): Boolean = classifyHost(host) == true

    /** [classifyHost] for the host of [url]. A url without a host is not local. */
    suspend fun classifyUrl(url: String): Boolean? {
        val host = runCatching { URI(url).host }.getOrNull() ?: return false
        return classifyHost(host)
    }

    /**
     * Whether [host], an IP address or a name, points into a local network, or null when the name
     * could not be resolved and so nothing is known. A name is resolved because a public name with
     * a valid certificate may still lead to a private address. A `.local` name is local by
     * definition, and resolving it is itself a local network request the system may refuse before
     * access is granted.
     */
    suspend fun classifyHost(host: String): Boolean? = withContext(Dispatchers.IO) {
        if (host.trimEnd('.').endsWith(".local", ignoreCase = true)) return@withContext true

        try {
            InetAddress.getAllByName(host.removePrefix("[").removeSuffix("]")).any(::isLocal)
        } catch (_: Exception) {
            null
        }
    }

    fun isLocal(address: InetAddress): Boolean = when {
        address.isLoopbackAddress -> false
        // fe80::/10 and 169.254.0.0/16
        address.isLinkLocalAddress -> true
        // Unique local addresses, fc00::/7. isSiteLocalAddress covers only the retired fec0::/10.
        address is Inet6Address -> (address.address[0].toInt() and 0xfe) == 0xfc
        // 10.0.0.0/8, 172.16.0.0/12 and 192.168.0.0/16
        else -> address.isSiteLocalAddress
    }
}

/**
 * Asks for local network access while [needed] is true and it is not granted yet. The system
 * dialog is shown at most once per app run, so a user who declines is not asked again until the
 * app restarts. [onGranted] runs once when access goes from missing to granted, whether the user
 * allows it in the dialog or in the system settings and then comes back; connections that failed
 * before then have to be retried.
 */
@Composable
fun LocalNetworkPermissionEffect(needed: Boolean, onGranted: () -> Unit) {
    if (Build.VERSION.SDK_INT < LocalNetworkAccess.MIN_SDK) return

    val context = LocalContext.current
    fun isGranted() = ContextCompat.checkSelfPermission(context, LocalNetworkAccess.PERMISSION) ==
        PackageManager.PERMISSION_GRANTED

    // True from the moment access was found missing until onGranted has run for it. The dialog
    // result and the resume that follows it both see the grant; only the first one reports it.
    // Saved, so a grant made in the system settings is still reported after the activity was
    // recreated in the meantime.
    var missing by rememberSaveable { mutableStateOf(false) }
    val reportIfGranted = {
        if (missing && isGranted()) {
            missing = false
            onGranted()
        }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        reportIfGranted()
    }

    LaunchedEffect(needed) {
        if (!needed || isGranted()) return@LaunchedEffect

        missing = true
        if (!LocalNetworkAccess.requested) {
            LocalNetworkAccess.requested = true
            launcher.launch(LocalNetworkAccess.PERMISSION)
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        reportIfGranted()
    }
}
