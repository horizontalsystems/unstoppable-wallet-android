package io.horizontalsystems.bankwallet.modules.transactionInfo

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseDialogFragment
import io.horizontalsystems.bankwallet.databinding.FragmentInfoBinding
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.core.dismissOnBackPressed
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.views.ListPosition
import kotlinx.parcelize.Parcelize

class InfoFragment : BaseDialogFragment() {

    private var _binding: FragmentInfoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInfoBinding.inflate(inflater, container, false)
        val view = binding.root
        dialog?.window?.setWindowAnimations(R.style.BottomDialogLargeAnimation)
        dialog?.dismissOnBackPressed { dismiss() }
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val infoParams = arguments?.getParcelable<InfoParameters>(INFO_PARAMETERS_KEY) ?: run {
            dismiss()
            return
        }

        binding.toolbar.title = infoParams.title
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menuClose -> {
                    dismiss()
                    true
                }
                else -> false
            }
        }

        binding.textDescription.text = infoParams.description

        if (infoParams.txHash != null && infoParams.conflictingTxHash != null) {
            binding.itemTxHash.bind(
                getString(R.string.Info_DoubleSpend_ThisTx),
                infoParams.txHash,
                ListPosition.First
            ) { copyText(infoParams.txHash) }
            binding.itemTxHash.isVisible = true

            binding.itemConflictingTxHash.bind(
                getString(R.string.Info_DoubleSpend_ConflictingTx),
                infoParams.conflictingTxHash,
                ListPosition.Last
            ) { copyText(infoParams.conflictingTxHash) }
            binding.itemConflictingTxHash.isVisible = true
        }
    }

    private fun copyText(txHash: String) {
        TextHelper.copyText(txHash)
        HudHelper.showSuccessMessage(requireView(), R.string.Hud_Text_Copied)
    }

    companion object {
        const val INFO_PARAMETERS_KEY = "info_parameters_key"

        fun prepareParams(infoParameters: InfoParameters) = bundleOf(INFO_PARAMETERS_KEY to infoParameters)
    }
}

@Parcelize
data class InfoParameters(
    val title: String,
    val description: String,
    val txHash: String? = null,
    val conflictingTxHash: String? = null
) : Parcelable
