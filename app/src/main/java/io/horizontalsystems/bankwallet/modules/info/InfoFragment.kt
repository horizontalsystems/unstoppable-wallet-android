package io.horizontalsystems.bankwallet.modules.info

import android.os.Bundle
import android.os.Parcelable
import android.view.*
import androidx.activity.addCallback
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

        val infoParams = arguments?.getParcelable<InfoParameters>(INFO_PARAMETERS_KEY) ?: run {
            parentFragmentManager.popBackStack()
            return
        }

        setHasOptionsMenu(true)
        setSupportActionBar(toolbar, title = infoParams.title)

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

        activity?.onBackPressedDispatcher?.addCallback(this) {
            parentFragmentManager.popBackStack()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.info_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menuClose -> {
                parentFragmentManager.popBackStack()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun copyText(txHash: String) {
        TextHelper.copyText(txHash)
        HudHelper.showSuccessMessage(requireView(), R.string.Hud_Text_Copied)
    }

    companion object {
        const val INFO_PARAMETERS_KEY = "info_parameters_key"

        fun instance(infoParameters: InfoParameters): InfoFragment {
            return InfoFragment().apply {
                arguments = Bundle(1).apply {
                    putParcelable(INFO_PARAMETERS_KEY, infoParameters)
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
