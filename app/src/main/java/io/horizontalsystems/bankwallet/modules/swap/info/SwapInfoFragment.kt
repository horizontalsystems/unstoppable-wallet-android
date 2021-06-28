package io.horizontalsystems.bankwallet.modules.swap.info

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.core.findNavController
import kotlinx.android.synthetic.main.fragment_swap_info.*

class SwapInfoFragment : BaseFragment() {

    private val vmFactory by lazy { SwapInfoModule.Factory(requireArguments()) }
    private val viewModel by viewModels<SwapInfoViewModel> { vmFactory }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_swap_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menuClose -> {
                    findNavController().popBackStack()
                    true
                }
                else -> false
            }
        }

        toolbar.title = viewModel.title
        description.text = viewModel.description
        headerRelated.text = viewModel.dexRelated
        transactionFeeDescription.text = viewModel.transactionFeeDescription
        btnLink.text = viewModel.linkText

        btnLink.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(viewModel.dexUrl)
            startActivity(intent)
        }
    }

}
