package io.horizontalsystems.bankwallet.ui.extensions

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentManager
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryRed
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportant

class ConfirmationDialog(
    private val listener: Listener,
    private val title: String,
    private val subtitle: String,
    private val icon: Int?,
    private val contentText: String?,
    private val actionButtonTitle: String?,
    private val cancelButtonTitle: String?,
    private val destructiveButtonTitle: String?
) : BaseComposableBottomSheetFragment() {

    interface Listener {
        fun onActionButtonClick() {}
        fun onDestructiveButtonClick() {}
        fun onCancelButtonClick() {}
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        listener.onCancelButtonClick()
    }

    override fun close() {
        super.close()
        listener.onCancelButtonClick()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                ComposeAppTheme {
                    BottomScreen()
                }
            }
        }
    }

    @Composable
    private fun BottomScreen() {
        BottomSheetHeader(
            iconPainter = painterResource(icon ?: R.drawable.ic_attention_yellow_24),
            title = title,
            subtitle = subtitle,
            onCloseClick = { close() }
        ) {
            Divider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 1.dp,
                color = ComposeAppTheme.colors.steel10
            )
            contentText?.let {
                Box(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    TextImportant(text = it)
                }
                Divider(
                    modifier = Modifier.fillMaxWidth(),
                    thickness = 1.dp,
                    color = ComposeAppTheme.colors.steel10
                )
            }
            actionButtonTitle?.let {
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 16.dp, end = 16.dp),
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
            Spacer(Modifier.height(16.dp))
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
