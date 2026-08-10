package io.horizontalsystems.walletkit.modules.evmfee
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.Warning
import io.horizontalsystems.walletkit.core.ethereum.CautionViewItem
import io.horizontalsystems.walletkit.ui.compose.ColoredTextStyle
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.ui.compose.animations.shake
import io.horizontalsystems.walletkit.ui.compose.components.HSpacer
import io.horizontalsystems.walletkit.uiv3.components.AlertCard
import io.horizontalsystems.walletkit.uiv3.components.AlertFormat
import io.horizontalsystems.walletkit.uiv3.components.AlertType
import io.horizontalsystems.walletkit.uiv3.components.controls.ButtonSize
import io.horizontalsystems.walletkit.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.walletkit.uiv3.components.controls.HSIconButton
import java.math.BigDecimal
import io.horizontalsystems.walletkit.ui.compose.components.HeaderText
import io.horizontalsystems.walletkit.modules.multiswap.SwapInfoSheet
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.core.HSCaution

@Composable
fun NumberInputWithButtons(
    value: BigDecimal,
    decimals: Int,
    textColor: Color,
    onValueChange: (BigDecimal) -> Unit,
    onClickIncrement: () -> Unit,
    onClickDecrement: () -> Unit
) {
    var textState by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        val text = value.toString()
        mutableStateOf(TextFieldValue(text))
    }
    var playShakeAnimation by remember { mutableStateOf(false) }

    LaunchedEffect(value) {
        textState = textState.copy(text = value.toString(), selection = TextRange("$value".length))
    }

    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .defaultMinSize(minHeight = 54.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(0.5.dp, ComposeAppTheme.colors.blade, RoundedCornerShape(16.dp))
            .background(ComposeAppTheme.colors.lawrence),
        verticalAlignment = Alignment.CenterVertically
    ) {

        BasicTextField(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .weight(1f)
                .shake(
                    enabled = playShakeAnimation,
                    onAnimationFinish = { playShakeAnimation = false }
                ),
            value = textState,
            onValueChange = { textFieldValue ->
                val newValue = textFieldValue.text.toBigDecimalOrNull() ?: BigDecimal.ZERO
                if (newValue.scale() <= decimals) {
                    val currentText = textState.text
                    textState = textFieldValue
                    if (currentText != textFieldValue.text) {
                        onValueChange(newValue)
                    }
                } else {
                    playShakeAnimation = true
                }
            },
            textStyle = ColoredTextStyle(
                color = textColor,
                textStyle = ComposeAppTheme.typography.body
            ),
            singleLine = true,
            cursorBrush = SolidColor(ComposeAppTheme.colors.leah),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )

        HSIconButton(
            icon = painterResource(id = R.drawable.ic_minus_20),
            variant = ButtonVariant.Secondary,
            size = ButtonSize.Small,
            onClick =onClickDecrement
        )
        HSpacer(16.dp)
        HSIconButton(
            icon = painterResource(id = R.drawable.ic_plus_20),
            variant = ButtonVariant.Secondary,
            size = ButtonSize.Small,
            onClick =onClickIncrement
        )
        HSpacer(16.dp)
    }
}

@Composable
fun ButtonsGroupWithShade(
    ButtonsContent: @Composable (() -> Unit)
) {
    Column(
        modifier = Modifier
            .offset(y = -(24.dp))
            .navigationBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .background(
                    brush = Brush.verticalGradient(
                        listOf(ComposeAppTheme.colors.transparent, ComposeAppTheme.colors.tyler)
                    )
                )
        )
        Box(
            modifier = Modifier.background(ComposeAppTheme.colors.tyler)
        ) {
            ButtonsContent()
        }
    }
}

@Composable
fun Cautions(cautions: List<CautionViewItem>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        cautions.forEach { caution ->
            val alertType = when (caution.type) {
                CautionViewItem.Type.Error -> AlertType.Critical
                CautionViewItem.Type.Warning -> AlertType.Caution
            }

            AlertCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp),
                format = AlertFormat.Structured,
                type = alertType,
                text = caution.text,
                titleCustom = caution.title
            )
        }
    }
}

@Composable
fun EvmSettingsInput(
    title: String,
    info: String,
    value: BigDecimal,
    decimals: Int,
    warnings: List<Warning>,
    errors: List<Throwable>,
    navigation: HSNavigation,
    onValueChange: (BigDecimal) -> Unit,
    onClickIncrement: () -> Unit,
    onClickDecrement: () -> Unit
) {
    val textColor = when {
        errors.isNotEmpty() -> ComposeAppTheme.colors.lucian
        warnings.isNotEmpty() -> ComposeAppTheme.colors.jacob
        else -> ComposeAppTheme.colors.leah
    }

    EvmSettingsInput(
        title = title,
        info = info,
        value = value,
        decimals = decimals,
        textColor = textColor,
        navigation = navigation,
        onValueChange = onValueChange,
        onClickIncrement = onClickIncrement,
        onClickDecrement = onClickDecrement
    )
}

@Composable
fun EvmSettingsInput(
    title: String,
    info: String,
    value: BigDecimal,
    decimals: Int,
    caution: HSCaution?,
    navigation: HSNavigation,
    onValueChange: (BigDecimal) -> Unit,
    onClickIncrement: () -> Unit,
    onClickDecrement: () -> Unit
) {
    val textColor = when (caution?.type) {
        HSCaution.Type.Error -> ComposeAppTheme.colors.lucian
        HSCaution.Type.Warning -> ComposeAppTheme.colors.jacob
        else -> ComposeAppTheme.colors.leah
    }

    EvmSettingsInput(
        title = title,
        info = info,
        value = value,
        decimals = decimals,
        textColor = textColor,
        navigation = navigation,
        onValueChange = onValueChange,
        onClickIncrement = onClickIncrement,
        onClickDecrement = onClickDecrement
    )
}

@Composable
private fun EvmSettingsInput(
    title: String,
    info: String,
    value: BigDecimal,
    decimals: Int,
    textColor: Color,
    navigation: HSNavigation,
    onValueChange: (BigDecimal) -> Unit,
    onClickIncrement: () -> Unit,
    onClickDecrement: () -> Unit,
) {
    HeaderText(text = title) {
        navigation.slideFromBottom(
            SwapInfoSheet(SwapInfoSheet.Input(title, info))
        )
    }

    NumberInputWithButtons(
        value,
        decimals,
        textColor,
        onValueChange,
        onClickIncrement,
        onClickDecrement
    )
}
