package io.horizontalsystems.bankwallet.ui.extensions

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import io.horizontalsystems.bankwallet.R

class ConfirmationDialog(
        private val listener: Listener,
        private val title: String,
        private val subtitle: String,
        private val contentText: String,
        private val actionButtonTitle: String)
    : BaseBottomSheetDialogFragment() {

    interface Listener {
        fun onActionButtonClick() {}
        fun onCancelButtonClick() {}
    }

    private lateinit var contentTextView: TextView
    private lateinit var btnYellow: Button
    private lateinit var btnGrey: Button

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        listener.onCancelButtonClick()
    }

    override fun close() {
        super.close()
        listener.onCancelButtonClick()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setContentView(R.layout.fragment_confirmation_dialog)

        setTitle(title)
        setSubtitle(subtitle)
        setHeaderIcon(R.drawable.ic_attention_yellow)

        contentTextView = view.findViewById(R.id.contentText)
        btnYellow = view.findViewById(R.id.btnYellow)
        btnGrey = view.findViewById(R.id.btnGrey)

        contentTextView.text = contentText

        bindActions()
    }

    private fun bindActions() {
        btnYellow.visibility = View.VISIBLE
        btnGrey.visibility = View.VISIBLE

        btnYellow.text = actionButtonTitle
        btnYellow.setOnClickListener {
            listener.onActionButtonClick()
            dismiss()
        }

        btnGrey.text = getString(R.string.Alert_Cancel)
        btnGrey.setOnClickListener {
            listener.onCancelButtonClick()
            dismiss()
        }
    }

    companion object {
        fun show(title: String, subtitle: String, contentText: String, actionButtonTitle: String, activity: FragmentActivity, listener: Listener) {
            val fragment = ConfirmationDialog(listener, title, subtitle, contentText, actionButtonTitle)
            val transaction = activity.supportFragmentManager.beginTransaction()

            transaction.add(fragment, "bottom_coin_settings_alert_dialog")
            transaction.commitAllowingStateLoss()
        }
    }
}
