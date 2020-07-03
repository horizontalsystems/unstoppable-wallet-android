package io.horizontalsystems.bankwallet.modules.guides

sealed class LoadStatus {
    object Initial : LoadStatus()
    object Loading : LoadStatus()
    object Loaded : LoadStatus()
    class Failed(val e: Throwable) : LoadStatus()
}
