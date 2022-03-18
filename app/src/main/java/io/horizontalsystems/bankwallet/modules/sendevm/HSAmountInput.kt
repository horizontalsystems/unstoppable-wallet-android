package io.horizontalsystems.bankwallet.modules.sendevm

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.horizontalsystems.bankwallet.modules.swap.settings.Caution
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.extensions.AmountInputView

@Composable
fun HSAmountInput(amountViewModel: AmountInputViewModel, caution: Caution?) {
    val amount by amountViewModel.amountLiveData.observeAsState()
    val revertAmount by amountViewModel.revertAmountLiveData.observeAsState()
    val maxEnabled by amountViewModel.maxEnabledLiveData.observeAsState(false)
    val secondaryText by amountViewModel.secondaryTextLiveData.observeAsState()
    val inputParams by amountViewModel.inputParamsLiveData.observeAsState()

    val borderColor = when (caution?.type) {
        Caution.Type.Error -> ComposeAppTheme.colors.red50
        Caution.Type.Warning -> ComposeAppTheme.colors.yellow50
        else -> ComposeAppTheme.colors.steel20
    }

    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                .background(ComposeAppTheme.colors.lawrence),
            factory = {
                AmountInputView(it).apply {
                    this.onTextChangeCallback = { _, new ->
                        amountViewModel.onChangeAmount(new ?: "")
                    }
                    this.onTapSecondaryCallback = {
                        amountViewModel.onSwitch()
                    }
                    this.onTapMaxCallback = {
                        amountViewModel.onClickMax()
                    }
                    this.postDelayed(
                        { this.setFocus() },
                        200
                    )
                }
            },
            update = {
                val amountInput = it
                if (amountInput.getAmount() != amount &&
                    !amountViewModel.areAmountsEqual(amountInput.getAmount(), amount)
                ) {
                    amountInput.setAmount(amount)
                }

                amountInput.revertAmount(revertAmount)
                amountInput.maxButtonVisible = maxEnabled
                amountInput.setSecondaryText(secondaryText)
                inputParams?.let { amountInput.setInputParams(it) }
            }
        )

        caution?.let { caution ->
            val color: Color = when (caution.type) {
                Caution.Type.Error -> ComposeAppTheme.colors.redD
                Caution.Type.Warning -> ComposeAppTheme.colors.yellowD
            }
            Text(
                modifier = Modifier.padding(start = 8.dp, top = 8.dp, end = 8.dp),
                text = caution.text,
                style = ComposeAppTheme.typography.caption,
                color = color,
            )
        }
    }
}