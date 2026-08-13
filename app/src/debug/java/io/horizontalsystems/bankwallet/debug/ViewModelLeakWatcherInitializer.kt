package io.horizontalsystems.bankwallet.debug

import android.content.Context
import androidx.startup.Initializer
import io.horizontalsystems.walletkit.core.debug.ViewModelLeakWatcher
import leakcanary.AppWatcher

/**
 * Makes LeakCanary watch screen scoped ViewModels.
 *
 * LeakCanary only watches ViewModels of an Activity or a Fragment, so with a single Activity and
 * Navigation 3 it sees none of the screen ViewModels: those live in per navigation entry stores
 * owned by `SharedViewModelStoreNavEntryDecorator`. Those stores report cleared ViewModels to
 * `ViewModelLeakWatcher`, and this hands them to LeakCanary.
 */
class ViewModelLeakWatcherInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        ViewModelLeakWatcher.registerService { viewModel ->
            AppWatcher.objectWatcher.expectWeaklyReachable(
                viewModel,
                "${viewModel::class.java.name} received ViewModel#onCleared() callback"
            )
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
