package io.horizontalsystems.bankwallet.ui.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.ui.extensions.CoinIconView
import io.horizontalsystems.bankwallet.viewHelpers.bottomDialog

class ManageWalletsDialog(private val listener: Listener, private val coin: Coin)
    : DialogFragment() {

    interface Listener {
        fun onClickCreateKey()
        fun onClickRestoreKey() {}
        fun onCancel() {}
    }

    private lateinit var rootView: View
    private lateinit var addKeyTitle: TextView
    private lateinit var addKeyInfo: TextView
    private lateinit var addCoinIcon: CoinIconView
    private lateinit var buttonCreateKey: Button
    private lateinit var buttonRestoreKey: Button

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        rootView = View.inflate(context, R.layout.fragment_bottom_manage_keys, null) as ViewGroup

        addKeyTitle = rootView.findViewById(R.id.addKeyTitle)
        addKeyInfo = rootView.findViewById(R.id.addKeyInfo)
        addCoinIcon = rootView.findViewById(R.id.addKeyIcon)
        buttonCreateKey = rootView.findViewById(R.id.btnCreateKey)
        buttonRestoreKey = rootView.findViewById(R.id.btnRestoreKey)

        bindContent()
        bindActions()

        return bottomDialog(activity, rootView)
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        listener.onCancel()
    }

    private fun bindContent() {
        addCoinIcon.bind(coin)

        addKeyTitle.text = getString(R.string.AddCoin_Title, coin.code)
        addKeyInfo.text = getString(R.string.AddCoin_Description, coin.title)
    }

    private fun bindActions() {
        buttonCreateKey.setOnClickListener {
            listener.onClickCreateKey()
            dismiss()
        }

        buttonRestoreKey.setOnClickListener {
            listener.onClickRestoreKey()
            dismiss()
        }
    }

    companion object {
        fun show(activity: FragmentActivity, listener: Listener, coin: Coin) {
            val fragment = ManageWalletsDialog(listener, coin)
            val transaction = activity.supportFragmentManager.beginTransaction()

            transaction.add(fragment, "bottom_manage_keys_dialog")
            transaction.commitAllowingStateLoss()
        }
    }
}
