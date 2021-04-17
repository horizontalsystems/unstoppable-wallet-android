package io.horizontalsystems.bankwallet.modules.showkey

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.backup.words.BackupWordsModule
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.getNavigationResult
import io.horizontalsystems.pin.PinInteractionType
import io.horizontalsystems.pin.PinModule
import kotlinx.android.synthetic.main.fragment_show_key.*

class ShowKeyFragment : BaseFragment() {
    private val viewModel by viewModels<ShowKeyViewModel> { ShowKeyModule.Factory(arguments?.getParcelable(ShowKeyModule.ACCOUNT)!!) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_show_key, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        buttonShow.setOnClickListener {
            viewModel.onClickShow()
        }

        viewModel.showKeyLiveEvent.observe(viewLifecycleOwner, {
            findNavController().navigate(R.id.showKeyFragment_to_showKeyWordsFragment, bundleOf(ShowKeyModule.WORDS to viewModel.words.toTypedArray()), navOptions())
        })

        viewModel.openUnlockLiveEvent.observe(viewLifecycleOwner, {
            findNavController().navigate(R.id.showKeyFragment_to_pinFragment, PinModule.forUnlock(), navOptions())
        })

        subscribeFragmentResults()
    }

    private fun subscribeFragmentResults() {
        getNavigationResult(PinModule.requestKey)?.let { bundle ->
            val resultType = bundle.getParcelable<PinInteractionType>(PinModule.requestType)
            val resultCode = bundle.getInt(PinModule.requestResult)

            if (resultType == PinInteractionType.UNLOCK) {
                when (resultCode) {
                    PinModule.RESULT_OK -> viewModel.onUnlock()
                    PinModule.RESULT_CANCELLED -> {
                        // on cancel unlock
                    }
                }
            }
        }

        getNavigationResult(BackupWordsModule.requestKey)?.let { bundle ->
            when (bundle.getInt(ShowKeyModule.SHOW_KEY_REQUEST)) {
                ShowKeyModule.RESULT_OK -> findNavController().popBackStack()
                else -> {
                }
            }
        }
    }

}
