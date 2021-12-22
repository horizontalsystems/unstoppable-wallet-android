package io.horizontalsystems.bankwallet.modules.send.submodules.hodler

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.stringResId
import io.horizontalsystems.bankwallet.databinding.ViewHodlerInputBinding
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.send.submodules.SendSubmoduleFragment
import io.horizontalsystems.bankwallet.ui.extensions.SelectorDialog
import io.horizontalsystems.bankwallet.ui.extensions.SelectorItem

class SendHodlerFragment(
    private val hodlerModuleDelegate: SendHodlerModule.IHodlerModuleDelegate,
    private val sendHandler: SendModule.ISendHandler
) : SendSubmoduleFragment() {

    private val presenter by activityViewModels<SendHodlerPresenter> {
        SendHodlerModule.Factory(
            sendHandler,
            hodlerModuleDelegate
        )
    }

    private var _binding: ViewHodlerInputBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ViewHodlerInputBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val presenterView = presenter.view as SendHodlerView

        view.setOnClickListener {
            presenter.onClickLockTimeInterval()
        }

        presenterView.selectedLockTimeInterval.observe(viewLifecycleOwner, Observer {
            binding.lockTimeMenu.setText(it.stringResId())
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
