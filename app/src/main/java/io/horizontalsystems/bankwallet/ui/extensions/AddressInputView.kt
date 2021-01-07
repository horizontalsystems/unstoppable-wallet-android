package io.horizontalsystems.bankwallet.ui.extensions

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.LifecycleOwner
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.swap.tradeoptions.IVerifiedInputViewModel
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.views.helpers.LayoutHelper
import kotlinx.android.synthetic.main.view_input_address.view.*

class AddressInputView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : ConstraintLayout(context, attrs, defStyleAttr) {

    private var showQrButton: Boolean = false

    private val params = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
        rightMargin = LayoutHelper.dp(8f, context)
    }

    private val buttonPaste by lazy {
        createButton(context, R.style.ButtonSecondary, R.string.Send_Button_Paste, params)
    }

    private val buttonQrScan by lazy {
        createImageButton(context, R.style.ImageButtonSecondary, R.drawable.ic_qr_scan_20, params)
    }

    private val buttonDelete by lazy {
        createImageButton(context, R.style.ImageButtonSecondary, R.drawable.ic_delete_20, params) {
            input.text = null
        }.also {
            actionsLayout.addView(it)
        }
    }

    private val progressBar by lazy {
        createProgressBar(context).also {
            actionsLayout.addView(it, 0)
        }
    }

    init {
        inflate(context, R.layout.view_input_address, this)

        val ta = context.obtainStyledAttributes(attrs, R.styleable.AddressInputView)
        try {
            title.text = ta.getString(R.styleable.AddressInputView_title)
            description.text = ta.getString(R.styleable.AddressInputView_description)
            showQrButton = ta.getBoolean(R.styleable.AddressInputView_showQrButton, true)
            input.hint = ta.getString(R.styleable.AddressInputView_hint)
        } finally {
            ta.recycle()
        }

        //  Add default buttons
        if (showQrButton) {
            actionsLayout.addView(buttonQrScan)
        }

        actionsLayout.addView(buttonPaste)
    }

    fun setText(text: String?) {
        input.setText(text)
        setDeleteButtonVisibility(!text.isNullOrBlank())
    }

    fun setHint(text: String?) {
        input.hint = text
    }

    fun setError(text: String?) {
        val isVisible = !text.isNullOrEmpty()
        error.text = text
        error.isVisible = isVisible

        // Highlights background with border color
        // todo: need to implement custom states
        inputBackground.isSelected = isVisible
    }

    fun setSpinner(isVisible: Boolean) {
        progressBar.isVisible = isVisible
    }

    fun onButtonQrScanClick(callback: () -> Unit) {
        buttonQrScan.setOnClickListener {
            callback()
        }
    }

    private fun setDeleteButtonVisibility(visible: Boolean) {
        buttonDelete.isVisible = visible
        buttonQrScan.isVisible = showQrButton && !visible
        buttonPaste.isVisible = !visible
    }

    fun setViewModel(viewModel: IVerifiedInputViewModel, lifecycleOwner: LifecycleOwner, onClickQrScan: () -> Unit) {
        input.maxLines = viewModel.inputFieldMaximumNumberOfLines

        viewModel.inputFieldSyncingLiveData.observe(lifecycleOwner, { visible ->
            setSpinner(visible)
        })

        viewModel.inputFieldValueLiveData.observe(lifecycleOwner, {
            setText(it)
        })

        viewModel.inputFieldCautionLiveData.observe(lifecycleOwner, {
            setError(it?.text)
        })

        setHint(viewModel.inputFieldPlaceholder)
        setText(viewModel.inputFieldInitialValue)

        input.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                viewModel.validateInputField()
            }
        }

        input.doOnTextChanged { text, _, _, _ ->
            viewModel.setInputFieldValue(text.toString())

            setDeleteButtonVisibility(!text.isNullOrBlank())
        }

        buttonPaste.setOnClickListener {
            input.setText(TextHelper.getCopiedText().trim())

            viewModel.validateInputField()
        }

        onButtonQrScanClick(onClickQrScan)
    }
}
