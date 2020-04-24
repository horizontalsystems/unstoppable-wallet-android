package io.horizontalsystems.bankwallet.modules.send.submodules.hodler

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.stringResId
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.send.submodules.SendSubmoduleFragment
import io.horizontalsystems.bankwallet.ui.extensions.SelectorDialog
import io.horizontalsystems.bankwallet.ui.extensions.SelectorItem
import kotlinx.android.synthetic.main.view_hodler_input.*

class SendHodlerFragment(
        private val hodlerModuleDelegate: SendHodlerModule.IHodlerModuleDelegate,
        private val sendHandler: SendModule.ISendHandler
) : SendSubmoduleFragment() {

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

        presenterView.selectedLockTimeInterval.observe(viewLifecycleOwner, Observer {
            lockTimeMenu.setText(it.stringResId())
        })

        presenterView.showLockTimeIntervals.observe(this, Observer { lockTimeIntervals ->
            val selectorItems = lockTimeIntervals.map {
                SelectorItem(getString(it.lockTimeInterval.stringResId()), it.selected)
            }

            SelectorDialog
                    .newInstance(selectorItems, getString(R.string.Send_DialogLockTime), { position ->
                        presenter.onSelectLockTimeInterval(position)
                    })
                    .show(this.parentFragmentManager, "time_intervals_selector")

        })
    }

    override fun init() {
        presenter.onViewDidLoad()
    }

}
