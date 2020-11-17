package io.horizontalsystems.bankwallet.ui.extensions

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import io.horizontalsystems.bankwallet.R

class ConfirmationDialog(
        private val listener: Listener,
        private val title: String,
        private val subtitle: String,
        private val icon: Int?,
        private val contentText: String?,
        private val actionButtonTitle: String?,
        private val cancelButtonTitle: String?,
        private val destructiveButtonTitle: String?)
    : BaseBottomSheetDialogFragment() {

    interface Listener {
        fun onActionButtonClick() {}
        fun onDestructiveButtonClick() {}
        fun onCancelButtonClick() {}
    }

    private lateinit var contentTextView: TextView
    private lateinit var btnAction: Button
    private lateinit var btnDestructive: Button
    private lateinit var btnCancel: Button

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

        // if null set default "Attention" ICON
        icon?.let {
          setHeaderIcon(it)
        }?:setHeaderIcon(R.drawable.ic_attention_yellow_24)

        contentTextView = view.findViewById(R.id.contentText)
        btnAction = view.findViewById(R.id.btnYellow)
        btnDestructive = view.findViewById(R.id.btnDestructive)
        btnCancel = view.findViewById(R.id.btnGrey)

        contentTextView.isVisible = contentText != null
        contentTextView.text = contentText

        bindActions()
    }

    private fun bindActions() {

        // Set Visibility based on title is NULL or not
        btnAction.isVisible = actionButtonTitle != null
        btnDestructive.isVisible = destructiveButtonTitle != null
        btnCancel.isVisible = cancelButtonTitle != null

        actionButtonTitle?.let {

            btnAction.text = actionButtonTitle
            btnAction.setOnClickListener {
                listener.onActionButtonClick()
                dismiss()
            }
        }

        destructiveButtonTitle?.let {
            btnDestructive.text = destructiveButtonTitle
            btnDestructive.setOnClickListener {
                listener.onDestructiveButtonClick()
                dismiss()
            }
        }

        cancelButtonTitle?.let {

            btnCancel.text = cancelButtonTitle
            btnCancel.setOnClickListener {
                listener.onCancelButtonClick()
                dismiss()
            }
        }
    }

    companion object {

        fun show(
                icon: Int? = null,
                title: String,
                subtitle: String,
                contentText: String?,
                actionButtonTitle: String? = "",
                cancelButtonTitle: String? = "",
                activity: FragmentActivity,
                listener: Listener,
                destructiveButtonTitle: String? = null
        ) {

            val fragment = ConfirmationDialog(listener, title, subtitle, icon, contentText, actionButtonTitle, cancelButtonTitle, destructiveButtonTitle)
            val transaction = activity.supportFragmentManager.beginTransaction()

            transaction.add(fragment, "bottom_coin_settings_alert_dialog")
            transaction.commitAllowingStateLoss()
        }
    }
}
