package io.horizontalsystems.walletkit.core.debug

import androidx.lifecycle.ViewModel

/**
 * Notified when a screen scoped [ViewModel] has been cleared and is expected to become
 * unreachable.
 *
 * Implemented only in debug builds (see the `debug` source set of `:app`), so that
 * `:walletkit` never depends on LeakCanary and release builds pay a single null check per
 * screen pop.
 */
fun interface ViewModelLeakWatcherService {
    fun onViewModelCleared(viewModel: ViewModel)
}

/**
 * Registry for [ViewModelLeakWatcherService].
 *
 * LeakCanary only watches ViewModels living in an Activity's or a Fragment's ViewModelStore.
 * Screens here keep their ViewModels in per navigation entry stores owned by
 * `SharedViewModelStoreNavEntryDecorator`, which LeakCanary has no way to find, so those
 * stores report cleared ViewModels through this registry instead.
 */
object ViewModelLeakWatcher {

    @Volatile
    private var service: ViewModelLeakWatcherService? = null

    val isEnabled: Boolean
        get() = service != null

    fun registerService(service: ViewModelLeakWatcherService) {
        this.service = service
    }

    /**
     * Must be called *after* [androidx.lifecycle.ViewModelStore.clear], i.e. once every
     * ViewModel passed here has received `onCleared()` and its store has dropped it.
     */
    fun onViewModelsCleared(viewModels: List<ViewModel>) {
        val service = service ?: return
        viewModels.forEach(service::onViewModelCleared)
    }
}
