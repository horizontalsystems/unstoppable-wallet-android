package io.horizontalsystems.bankwallet.modules.swap.info

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.swap.SwapModule
import io.horizontalsystems.core.findNavController

class SwapInfoFragment : BaseFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_swap_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar = view.findViewById<Toolbar>(R.id.toolbar)
        val btnLink = view.findViewById<Button>(R.id.btnLink)

        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menuClose -> {
                    findNavController().popBackStack()
                    true
                }
                else -> false
            }
        }

        val dex = arguments?.getParcelable(dexKey) ?: SwapModule.Dex.Uniswap
        val viewModel = ViewModelProvider(this, SwapInfoModule.Factory(dex)).get(SwapInfoViewModel::class.java)

        toolbar.title = viewModel.title
        view.findViewById<TextView>(R.id.description).text = viewModel.description
        view.findViewById<TextView>(R.id.headerRelated).text = viewModel.dexRelated
        view.findViewById<TextView>(R.id.transactionFeeDescription).text = viewModel.transactionFeeDescription
        btnLink.text = viewModel.linkText

        btnLink.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(viewModel.dexUrl)
            startActivity(intent)
        }
    }

    companion object {
        const val dexKey = "dex"
    }

}
