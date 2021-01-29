package io.horizontalsystems.bankwallet.ui.extensions

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.swap.tradeoptions.IVerifiedInputViewModel
import kotlinx.android.synthetic.main.view_input_with_buttons.view.*

class InputWithButtonsView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : ConstraintLayout(context, attrs, defStyleAttr) {

    private var onTextChangeCallback: ((old: String?, new: String?) -> Unit)? = null

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
        inflate(context, R.layout.view_input_with_buttons, this)

        val ta = context.obtainStyledAttributes(attrs, R.styleable.InputWithButtonsView)
        try {
            title.text = ta.getString(R.styleable.InputWithButtonsView_title)
            description.text = ta.getString(R.styleable.InputWithButtonsView_description)
            input.inputType = ta.getInt(R.styleable.InputWithButtonsView_android_inputType, EditorInfo.TYPE_TEXT_VARIATION_NORMAL)
        } finally {
            ta.recycle()
        }

        input.addTextChangedListener(textWatcher)
        deleteButton.setOnClickListener { input.text = null }
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
        setDeleteButtonVisibility(!text.isNullOrBlank())
    }

    fun setHint(text: String?) {
        input.hint = text
    }

    fun setError(text: String?) {
        val isVisible = !text.isNullOrEmpty()
        error.text = text
        error.isVisible = isVisible
        inputBackground.hasError = isVisible
    }

    fun onTextChange(callback: (old: String?, new: String?) -> Unit) {
        onTextChangeCallback = callback
    }

    fun setLeftButtonTitle(text: String) {
        inputButtonLeft.text = text
    }

    fun onLeftButtonClick(callback: () -> Unit) {
        inputButtonLeft.setOnClickListener {
            callback()
        }
    }

    fun setRightButtonTitle(text: String) {
        inputButtonRight.text = text
    }

    fun onRightButtonClick(callback: () -> Unit) {
        inputButtonRight.setOnClickListener {
            callback()
        }
    }

    private fun setDeleteButtonVisibility(visible: Boolean) {
        deleteButton.isVisible = visible
        inputButtonLeft.isVisible = !visible
        inputButtonRight.isVisible = !visible
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
            setError(it?.text)
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

        // todo: it should work with any number of buttons
        viewModel.inputFieldButtonItems.getOrNull(0)?.let { button ->
            setLeftButtonTitle(button.title)
            onLeftButtonClick(button.onClick)
        }

        viewModel.inputFieldButtonItems.getOrNull(1)?.let { button ->
            setRightButtonTitle(button.title)
            onRightButtonClick(button.onClick)
        }
    }

}
