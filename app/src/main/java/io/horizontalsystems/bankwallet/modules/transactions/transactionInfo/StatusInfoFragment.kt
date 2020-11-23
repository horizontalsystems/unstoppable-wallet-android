package io.horizontalsystems.bankwallet.modules.transactions.transactionInfo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import kotlinx.android.synthetic.main.fragment_status_info.toolbar

class StatusInfoFragment : BaseFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_status_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.inflateMenu(R.menu.status_info_menu)
        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menuClose -> {
                    parentFragmentManager.popBackStack()
                    true
                }
                else -> super.onOptionsItemSelected(menuItem)
            }
        }

        activity?.onBackPressedDispatcher?.addCallback(this) {
            parentFragmentManager.popBackStack()
        }
    }

}
