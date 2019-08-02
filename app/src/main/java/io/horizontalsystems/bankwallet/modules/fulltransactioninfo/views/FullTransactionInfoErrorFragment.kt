package io.horizontalsystems.bankwallet.modules.fulltransactioninfo.views

import android.content.Context
import android.os.Bundle
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import io.horizontalsystems.bankwallet.R
import kotlinx.android.synthetic.main.fragment_full_transaction_info_error.*

class FullTransactionInfoErrorFragment : Fragment() {

    interface Listener {
        fun onRetry()
        fun onChangeProvider()
    }

    private var provider: String? = null
    private var listener: Listener? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_full_transaction_info_error, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        providerName.text = provider
        providerError.text = getString(R.string.FullInfo_Error_Subtitle)

        val changeProviderStyle = SpannableString(getString(R.string.FullInfo_Error_ChangeSource))
        changeProviderStyle.setSpan(UnderlineSpan(), 0, changeProviderStyle.length, 0)
        changeProvider.text = changeProviderStyle

        btnRetry.setOnClickListener {
            listener?.onRetry()
        }

        changeProvider.setOnClickListener {
            listener?.onChangeProvider()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is Listener) {
            listener = context
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    companion object {
        fun newInstance(provider: String): FullTransactionInfoErrorFragment {
            val fragment = FullTransactionInfoErrorFragment()
            fragment.provider = provider
            return fragment
        }
    }
}
