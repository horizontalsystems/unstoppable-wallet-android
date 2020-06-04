package io.horizontalsystems.bankwallet.modules.managecoins.views

import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.extensions.BaseBottomSheetDialogFragment
import kotlinx.android.synthetic.main.fragment_bottom_add_token.*

class AddTokenDialog(private val listener: Listener)
    : BaseBottomSheetDialogFragment() {

    interface Listener {
        fun onClickAddErc20Token()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setContentView(R.layout.fragment_bottom_add_token)

        setTitle(activity?.getString(R.string.AddTokenDialog_Title))
        setSubtitle(getString(R.string.AddTokenDialog_Subtitle))
        setHeaderIcon(R.drawable.ic_plus)

        bindActions()
    }

    private fun bindActions() {
        erc20TokensBtn.setOnClickListener {
            listener.onClickAddErc20Token()
            dismiss()
        }
    }

    companion object {
        fun show(activity: FragmentActivity, listener: Listener) {
            val fragment = AddTokenDialog(listener)
            val transaction = activity.supportFragmentManager.beginTransaction()

            transaction.add(fragment, "bottom_add_token_dialog")
            transaction.commitAllowingStateLoss()
        }
    }
}
