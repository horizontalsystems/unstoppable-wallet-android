package io.horizontalsystems.bankwallet.modules.send.submodules.confirmation.subviews

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.send.submodules.confirmation.SendConfirmationModule
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import kotlinx.android.synthetic.main.view_send_button.view.*

class ConfirmationSendButtonView : FrameLayout {

    init {
        inflate(context, R.layout.view_send_button, this)
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun bind(state: SendConfirmationModule.SendButtonState, onClick: () -> Unit) {
        when (state) {
            SendConfirmationModule.SendButtonState.ACTIVE -> {
                updateButton(context.getString(R.string.Send_Confirmation_Send_Button), true, onClick)
            }
            SendConfirmationModule.SendButtonState.SENDING -> {
                updateButton(context.getString(R.string.Send_Sending), false, onClick)
            }
        }
    }

    private fun updateButton(title: String, enabled: Boolean, onClick: () -> Unit) {
        buttonSendCompose.setContent {
            ComposeAppTheme {
                ButtonPrimaryYellow(
                    modifier = Modifier.padding(
                        start = 16.dp,
                        top = 24.dp,
                        end = 16.dp,
                        bottom = 24.dp
                    ),
                    title = title,
                    onClick = onClick,
                    enabled = enabled
                )
            }
        }
    }

}
