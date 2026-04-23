package io.horizontalsystems.dapp.walletconnect

import android.content.Context
import androidx.startup.Initializer
import io.horizontalsystems.dapp.core.DAppManager

class DAppServiceInitializer : Initializer<DAppServiceWalletConnect> {
    override fun create(context: Context): DAppServiceWalletConnect {
        val service = DAppServiceWalletConnect()
        DAppManager.registerService(service)
        return service
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
