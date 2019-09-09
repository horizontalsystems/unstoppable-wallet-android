package io.horizontalsystems.bankwallet.ui.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.ui.extensions.CoinIconView
import io.horizontalsystems.bankwallet.viewHelpers.bottomDialog

class ManageWalletsDialog(private val listener: Listener, private val coin: Coin, private val accountKeyName: Int)
    : DialogFragment() {

    interface Listener {
        fun onClickCreateKey()
        fun onClickRestoreKey() {}
        fun onCancel() {}
    }

    private lateinit var rootView: View
    private lateinit var addKeyTitle: TextView
    private lateinit var addKeySubtitle: TextView
    private lateinit var addKeyInfo: TextView
    private lateinit var addCoinIcon: CoinIconView
    private lateinit var btnCreateKey: Button
    private lateinit var btnRestoreKey: Button
    private lateinit var btnClose: ImageView

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        rootView = View.inflate(context, R.layout.fragment_bottom_manage_keys, null) as ViewGroup

        addKeyTitle = rootView.findViewById(R.id.addKeyTitle)
        addKeySubtitle = rootView.findViewById(R.id.addKeySubtitle)
        addKeyInfo = rootView.findViewById(R.id.addKeyInfo)
        addCoinIcon = rootView.findViewById(R.id.addKeyIcon)
        btnCreateKey = rootView.findViewById(R.id.btnYellow)
        btnRestoreKey = rootView.findViewById(R.id.btnGrey)
        btnClose = rootView.findViewById(R.id.closeButton)

        btnClose.setOnClickListener {
            dismiss()
            listener.onCancel()
        }

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

        addKeyTitle.text = getString(R.string.AddCoin_Title)
        addKeySubtitle.text = getString(R.string.AddCoin_Subtitle, coin.title)
        addKeyInfo.text = getString(R.string.AddCoin_Description, coin.title, getString(accountKeyName), getString(accountKeyName), coin.title)
    }

    private fun bindActions() {
        btnCreateKey.setOnClickListener {
            listener.onClickCreateKey()
            dismiss()
        }

        btnRestoreKey.setOnClickListener {
            listener.onClickRestoreKey()
            dismiss()
        }
    }

    companion object {
        fun show(activity: FragmentActivity, listener: Listener, coin: Coin, accountKeyName: Int) {
            val fragment = ManageWalletsDialog(listener, coin, accountKeyName)
            val transaction = activity.supportFragmentManager.beginTransaction()

            transaction.add(fragment, "bottom_manage_keys_dialog")
            transaction.commitAllowingStateLoss()
        }
    }
}
