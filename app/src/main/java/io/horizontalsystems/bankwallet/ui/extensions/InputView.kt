package io.horizontalsystems.bankwallet.ui.extensions

import android.content.Context
import android.graphics.Typeface
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.animation.AnimationUtils
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.databinding.ViewInputBinding
import io.horizontalsystems.bankwallet.modules.swap.settings.Caution
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryCircle

class InputView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding = ViewInputBinding.inflate(LayoutInflater.from(context), this)

    private var onTextChangeCallback: ((prevText: String?, newText: String?) -> Unit)? = null
    private var onPasteCallback: ((text: String?) -> Unit)? = null

    private val textWatcher = object : TextWatcher {
        private var prevValue: String? = null

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            onTextChangeCallback?.invoke(prevValue, s?.toString())
            setDeleteButtonVisibility(!s.isNullOrBlank())
        }

        override fun afterTextChanged(s: Editable?) {}
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            prevValue = s?.toString()
        }
    }

    init {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.InputView)
        try {
            binding.input.hint = ta.getString(R.styleable.InputView_android_hint)
            val inputType = ta.getInt(R.styleable.InputView_android_inputType, -1)
            if (inputType != -1) {
                binding.input.inputType = inputType

                // When inputType is password it uses monospace font and hint looks different. We need to set it to Typeface.DEFAULT
                binding.input.typeface = Typeface.DEFAULT
            }
        } finally {
            ta.recycle()
        }

        binding.input.addTextChangedListener(textWatcher)
    }

    fun setText(text: String?, skipChangeEvent: Boolean = true) {
        binding.input.apply {
            if (skipChangeEvent) {
                removeTextChangedListener(textWatcher)
            }

            setText(text)
            setSelection(text?.length ?: 0)

            if (skipChangeEvent) {
                addTextChangedListener(textWatcher)
            }
        }

        setDeleteButtonVisibility(!text.isNullOrBlank())
    }

    fun setHint(text: String?) {
        binding.input.hint = text
    }

    fun setError(caution: Caution?) {
        binding.error.text = caution?.text
        binding.error.isVisible = caution != null

        when (caution?.type) {
            Caution.Type.Error -> {
                binding.inputBackground.hasError = true
                binding.error.setTextColor(context.getColor(R.color.red_d))
            }
            Caution.Type.Warning -> {
                binding.inputBackground.hasWarning = true
                binding.error.setTextColor(context.getColor(R.color.yellow_d))
            }
            else -> {
                binding.inputBackground.clearStates()
            }
        }
    }

    fun onTextChange(callback: ((prevText: String?, newText: String?) -> Unit)?) {
        onTextChangeCallback = callback
    }

    fun onPasteText(callback: (String?) -> Unit) {
        onPasteCallback = callback
    }

    fun setEditable(isEditable: Boolean) {
        binding.input.isEnabled = isEditable
    }

    fun bindPrefix(prefix: String?) {
        binding.txtPrefix.text = prefix
        binding.txtPrefix.isVisible = !prefix.isNullOrBlank()
    }

    fun revertText(text: String?) {
        setText(text)
        val shake = AnimationUtils.loadAnimation(context, R.anim.shake_edittext)
        binding.input.startAnimation(shake)
    }

    private fun setDeleteButtonVisibility(visible: Boolean) {
        binding.buttonDeleteCompose.setContent {
            if (visible) {
                ComposeAppTheme {
                    ButtonSecondaryCircle(
                        modifier = Modifier.padding(end = 8.dp),
                        icon = R.drawable.ic_delete_20,
                        onClick = {
                            binding.input.text = null
                        }
                    )
                }
            }
        }
    }

}
