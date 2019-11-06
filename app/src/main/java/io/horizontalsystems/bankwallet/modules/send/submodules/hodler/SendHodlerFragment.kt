package io.horizontalsystems.bankwallet.modules.send.submodules.hodler

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.send.submodules.SendSubmoduleFragment
import io.horizontalsystems.bankwallet.ui.dialogs.SelectorDialog
import io.horizontalsystems.bankwallet.ui.dialogs.SelectorItem
import io.horizontalsystems.hodler.LockTimeInterval
import kotlinx.android.synthetic.main.view_hodler_input.*

class SendHodlerFragment(
        private val hodlerModuleDelegate: SendHodlerModule.IHodlerModuleDelegate,
        private val sendHandler: SendModule.ISendHandler
) : SendSubmoduleFragment(), SelectorDialog.Listener {

    private lateinit var presenter: SendHodlerPresenter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.view_hodler_input, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        presenter = ViewModelProvider(this, SendHodlerModule.Factory(sendHandler, hodlerModuleDelegate)).get(SendHodlerPresenter::class.java)
        val presenterView = presenter.view as SendHodlerView

        view.setOnClickListener {
            presenter.onClickLockTimeInterval()
        }

        presenterView.selectedLockTimeInterval.observe(this, Observer {
            lockTimeMenu.text = getLockTimeIntervalString(it)
        })

        presenterView.showLockTimeIntervals.observe(this, Observer { lockTimeIntervals ->
            val selectorItems = lockTimeIntervals.map {
                SelectorItem(getLockTimeIntervalString(it.lockTimeInterval), it.selected)
            }

            SelectorDialog
                    .newInstance(this, selectorItems, getString(R.string.Send_DialogLockTime))
                    .show(this.parentFragmentManager, "time_intervals_selector")

        })
    }

    private fun getLockTimeIntervalString(lockTimeInterval: LockTimeInterval?): String {
        return when(lockTimeInterval) {
            LockTimeInterval.hour -> getString(R.string.Send_LockTime_Hour)
            LockTimeInterval.month -> getString(R.string.Send_LockTime_Month)
            LockTimeInterval.halfYear -> getString(R.string.Send_LockTime_HalfYear)
            LockTimeInterval.year -> getString(R.string.Send_LockTime_Year)
            null -> getString(R.string.Send_LockTime_Off)
        }
    }

    override fun onSelectItem(position: Int) {
        presenter.onSelectLockTimeInterval(position)
    }

    override fun init() {
        presenter.onViewDidLoad()
    }

}