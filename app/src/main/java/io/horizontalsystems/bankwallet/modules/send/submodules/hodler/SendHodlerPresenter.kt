package io.horizontalsystems.bankwallet.modules.send.submodules.hodler

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bitcoincore.core.IPluginData
import io.horizontalsystems.hodler.HodlerData
import io.horizontalsystems.hodler.HodlerPlugin
import io.horizontalsystems.hodler.LockTimeInterval

class SendHodlerPresenter(
        val view: SendHodlerModule.IView,
        private val interactor: SendHodlerModule.IInteractor
) : ViewModel(), SendHodlerModule.IViewDelegate, SendHodlerModule.IHodlerModule {

    var moduleDelegate: SendHodlerModule.IHodlerModuleDelegate? = null

    private var lockTimeIntervals = arrayOf<LockTimeInterval?>()
    private var lockTimeIntervalSelected: LockTimeInterval? = null

    override fun onViewDidLoad() {
        lockTimeIntervals = arrayOf<LockTimeInterval?>(null) + interactor.getLockTimeIntervals()

        view.setSelectedLockTimeInterval(lockTimeIntervalSelected)
    }

    override fun onClickLockTimeInterval() {
        val items = lockTimeIntervals.map {
            SendHodlerModule.LockTimeIntervalViewItem(it, it == lockTimeIntervalSelected)
        }
        view.showLockTimeIntervalSelector(items)
    }

    override fun onSelectLockTimeInterval(position: Int) {
        lockTimeIntervalSelected = lockTimeIntervals[position]

        view.setSelectedLockTimeInterval(lockTimeIntervalSelected)

        moduleDelegate?.onUpdateLockTimeInterval(lockTimeIntervalSelected)
    }

    override fun pluginData(): Map<Byte, IPluginData> {
        return lockTimeIntervalSelected?.let {
            mapOf(HodlerPlugin.id to HodlerData(it))
        } ?: mapOf()
    }
}
