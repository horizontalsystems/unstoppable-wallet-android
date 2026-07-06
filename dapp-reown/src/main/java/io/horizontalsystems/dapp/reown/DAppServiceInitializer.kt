package io.horizontalsystems.dapp.reown

import android.content.Context
import androidx.startup.Initializer
import io.horizontalsystems.dapp.core.DAppManager

class DAppServiceInitializer : Initializer<DAppServiceReown> {
    override fun create(context: Context): DAppServiceReown {
        val service = DAppServiceReown()
        DAppManager.registerService(service)
        return service
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
