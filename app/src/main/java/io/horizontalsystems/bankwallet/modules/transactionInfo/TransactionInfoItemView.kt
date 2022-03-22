package io.horizontalsystems.bankwallet.modules.transactionInfo

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.widget.ConstraintLayout
import io.horizontalsystems.bankwallet.databinding.ViewTransactionInfoItemBinding
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.Ellipsis
import io.horizontalsystems.views.ListPosition

class TransactionInfoItemView : ConstraintLayout {

    private val binding = ViewTransactionInfoItemBinding.inflate(LayoutInflater.from(context), this)

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    fun bind(title: String, address: String, listPosition: ListPosition, onHashClick: () -> Unit) {
        binding.txtTitle.text = title
        binding.txViewBackground.setBackgroundResource(listPosition.getBackground())

        binding.decoratedTextCompose.setContent {
            ComposeAppTheme {
                ButtonSecondaryDefault(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    title = address,
                    onClick = {
                        onHashClick.invoke()
                    },
                    ellipsis = Ellipsis.Middle()
                )
            }
        }

        invalidate()
    }

}
