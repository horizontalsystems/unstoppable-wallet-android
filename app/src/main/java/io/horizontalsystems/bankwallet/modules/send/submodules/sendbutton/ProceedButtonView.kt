package io.horizontalsystems.bankwallet.modules.send.submodules.sendbutton

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.widget.ConstraintLayout
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import kotlinx.android.synthetic.main.view_button_proceed.view.*

class ProceedButtonView : ConstraintLayout {

    init {
        inflate(context, R.layout.view_button_proceed, this)
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

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
        buttonProceedCompose.setContent {
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
