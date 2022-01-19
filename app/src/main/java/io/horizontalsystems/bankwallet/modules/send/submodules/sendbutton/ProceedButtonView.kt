package io.horizontalsystems.bankwallet.modules.send.submodules.sendbutton

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.widget.ConstraintLayout
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.databinding.ViewButtonProceedBinding
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow

class ProceedButtonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding = ViewButtonProceedBinding.inflate(LayoutInflater.from(context), this, true)

    private var onClick: (() -> (Unit))? = null
    private var title: String = ""
    private var enabledState: Boolean = false

    fun bind(onClick: (() -> (Unit))? = null) {
        this.onClick = onClick
        updateButton()
    }

    fun updateState(enabled: Boolean) {
        enabledState = enabled
        updateButton()
    }

    fun setTitle(title: String) {
        this.title = title
        updateButton()
    }

    private fun updateButton() {
        binding.buttonProceedCompose.setContent {
            ComposeAppTheme {
                ButtonPrimaryYellow(
                    modifier = Modifier.padding(top = 24.dp, bottom = 24.dp),
                    title = context.getString(R.string.Send_DialogProceed),
                    onClick = {
                        onClick?.invoke()
                    },
                    enabled = enabledState
                )
            }
        }
    }
}
