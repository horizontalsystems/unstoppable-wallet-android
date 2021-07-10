package io.horizontalsystems.bankwallet.modules.enablecoins

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.extensions.BaseBottomSheetDialogFragment

class EnableCoinsDialog(private val listener: Listener, private val tokenType: String) : BaseBottomSheetDialogFragment() {

    interface Listener {
        fun onClickEnable()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setContentView(R.layout.fragment_enable_coins)

        setTitle(activity?.getString(R.string.EnalbeToken_Title))
        setSubtitle(tokenType)
        view.findViewById<TextView>(R.id.alertText).text = getString(R.string.EnalbeToken_Description, tokenType)

        val icon = getIcon(tokenType)

        setHeaderIcon(icon)

        view.findViewById<Button>(R.id.enableBtn).setOnClickListener {
            listener.onClickEnable()
            dismiss()
        }
    }

    private fun getIcon(tokenType: String): Int {
        return when(tokenType){
            "BEP2" -> R.drawable.bep2
            "BEP20" -> R.drawable.bep20
            else -> R.drawable.erc20
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
