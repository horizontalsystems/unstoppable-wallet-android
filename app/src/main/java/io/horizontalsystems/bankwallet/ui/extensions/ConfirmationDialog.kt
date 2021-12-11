package io.horizontalsystems.bankwallet.ui.extensions

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryRed
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import kotlinx.android.synthetic.main.fragment_confirmation_dialog.*

class ConfirmationDialog(
    private val listener: Listener,
    private val title: String,
    private val subtitle: String,
    private val icon: Int?,
    private val contentText: String?,
    private val actionButtonTitle: String?,
    private val cancelButtonTitle: String?,
    private val destructiveButtonTitle: String?
) : BaseBottomSheetDialogFragment() {

    interface Listener {
        fun onActionButtonClick() {}
        fun onDestructiveButtonClick() {}
        fun onCancelButtonClick() {}
    }

    private lateinit var contentTextView: TextView

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
        } ?: setHeaderIcon(R.drawable.ic_attention_yellow_24)

        contentTextView = view.findViewById(R.id.contentText)

        contentTextView.isVisible = contentText != null
        contentTextView.text = contentText

        buttonsCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
        )

        setButtons()
    }

    private fun setButtons() {
        buttonsCompose.setContent {
            ComposeAppTheme {
                Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                    actionButtonTitle?.let {
                        ButtonPrimaryYellow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp),
                            title = actionButtonTitle,
                            onClick = {
                                listener.onActionButtonClick()
                                dismiss()
                            }
                        )
                    }
                    destructiveButtonTitle?.let {
                        ButtonPrimaryRed(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, top = 16.dp, end = 16.dp),
                            title = destructiveButtonTitle,
                            onClick = {
                                listener.onDestructiveButtonClick()
                                dismiss()
                            }
                        )
                    }
                    cancelButtonTitle?.let {
                        ButtonPrimaryDefault(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, top = 16.dp, end = 16.dp),
                            title = cancelButtonTitle,
                            onClick = {
                                listener.onCancelButtonClick()
                                dismiss()
                            }
                        )
                    }
                }
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
                fragmentManager: FragmentManager,
                listener: Listener,
                destructiveButtonTitle: String? = null
        ) {

            val fragment = ConfirmationDialog(listener, title, subtitle, icon, contentText, actionButtonTitle, cancelButtonTitle, destructiveButtonTitle)
            val transaction = fragmentManager.beginTransaction()

            transaction.add(fragment, "bottom_coin_settings_alert_dialog")
            transaction.commitAllowingStateLoss()
        }
    }
}
