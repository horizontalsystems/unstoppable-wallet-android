package io.horizontalsystems.bankwallet.modules.fulltransactioninfo.views

import android.content.Context
import android.os.Bundle
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import io.horizontalsystems.bankwallet.R
import kotlinx.android.synthetic.main.fragment_full_transaction_info_error.*

class FullTransactionInfoErrorFragment : Fragment() {

    interface Listener {
        fun onRetry()
        fun onChangeProvider()
    }

    private var provider: String? = null
    private var errorText: String? = null
    private var errorImageRes: Int? = null
    private var showRetry: Boolean = false

    var listener: Listener? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_full_transaction_info_error, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        providerName.text = provider
        providerError.text = errorText
        errorImageRes?.let { errorImageView.setImageResource(it) }

        val changeProviderStyle = SpannableString(getString(R.string.FullInfo_Error_ChangeSource))
        changeProviderStyle.setSpan(UnderlineSpan(), 0, changeProviderStyle.length, 0)
        changeProvider.text = changeProviderStyle

        btnRetry.isVisible = showRetry
        if (showRetry) {
            btnRetry.setOnClickListener {
                listener?.onRetry()
            }
        }

        changeProvider.setOnClickListener {
            listener?.onChangeProvider()
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    companion object {
        fun newInstance(provider: String, error: String, errorImageRes: Int, showRetry: Boolean = true): FullTransactionInfoErrorFragment {
            val fragment = FullTransactionInfoErrorFragment()
            fragment.provider = provider
            fragment.errorText = error
            fragment.errorImageRes = errorImageRes
            fragment.showRetry = showRetry
            return fragment
        }
    }
}
