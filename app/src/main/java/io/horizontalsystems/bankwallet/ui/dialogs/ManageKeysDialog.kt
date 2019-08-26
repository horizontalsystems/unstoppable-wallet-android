package io.horizontalsystems.bankwallet.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.extensions.CoinIconView
import io.horizontalsystems.bankwallet.viewHelpers.bottomDialog

class ManageKeysDialog(
        private val listener: Listener,
        private val title: String,
        private val content: String,
        private val action: ManageAction)
    : DialogFragment() {

    interface Listener {
        fun onClickCreateKey() {}
        fun onClickBackupKey() {}
    }

    private lateinit var rootView: View
    private lateinit var addKeyTitle: TextView
    private lateinit var addKeyInfo: TextView
    private lateinit var addCoinIcon: CoinIconView
    private lateinit var btnYellow: Button
    private lateinit var btnGrey: Button
    private lateinit var btnClose: ImageView

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        rootView = View.inflate(context, R.layout.fragment_bottom_manage_keys, null) as ViewGroup

        addKeyTitle = rootView.findViewById(R.id.addKeyTitle)
        addKeyInfo = rootView.findViewById(R.id.addKeyInfo)
        addCoinIcon = rootView.findViewById(R.id.addKeyIcon)

        btnYellow = rootView.findViewById(R.id.btnYellow)
        btnGrey = rootView.findViewById(R.id.btnGrey)
        btnClose = rootView.findViewById(R.id.closeButton)

        btnClose.setOnClickListener { dismiss() }

        bindContent()
        bindActions()

        return bottomDialog(activity, rootView)
    }

    private fun bindContent() {
        addCoinIcon.bind(R.drawable.ic_manage_keys)

        addKeyTitle.text = title
        addKeyInfo.text = content
    }

    private fun bindActions() {
        btnGrey.visibility = View.GONE
        btnYellow.visibility = View.VISIBLE

        if (action == ManageAction.CREATE) {
            btnYellow.text = getString(R.string.ManageKeys_Create)
            btnYellow.setOnClickListener {
                listener.onClickCreateKey()
                dismiss()
            }
        }

        if (action == ManageAction.BACKUP) {
            btnYellow.text = getString(R.string.ManageKeys_Backup)
            btnYellow.setOnClickListener {
                listener.onClickBackupKey()
                dismiss()
            }
        }
    }

    enum class ManageAction {
        CREATE,
        BACKUP
    }

    companion object {
        fun show(title: String, content: String, activity: FragmentActivity, listener: Listener, action: ManageAction) {
            val fragment = ManageKeysDialog(listener, title, content, action)
            val transaction = activity.supportFragmentManager.beginTransaction()

            transaction.add(fragment, "bottom_create_key_dialog")
            transaction.commitAllowingStateLoss()
        }
    }
}
