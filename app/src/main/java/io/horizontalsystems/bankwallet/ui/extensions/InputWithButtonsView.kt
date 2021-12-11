package io.horizontalsystems.bankwallet.ui.extensions

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.swap.settings.Caution
import io.horizontalsystems.bankwallet.modules.swap.settings.IVerifiedInputViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryCircle
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryDefault
import kotlinx.android.synthetic.main.view_input_with_buttons.view.*
import kotlinx.android.synthetic.main.view_input_with_buttons.view.title

class InputWithButtonsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var onTextChangeCallback: ((old: String?, new: String?) -> Unit)? = null
    private var buttons: List<Button> = emptyList()

    private val textWatcher = object : TextWatcher {
        private var prevValue: String? = null

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            onTextChangeCallback?.invoke(prevValue, s?.toString())
            updateButtons()
        }

        override fun afterTextChanged(s: Editable?) {}
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            prevValue = s?.toString()
        }
    }

    init {
        inflate(context, R.layout.view_input_with_buttons, this)

        val ta = context.obtainStyledAttributes(attrs, R.styleable.InputWithButtonsView)
        try {
            title.text = ta.getString(R.styleable.InputWithButtonsView_title)
            description.text = ta.getString(R.styleable.InputWithButtonsView_description)
            input.inputType = ta.getInt(
                R.styleable.InputWithButtonsView_android_inputType,
                EditorInfo.TYPE_TEXT_VARIATION_NORMAL
            )
        } finally {
            ta.recycle()
        }

        input.addTextChangedListener(textWatcher)
    }

    private fun updateButtons() {
        actionsCompose.setContent {
            ComposeAppTheme {
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (input.text.isEmpty()) {
                        buttons.forEach { button ->
                            ButtonSecondaryDefault(
                                modifier = Modifier.padding(end = 8.dp),
                                title = button.title,
                                onClick = button.onClick
                            )
                        }
                    } else {
                        ButtonSecondaryCircle(
                            modifier = Modifier.padding(end = 8.dp),
                            icon = R.drawable.ic_delete_20,
                            onClick = {
                                input.text = null
                            }
                        )
                    }
                }
            }
        }
    }

    fun setText(text: String?, skipChangeEvent: Boolean = true, shakeAnimate: Boolean = false) {
        input.apply {
            if (skipChangeEvent) {
                removeTextChangedListener(textWatcher)
            }

            setText(text)

            if (skipChangeEvent) {
                addTextChangedListener(textWatcher)
            }

            text?.let {
                setSelection(it.length)
            }

            if (shakeAnimate) {
                startAnimation(AnimationUtils.loadAnimation(context, R.anim.shake_edittext))
            }
        }

        updateButtons()
    }

    fun setHint(text: String?) {
        input.hint = text
    }

    fun bind(buttons: List<Button>) {
        this.buttons = buttons
        updateButtons()
    }

    private fun setCaution(caution: Caution?) {
        error.text = caution?.text
        error.isVisible = !caution?.text.isNullOrEmpty()

        when (caution?.type) {
            Caution.Type.Error -> {
                error.setTextColor(context.getColor(R.color.lucian))
                inputBackground.hasError = true
            }
            Caution.Type.Warning -> {
                error.setTextColor(context.getColor(R.color.jacob))
                inputBackground.hasWarning = true
            }
            null -> {
                inputBackground.clearStates()
            }
        }
    }

    fun onTextChange(callback: (old: String?, new: String?) -> Unit) {
        onTextChangeCallback = callback
    }

    fun setViewModel(viewModel: IVerifiedInputViewModel, lifecycleOwner: LifecycleOwner) {
        input.maxLines = viewModel.inputFieldMaximumNumberOfLines
        if (!viewModel.inputFieldCanEdit) {
            input.keyListener = null
        }
        viewModel.setTextLiveData.observe(lifecycleOwner, {
            setText(it)
        })

        viewModel.cautionLiveData.observe(lifecycleOwner, {
            setCaution(it)
        })

        setHint(viewModel.inputFieldPlaceholder)
        setText(viewModel.initialValue)

        onTextChange { old, new ->
            if (viewModel.isValid(new)) {
                viewModel.onChangeText(new)
            } else {
                setText(old, true, true)
            }
        }

        bind(viewModel.inputFieldButtonItems.map { Button(it.title, it.onClick) })
    }

    data class Button(val title: String, val onClick: () -> Unit)

}
