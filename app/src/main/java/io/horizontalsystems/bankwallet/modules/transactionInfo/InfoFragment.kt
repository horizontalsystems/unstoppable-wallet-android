package io.horizontalsystems.bankwallet.modules.transactionInfo

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseDialogFragment
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.core.dismissOnBackPressed
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.views.ListPosition
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.fragment_info.*

class InfoFragment : BaseDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dialog?.window?.setWindowAnimations(R.style.BottomDialogLargeAnimation)
        dialog?.dismissOnBackPressed { dismiss() }
        return inflater.inflate(R.layout.fragment_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val infoParams = arguments?.getParcelable<InfoParameters>(INFO_PARAMETERS_KEY) ?: run {
            dismiss()
            return
        }

        toolbar.title = infoParams.title
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menuClose -> {
                    dismiss()
                    true
                }
                else -> false
            }
        }

        textDescription.text = infoParams.description

        if (infoParams.txHash != null && infoParams.conflictingTxHash != null) {
            itemTxHash.bind(
                getString(R.string.Info_DoubleSpend_ThisTx),
                infoParams.txHash,
                ListPosition.First
            ) { copyText(infoParams.txHash) }
            itemTxHash.isVisible = true

            itemConflictingTxHash.bind(
                getString(R.string.Info_DoubleSpend_ConflictingTx),
                infoParams.conflictingTxHash,
                ListPosition.Last
            ) { copyText(infoParams.conflictingTxHash) }
            itemConflictingTxHash.isVisible = true
        }
    }

    private fun copyText(txHash: String) {
        TextHelper.copyText(txHash)
        HudHelper.showSuccessMessage(requireView(), R.string.Hud_Text_Copied)
    }

    companion object {
        const val INFO_PARAMETERS_KEY = "info_parameters_key"

        fun arguments(infoParameters: InfoParameters) = Bundle(1).apply {
            putParcelable(INFO_PARAMETERS_KEY, infoParameters)
        }
    }
}

@Parcelize
data class InfoParameters(
    val title: String,
    val description: String,
    val txHash: String? = null,
    val conflictingTxHash: String? = null
) : Parcelable
