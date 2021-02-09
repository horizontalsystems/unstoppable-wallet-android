package io.horizontalsystems.bankwallet.modules.enablecoins

import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.extensions.BaseBottomSheetDialogFragment
import kotlinx.android.synthetic.main.fragment_enable_coins.*

class EnableCoinsDialog(private val listener: Listener, private val tokenType: String) : BaseBottomSheetDialogFragment() {

    interface Listener {
        fun onClickEnable()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setContentView(R.layout.fragment_enable_coins)

        setTitle(activity?.getString(R.string.EnalbeToken_Title))
        setSubtitle(tokenType)
        alertText.text = getString(R.string.EnalbeToken_Description, tokenType)

        if (tokenType == "BEP2") {
            setHeaderIcon(R.drawable.ic_bep2)
        } else {
            setHeaderIcon(R.drawable.ic_erc20)
        }

        enableBtn.setOnClickListener {
            listener.onClickEnable()
            dismiss()
        }
    }

    companion object {
        fun show(activity: FragmentActivity, tokenType: String, listener: Listener) {
            val fragment = EnableCoinsDialog(listener, tokenType)
            val transaction = activity.supportFragmentManager.beginTransaction()

            transaction.add(fragment, "bottom_enable_token_dialog")
            transaction.commitAllowingStateLoss()
        }
    }
}