package io.horizontalsystems.bankwallet.ui.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
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

class ManageKeysDialog(private val coin: Coin, private val listener: Listener, private val showCreateOption: Boolean = true)
    : DialogFragment() {

    interface Listener {
        fun onClickCreateKey()
        fun onClickRestoreKey()
        fun onCancel()
    }

    private var mDialog: Dialog? = null
    private lateinit var rootView: View
    private lateinit var buttonCreateKey: Button
    private lateinit var buttonRestoreKey: Button
    private lateinit var addKeyTitle: TextView
    private lateinit var addKeyDescription: TextView
    private lateinit var addCoinIcon: CoinIconView

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = activity?.let { AlertDialog.Builder(it, R.style.BottomDialog) }

        rootView = View.inflate(context, R.layout.fragment_bottom_manage_keys, null) as ViewGroup
        buttonCreateKey = rootView.findViewById(R.id.btnCreateKey)
        buttonRestoreKey = rootView.findViewById(R.id.btnRestoreKey)
        addKeyTitle = rootView.findViewById(R.id.addKeyTitle)
        addKeyDescription = rootView.findViewById(R.id.addKeyDescription)
        addCoinIcon = rootView.findViewById(R.id.addKeyIcon)

        builder?.setView(rootView)
        mDialog = builder?.create()
        mDialog?.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        mDialog?.window?.setGravity(Gravity.BOTTOM)
        mDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        addKeyTitle.text = getString(R.string.AddCoin_Title, coin.code)
        addKeyDescription.text = getString(R.string.AddCoin_Description, coin.title)

        addCoinIcon.bind(coin)

        if (!showCreateOption) {
            buttonCreateKey.visibility = View.INVISIBLE
        } else {
            buttonCreateKey.visibility = View.VISIBLE
            buttonCreateKey.setOnClickListener {
                listener.onClickCreateKey()
                dismiss()
            }
        }

        buttonRestoreKey.setOnClickListener {
            listener.onClickRestoreKey()
            dismiss()
        }

        return mDialog as Dialog
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        listener.onCancel()
    }

    companion object {
        fun show(activity: FragmentActivity, coin: Coin, listener: Listener) {
            val fragment = ManageKeysDialog(coin, listener)
            val transaction = activity.supportFragmentManager.beginTransaction()

            transaction.add(fragment, "bottom_manage_keys_alert")
            transaction.commitAllowingStateLoss()
        }

        fun showWithoutCreateOption(activity: FragmentActivity, coin: Coin, listener: Listener) {
            val fragment = ManageKeysDialog(coin, listener, false)
            val transaction = activity.supportFragmentManager.beginTransaction()

            transaction.add(fragment, "bottom_manage_keys_alert")
            transaction.commitAllowingStateLoss()
        }
    }
}
