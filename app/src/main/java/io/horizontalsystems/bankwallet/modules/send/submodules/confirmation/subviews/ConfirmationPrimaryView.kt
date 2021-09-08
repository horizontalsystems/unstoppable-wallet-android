package io.horizontalsystems.bankwallet.modules.send.submodules.confirmation.subviews

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.send.submodules.confirmation.SendConfirmationModule
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.Ellipsis
import kotlinx.android.synthetic.main.view_confirmation_primary_item_view.view.*

class ConfirmationPrimaryView : ConstraintLayout {

    init {
        inflate(context, R.layout.view_confirmation_primary_item_view, this)
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    fun bind(
        primaryItemData: SendConfirmationModule.PrimaryItemData,
        onReceiverClick: (() -> (Unit))
    ) {
        primaryName.text = primaryItemData.primaryName
        primaryAmount.text = primaryItemData.primaryAmount
        primaryAmount.setCompoundDrawablesWithIntrinsicBounds(
            0,
            0,
            if (primaryItemData.locked) R.drawable.ic_lock_20 else 0,
            0
        )
        secondaryName.text = primaryItemData.secondaryName
        secondaryAmount.text = primaryItemData.secondaryAmount

        valuesCompose.setContent {
            ComposeAppTheme {
                Column(
                    modifier = Modifier.width(IntrinsicSize.Max)
                ) {
                    primaryItemData.domain?.let { domain ->
                        drawValue(
                            context.getString(R.string.Send_Confirmation_Domain),
                            domain,
                            { }
                        )
                        Divider(thickness = 1.dp, color = ComposeAppTheme.colors.steel10)
                    }
                    drawValue(
                        context.getString(R.string.Send_Confirmation_Receiver),
                        primaryItemData.receiver,
                        onReceiverClick
                    )
                }
            }
        }

        primaryItemData.memo?.let {
            borderMemo.isVisible = true
            memoLayout.isVisible = true
            memoValue.text = it
        }
    }

    @Composable
    private fun drawValue(title: String, value: String, onClick: () -> Unit) {
        Row(
            modifier = Modifier.fillMaxWidth().height(47.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProvideTextStyle(
                value = ComposeAppTheme.typography.subhead2
            ) {
                Text(
                    text = title,
                    color = ComposeAppTheme.colors.grey,
                )
            }
            ButtonSecondaryDefault(
                title = value,
                onClick = onClick,
                ellipsis = Ellipsis.Middle(8)
            )
        }
    }
}
