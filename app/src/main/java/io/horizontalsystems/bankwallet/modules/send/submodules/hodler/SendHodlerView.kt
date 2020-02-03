package io.horizontalsystems.bankwallet.modules.send.submodules.hodler

import androidx.lifecycle.MutableLiveData
import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.hodler.LockTimeInterval

class SendHodlerView : SendHodlerModule.IView {
    val showLockTimeIntervals = SingleLiveEvent<List<SendHodlerModule.LockTimeIntervalViewItem>>()
    val selectedLockTimeInterval = MutableLiveData<LockTimeInterval?>()

    override fun showLockTimeIntervalSelector(items: List<SendHodlerModule.LockTimeIntervalViewItem>) {
        showLockTimeIntervals.postValue(items)
    }

    override fun setSelectedLockTimeInterval(timeInterval: LockTimeInterval?) {
        selectedLockTimeInterval.postValue(timeInterval)
    }
}
