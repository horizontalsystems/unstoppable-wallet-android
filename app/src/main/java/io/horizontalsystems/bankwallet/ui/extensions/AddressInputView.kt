package io.horizontalsystems.bankwallet.ui.extensions

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.swap.tradeoptions.Caution
import io.horizontalsystems.bankwallet.modules.swap.tradeoptions.RecipientAddressViewModel
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.views.helpers.LayoutHelper
import kotlinx.android.synthetic.main.view_input_address.view.*

class AddressInputView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : ConstraintLayout(context, attrs, defStyleAttr) {

    private var showQrButton: Boolean = false
    private var onTextChangeCallback: ((text: String?) -> Unit)? = null
    private var onPasteCallback: ((text: String?) -> Unit)? = null

    private val textWatcher = object : TextWatcher {
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            onTextChangeCallback?.invoke(s?.toString())
            setDeleteButtonVisibility(!s.isNullOrBlank())
        }

        override fun afterTextChanged(s: Editable?) {}
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }
    }

    private val params = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
        rightMargin = LayoutHelper.dp(8f, context)
    }

    private val buttonPaste by lazy {
        createButton(context, R.style.ButtonSecondary, R.string.Send_Button_Paste, params) {
            onPasteCallback?.invoke(TextHelper.getCopiedText().trim())
        }
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
            title.isVisible = title.text.isNotEmpty()
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
        input.addTextChangedListener(textWatcher)
    }

    fun setText(text: String?, skipChangeEvent: Boolean = true) {
        input.apply {
            if (skipChangeEvent) {
                removeTextChangedListener(textWatcher)
            }

            setText(text)

            if (skipChangeEvent) {
                addTextChangedListener(textWatcher)
            }
        }

        setDeleteButtonVisibility(!text.isNullOrBlank())
    }

    fun setHint(text: String?) {
        input.hint = text
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

    fun onTextChange(callback: (String?) -> Unit) {
        onTextChangeCallback = callback
    }

    fun onPasteText(callback: (String?) -> Unit) {
        onPasteCallback = callback
    }

    fun setSpinner(isVisible: Boolean) {
        progressBar.isVisible = isVisible
    }

    fun onButtonQrScanClick(callback: () -> Unit) {
        buttonQrScan.setOnClickListener {
            callback()
        }
    }

    fun setEditable(isEditable: Boolean) {
        input.isEnabled = isEditable
    }

    private fun setDeleteButtonVisibility(visible: Boolean) {
        buttonDelete.isVisible = visible
        buttonQrScan.isVisible = showQrButton && !visible
        buttonPaste.isVisible = !visible
    }

    fun setViewModel(viewModel: RecipientAddressViewModel, lifecycleOwner: LifecycleOwner, onClickQrScan: () -> Unit) {
        setHint(viewModel.inputFieldPlaceholder)
        setText(viewModel.initialValue)

        viewModel.isLoadingLiveData.observe(lifecycleOwner, { visible ->
            setSpinner(visible)
        })

        viewModel.setTextLiveData.observe(lifecycleOwner, {
            setText(it, false)
        })

        viewModel.cautionLiveData.observe(lifecycleOwner, {
            setError(it)
        })

        input.setOnFocusChangeListener { _, hasFocus ->
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
