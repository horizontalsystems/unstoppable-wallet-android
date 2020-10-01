package io.horizontalsystems.bankwallet.modules.info

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.views.TopMenuItem
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.fragment_info.*

class InfoFragment : BaseFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val infoParams = arguments?.getParcelable<InfoParameters>("infoParameters") ?: run {
            parentFragmentManager.popBackStack()
            return
        }

        shadowlessToolbar.bind(
                title = infoParams.title,
                rightBtnItem = TopMenuItem(text = R.string.Button_Close, onClick = { parentFragmentManager.popBackStack() })
        )

        textDescription.text = infoParams.description

        infoParams.txHash?.let { txHash ->
            itemTxHash.bindHashId(getString(R.string.Info_DoubleSpend_ThisTx), txHash)
            itemTxHash.isVisible = true

            itemTxHash.setOnClickListener { copyText(txHash) }
        }

        infoParams.conflictingTxHash?.let { conflictingTxHash ->
            itemConflictingTxHash.bindHashId(getString(R.string.Info_DoubleSpend_ConflictingTx), conflictingTxHash)
            itemConflictingTxHash.isVisible = true

            itemConflictingTxHash.setOnClickListener { copyText(conflictingTxHash) }
        }

    }

    override fun canHandleOnBackPress(): Boolean {
        parentFragmentManager.popBackStack()
        return true
    }

    private fun copyText(txHash: String) {
        TextHelper.copyText(txHash)
        HudHelper.showSuccessMessage(requireView(), R.string.Hud_Text_Copied)
    }

    companion object {
        fun instance(infoParameters: InfoParameters): InfoFragment {
            return InfoFragment().apply {
                arguments = Bundle(1).apply {
                    putParcelable("infoParameters", infoParameters)
                }
            }
        }
    }

}

@Parcelize
data class InfoParameters(val title: String,
                          val description: String,
                          val txHash: String? = null,
                          val conflictingTxHash: String? = null) : Parcelable
