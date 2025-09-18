package io.horizontalsystems.bankwallet.uiv3.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.Caution
import io.horizontalsystems.bankwallet.ui.compose.ColoredTextStyle
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.body_andy
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonSize
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonStyle
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.bankwallet.uiv3.components.controls.HSButton
import io.horizontalsystems.bankwallet.uiv3.components.controls.HSIconButton

@Composable
private fun FormsInputInner(
    modifier: Modifier,
    placeholder: String,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    keyboardOptions: KeyboardOptions,
    buttonsSlot: @Composable () -> Unit,
    caution: Caution?,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val borderColor = when (caution?.type) {
            Caution.Type.Error -> ComposeAppTheme.colors.lucian
            Caution.Type.Warning -> ComposeAppTheme.colors.jacob
            null -> ComposeAppTheme.colors.blade
        }

        Row(
            modifier = Modifier
                .background(ComposeAppTheme.colors.lawrence, RoundedCornerShape(12.dp))
                .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BasicTextField(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 16.dp),
                value = value,
                onValueChange = onValueChange,
                textStyle = ColoredTextStyle(
                    color = ComposeAppTheme.colors.leah,
                    textStyle = ComposeAppTheme.typography.body
                ),
                keyboardOptions = keyboardOptions,
                decorationBox = { innerTextField ->
                    if (value.text.isBlank()) {
                        body_andy(text = placeholder)
                    }
                    innerTextField()
                }
            )

            buttonsSlot()
        }

        caution?.let {
            val color = when (caution.type) {
                Caution.Type.Error -> ComposeAppTheme.colors.lucian
                Caution.Type.Warning -> ComposeAppTheme.colors.jacob
            }

            Text(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = caution.text,
                style = ComposeAppTheme.typography.captionSB,
                color = color,
            )
        }
    }
}

interface InputTextValidator {
    fun isValid(text: String) : Boolean

    companion object {
        fun getValidator(keyboardType: KeyboardType) = when (keyboardType) {
            KeyboardType.Decimal -> InputTextValidatorDecimal()
            else -> InputTextValidatorDefault()
        }
    }
}

class InputTextValidatorDefault: InputTextValidator {
    override fun isValid(text: String): Boolean {
        return true
    }
}

class InputTextValidatorDecimal: InputTextValidator {
    override fun isValid(text: String): Boolean {
        if (text.isBlank()) return true

        return text.toBigDecimalOrNull() != null
    }
}

@Composable
fun FormsInput(
    modifier: Modifier = Modifier,
    value: String?,
    placeholder: String = "",
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    pasteButton: Boolean = false,
    clearButton: Boolean = false,
    buttons: List<FormsInputButton> = listOf(),
    caution: Caution? = null,
    onValueChange: (String) -> Unit,
) {
    var textState by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(value ?: ""))
    }

    LaunchedEffect(textState.text) {
        onValueChange.invoke(textState.text)
    }

    val allButtons = buildList {
        if (pasteButton) {
            val clipboardManager = LocalClipboardManager.current
            add(
                FormsInputButton(
                    type = FormsInputButton.Type.Text(
                        title = stringResource(id = R.string.Send_Button_Paste),
                    ),
                    visible = {
                        it.text.isBlank()
                    },
                    onClick = { textFieldValue ->
                        clipboardManager.getText()?.text?.let { textInClipboard ->
                            textFieldValue.copy(
                                text = textInClipboard,
                                selection = TextRange(textInClipboard.length)
                            )
                        }
                    }
                )
            )
        }

        if (clearButton) {
            add(
                FormsInputButton(
                    type = FormsInputButton.Type.Icon(
                        icon = painterResource(R.drawable.trash_24),
                    ),
                    visible = {
                        !it.text.isBlank()
                    },
                    onClick = { textState ->
                        textState.copy(text = "")
                    }
                )
            )
        }

        addAll(buttons)
    }

    val validator = remember(keyboardOptions.keyboardType) {
        InputTextValidator.getValidator(keyboardOptions.keyboardType)
    }

    FormsInputInner(
        modifier = modifier,
        placeholder = placeholder,
        value = textState,
        onValueChange = { newValue ->
            if (validator.isValid(newValue.text)) {
                textState = newValue
            }
        },
        keyboardOptions = keyboardOptions,
        buttonsSlot = {
            if (allButtons.any { it.visible(textState) }) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    allButtons.forEach { button ->
                        if (button.visible(textState)) {
                            when (button.type) {
                                is FormsInputButton.Type.Icon -> {
                                    HSIconButton(
                                        variant = ButtonVariant.Secondary,
                                        style = ButtonStyle.Solid,
                                        size = ButtonSize.Small,
                                        icon = button.type.icon,
                                        onClick = {
                                            button.onClick(textState)?.let {
                                                textState = it
                                            }
                                        }
                                    )

                                }

                                is FormsInputButton.Type.Text -> {
                                    HSButton(
                                        variant = ButtonVariant.Secondary,
                                        style = ButtonStyle.Solid,
                                        size = ButtonSize.Small,
                                        title = button.type.title,
                                        onClick = {
                                            button.onClick(textState)?.let {
                                                textState = it
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        caution = caution
    )
}

data class FormsInputButton(
    val type: Type,
    val visible: (TextFieldValue) -> Boolean,
    val onClick: (TextFieldValue) -> TextFieldValue?
) {
    sealed class Type {
        data class Icon(val icon: Painter) : Type()
        data class Text(val title: String) : Type()
    }
}

@Preview
@Composable
fun Preview_FormsInput() {
    ComposeAppTheme {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FormsInput(
                value = "Input",
                pasteButton = true,
                clearButton = true,
                onValueChange = {
                    Log.e("AAA", "onValueChange1: $it")
                }
            )

            FormsInput(
                value = "",
                placeholder = "Placeholder",
                pasteButton = true,
                clearButton = true,
                onValueChange = {
                    Log.e("AAA", "onValueChange2: $it")
                },
            )

            FormsInput(
                modifier = Modifier.padding(horizontal = 16.dp),
                value = "Error",
                caution = Caution("Some Error", Caution.Type.Error),
                onValueChange = {
                },
            )

            FormsInput(
                modifier = Modifier.padding(horizontal = 16.dp),
                value = "Warning",
                caution = Caution("Some Warning", Caution.Type.Warning),
                onValueChange = {
                },
            )
        }
    }
}
