package io.horizontalsystems.bankwallet.modules.transactions.transactionInfo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseDialogFragment
import io.horizontalsystems.core.dismissOnBackPressed

class StatusInfoFragment : BaseDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        dialog?.window?.setWindowAnimations(R.style.BottomDialogLargeAnimation)
        dialog?.dismissOnBackPressed { dismiss() }
        return inflater.inflate(R.layout.fragment_status_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar = view.findViewById<Toolbar>(R.id.toolbar)

        toolbar.inflateMenu(R.menu.status_info_menu)
        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menuClose -> {
                    dismiss()
                    true
                }
                else -> super.onOptionsItemSelected(menuItem)
            }
        }
    }
}
