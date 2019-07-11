package io.horizontalsystems.bankwallet.ui.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.ui.extensions.CoinIconView

class BottomManageKeysAlert(val coin: Coin, val listener: Listener) : DialogFragment() {

    interface Listener {
        fun onClickManageKeys()
    }

    private var mDialog: Dialog? = null
    private lateinit var rootView: View
    private lateinit var buttonManageKeys: Button
    private lateinit var addCoinTitle: TextView
    private lateinit var addCoinIcon: CoinIconView

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = activity?.let { AlertDialog.Builder(it, R.style.BottomDialog) }

        rootView = View.inflate(context, R.layout.fragment_bottom_manage_keys, null) as ViewGroup
        buttonManageKeys = rootView.findViewById(R.id.btnManageKeys)
        addCoinTitle = rootView.findViewById(R.id.addCoinTitle)
        addCoinIcon = rootView.findViewById(R.id.addCoinIcon)

        builder?.setView(rootView)
        mDialog = builder?.create()
        mDialog?.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        mDialog?.window?.setGravity(Gravity.BOTTOM)
        mDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        addCoinTitle.text = getString(R.string.AddCoin_title, coin.title)
        addCoinIcon.bind(coin)
        buttonManageKeys.setOnClickListener {
            listener.onClickManageKeys()
            dismiss()
        }

        return mDialog as Dialog
    }

    companion object {
        fun show(activity: FragmentActivity, coin: Coin, listener: Listener) {
            val fragment = BottomManageKeysAlert(coin, listener)
            val transaction = activity.supportFragmentManager.beginTransaction()

            transaction.add(fragment, "bottom_confirm_alert")
            transaction.commitAllowingStateLoss()
        }
    }
}
