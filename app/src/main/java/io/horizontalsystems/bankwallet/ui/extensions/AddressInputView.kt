package io.horizontalsystems.bankwallet.ui.extensions

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.swap.settings.Caution
import io.horizontalsystems.bankwallet.modules.swap.settings.RecipientAddressViewModel
import io.horizontalsystems.bankwallet.ui.compose.ColoredTextStyle
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryCircle
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryDefault
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import kotlinx.android.synthetic.main.view_input_address.view.*

class AddressInputView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var showQrButton: Boolean = false
    private var onTextChangeCallback: ((text: String?) -> Unit)? = null
    private var onPasteCallback: ((text: String?) -> Unit)? = null
    private var onFocusChangeCallback: ((hasFocus: Boolean) -> Unit)? = null
    private var onQrButtonClickCallback: (() -> Unit)? = null
    private var showSpinner = false
    private var hintText: String? = null
    private var inputText: String = ""
    private var editable = true


    init {
        inflate(context, R.layout.view_input_address, this)

        val ta = context.obtainStyledAttributes(attrs, R.styleable.AddressInputView)
        try {
            title.text = ta.getString(R.styleable.AddressInputView_title)
            title.isVisible = title.text.isNotEmpty()
            description.text = ta.getString(R.styleable.AddressInputView_description)
            showQrButton = ta.getBoolean(R.styleable.AddressInputView_showQrButton, true)
            hintText = ta.getString(R.styleable.AddressInputView_hint)
        } finally {
            ta.recycle()
        }

        updateInput()
    }

    private fun updateInput() {
        actionsCompose.setContent {
            ComposeAppTheme {
                val customTextSelectionColors = TextSelectionColors(
                    handleColor = ComposeAppTheme.colors.jacob,
                    backgroundColor = ComposeAppTheme.colors.jacob.copy(alpha = 0.4f)
                )
                val focusRequester = remember { FocusRequester() }

                Row(
                    modifier = Modifier
                        .padding(
                            start = 12.dp,
                            top = 8.dp,
                            end = 8.dp,
                            bottom = 8.dp
                        )
                        //on focus change listener
                        .focusRequester(focusRequester)
                        .onFocusChanged {
                            onFocusChangeCallback?.invoke(it.hasFocus)
                        }
                        .focusTarget()
                        .pointerInput(Unit) { detectTapGestures { focusRequester.requestFocus() } },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompositionLocalProvider(LocalTextSelectionColors provides customTextSelectionColors) {
                        BasicTextField(
                            modifier = Modifier
                                .padding(start = 0.dp, top = 4.dp, end = 8.dp, bottom = 4.dp)
                                .weight(1f),
                            value = inputText,
                            onValueChange = {
                                inputText = it
                                updateInput()
                            },
                            textStyle = ColoredTextStyle(
                                color = ComposeAppTheme.colors.oz,
                                textStyle = ComposeAppTheme.typography.body
                            ),
                            //input hint
                            decorationBox = { innerTextField ->
                                if (inputText.isEmpty()) {
                                    Text(
                                        hintText ?: "",
                                        color = ComposeAppTheme.colors.grey50,
                                        style = ComposeAppTheme.typography.body
                                    )
                                }
                                innerTextField()
                            },
                            cursorBrush = SolidColor(ComposeAppTheme.colors.oz),
                            enabled = editable
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (showSpinner) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp).padding(top = 4.dp, end = 8.dp),
                                color = ComposeAppTheme.colors.grey,
                                strokeWidth = 2.dp
                            )
                        }
                        if (inputText.isEmpty()) {
                            if (showQrButton) {
                                ButtonSecondaryCircle(
                                    modifier = Modifier.padding(end = 8.dp),
                                    icon = R.drawable.ic_qr_scan_20,
                                    onClick = {
                                        onQrButtonClickCallback?.invoke()
                                    }
                                )
                            }
                            ButtonSecondaryDefault(
                                modifier = Modifier.padding(0.dp),
                                title = context.getString(R.string.Send_Button_Paste),
                                onClick = {
                                    val pastedText = TextHelper.getCopiedText().trim()
                                    inputText = pastedText
                                    updateInput()
                                    onPasteCallback?.invoke(pastedText)
                                }
                            )
                        } else {
                            ButtonSecondaryCircle(
                                icon = R.drawable.ic_delete_20,
                                onClick = {
                                    inputText = ""
                                    updateInput()
                                }
                            )
                        }
                    }
                }
            }
        }

        onTextChangeCallback?.invoke(inputText)
    }

    fun setText(text: String?) {
        inputText = text ?: ""
        updateInput()
    }

    fun setHint(text: String?) {
        hintText = text
        updateInput()
    }

    fun setError(caution: Caution?) {
        error.text = caution?.text
        error.isVisible = caution != null

        when (caution?.type) {
            Caution.Type.Error -> {
                inputBackground.hasError = true
                error.setTextColor(context.getColor(R.color.red_d))
            }
            Caution.Type.Warning -> {
                inputBackground.hasWarning = true
                error.setTextColor(context.getColor(R.color.yellow_d))
            }
            else -> {
                inputBackground.clearStates()
            }
        }
    }

    private fun onFocusChange(callback: (Boolean) -> Unit) {
        onFocusChangeCallback = callback
    }

    fun onTextChange(callback: (String?) -> Unit) {
        onTextChangeCallback = callback
    }

    fun onPasteText(callback: (String?) -> Unit) {
        onPasteCallback = callback
    }

    fun setSpinner(isVisible: Boolean) {
        showSpinner = isVisible
        updateInput()
    }

    fun onButtonQrScanClick(callback: () -> Unit) {
        onQrButtonClickCallback = callback
    }

    fun setEditable(isEditable: Boolean) {
        editable = isEditable
        updateInput()
    }

    fun setViewModel(
        viewModel: RecipientAddressViewModel,
        lifecycleOwner: LifecycleOwner,
        onClickQrScan: () -> Unit
    ) {
        setHint(viewModel.inputFieldPlaceholder)
        setText(viewModel.initialValue)

        viewModel.isLoadingLiveData.observe(lifecycleOwner, { visible ->
            setSpinner(visible)
        })

        viewModel.setTextLiveData.observe(lifecycleOwner, {
            setText(it)
        })

        viewModel.cautionLiveData.observe(lifecycleOwner, {
            setError(it)
        })

        onFocusChange { hasFocus ->
            viewModel.onChangeFocus(hasFocus)
        }

        onTextChange {
            viewModel.onChangeText(it)
        }

        onPasteText {
            viewModel.onFetch(it)
        }

        onButtonQrScanClick(onClickQrScan)
    }
}
