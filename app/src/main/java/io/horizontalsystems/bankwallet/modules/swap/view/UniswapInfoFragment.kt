package io.horizontalsystems.bankwallet.modules.swap.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.views.TopMenuItem
import kotlinx.android.synthetic.main.fragment_swap_info.*

class UniswapInfoFragment : BaseFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_swap_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        shadowlessToolbar.bind(
                title = getString(R.string.UniswapInfo_Title),
                rightBtnItem = TopMenuItem(text = R.string.Button_Close, onClick = {
                    findNavController().popBackStack()
                })
        )

        buttonUniswap.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("https://uniswap.org/")
            startActivity(intent)
        }
    }
}
