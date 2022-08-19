package io.horizontalsystems.bankwallet.ui.extensions

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentManager
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryTransparent
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportantWarning

class ConfirmationDialog(
    private val listener: Listener,
    private val title: String,
    private val icon: Int?,
    private val warningTitle: String?,
    private val warningText: String?,
    private val actionButtonTitle: String?,
    private val transparentButtonTitle: String?,
) : BaseComposableBottomSheetFragment() {

    interface Listener {
        fun onActionButtonClick() {}
        fun onTransparentButtonClick() {}
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
            iconPainter = painterResource(icon ?: R.drawable.ic_attention_24),
            iconTint = ColorFilter.tint(ComposeAppTheme.colors.jacob),
            title = title,
            onCloseClick = { close() }
        ) {

            warningText?.let {
                TextImportantWarning(
                    title = warningTitle,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    text = it
                )
                Spacer(Modifier.height(8.dp))
            }
            actionButtonTitle?.let {
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, top = 12.dp, end = 24.dp),
                    title = actionButtonTitle,
                    onClick = {
                        listener.onActionButtonClick()
                        dismiss()
                    }
                )
            }
            transparentButtonTitle?.let {
                ButtonPrimaryTransparent(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, top = 12.dp, end = 24.dp),
                    title = transparentButtonTitle,
                    onClick = {
                        listener.onTransparentButtonClick()
                        dismiss()
                    }
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    companion object {

        fun show(
            icon: Int? = null,
            title: String,
            warningTitle: String? = null,
            warningText: String?,
            actionButtonTitle: String? = "",
            transparentButtonTitle: String? = "",
            fragmentManager: FragmentManager,
            listener: Listener,
        ) {

            val fragment = ConfirmationDialog(
                listener,
                title,
                icon,
                warningTitle,
                warningText,
                actionButtonTitle,
                transparentButtonTitle,
            )
            val transaction = fragmentManager.beginTransaction()

            transaction.add(fragment, "bottom_coin_settings_alert_dialog")
            transaction.commitAllowingStateLoss()
        }
    }
}
