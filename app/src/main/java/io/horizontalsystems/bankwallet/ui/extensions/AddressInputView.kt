package io.horizontalsystems.bankwallet.ui.extensions

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
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
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.lifecycle.*
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.databinding.ViewInputAddressBinding
import io.horizontalsystems.bankwallet.modules.swap.settings.Caution
import io.horizontalsystems.bankwallet.ui.compose.ColoredTextStyle
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryCircle
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryDefault
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper

class AddressInputView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding = ViewInputAddressBinding.inflate(LayoutInflater.from(context), this)

    interface Listener {
        fun onTextChange(text: String)
        fun onQrButtonClick()
        fun onFocusChange(hasFocus: Boolean)
    }

    init {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.AddressInputView)
        try {
            binding.title.text = ta.getString(R.styleable.AddressInputView_title)
            binding.title.isVisible = binding.title.text.isNotEmpty()
            binding.description.text = ta.getString(R.styleable.AddressInputView_description)
        } finally {
            ta.recycle()
        }
    }

    private lateinit var listener: Listener

    private val viewModel by lazy {
        ViewModelProvider(ViewTreeViewModelStoreOwner.get(this)!!).get<AddressInputViewModel>()
    }

    private fun drawView() {
        binding.actionsCompose.setContent {
            ComposeAppTheme {
                AddressInputViewComponent(viewModel, listener)
            }
        }
    }

    private fun observe() {
        findViewTreeLifecycleOwner()?.let { lifecycleOwner ->
            viewModel.inputLiveData.observe(lifecycleOwner, Observer {
                listener.onTextChange(it.second)
            })
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        drawView()
        observe()
    }

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    fun setText(text: String?) {
        viewModel.setText(text ?: "")
    }

    fun setHint(text: String) {
        viewModel.setHint(text)
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

    fun setSpinner(isVisible: Boolean) {
        viewModel.setSpinner(isVisible)
    }

    fun setEditable(editable: Boolean) {
        viewModel.setInputEditable(editable)
    }
}

@Composable
fun AddressInputViewComponent(
    viewModel: AddressInputViewModel,
    listener: AddressInputView.Listener
) {
    val inputData by viewModel.inputLiveData.observeAsState(Pair(0, ""))
    var text by remember(inputData.first) { mutableStateOf(inputData.second) }

    val inputHint by viewModel.inputHintLiveData.observeAsState("")
    val inputEditable by viewModel.inputEditableLiveData.observeAsState(true)
    val buttonsData by viewModel.buttonsLiveData.observeAsState(listOf<AddressInputButton>())
    val showSpinner by viewModel.showSpinnerLiveData.observeAsState(false)

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
                listener.onFocusChange(it.hasFocus)
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
                value = text,
                onValueChange = {
                    text = it
                    viewModel.updateText(it)
                },
                textStyle = ColoredTextStyle(
                    color = ComposeAppTheme.colors.oz,
                    textStyle = ComposeAppTheme.typography.body
                ),
                //input hint
                decorationBox = { innerTextField ->
                    if (text.isEmpty()) {
                        Text(
                            inputHint,
                            color = ComposeAppTheme.colors.grey50,
                            style = ComposeAppTheme.typography.body
                        )
                    }
                    innerTextField()
                },
                cursorBrush = SolidColor(ComposeAppTheme.colors.oz),
                enabled = inputEditable
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
            buttonsData.forEach {
                when (it) {
                    AddressInputButton.Delete -> {
                        ButtonSecondaryCircle(
                            icon = R.drawable.ic_delete_20,
                            onClick = {
                                viewModel.setText("")
                            }
                        )
                    }
                    AddressInputButton.Scan -> {
                        ButtonSecondaryCircle(
                            modifier = Modifier.padding(end = 8.dp),
                            icon = R.drawable.ic_qr_scan_20,
                            onClick = {
                                listener.onQrButtonClick()
                            }
                        )
                    }
                    AddressInputButton.Paste -> {
                        ButtonSecondaryDefault(
                            modifier = Modifier.padding(0.dp),
                            title = stringResource(R.string.Send_Button_Paste),
                            onClick = {
                                viewModel.setText(TextHelper.getCopiedText().trim())
                            }
                        )
                    }
                }
            }
        }
    }
}

enum class AddressInputButton {
    Delete, Scan, Paste
}

class AddressInputViewModel : ViewModel() {

    private var inputText = ""
    private var textUpdateKey = 0

    val buttonsLiveData = MutableLiveData(getButtons())
    val showSpinnerLiveData = MutableLiveData(false)
    val inputLiveData = MutableLiveData(Pair(textUpdateKey, inputText))
    val inputHintLiveData = MutableLiveData("")
    val inputEditableLiveData = MutableLiveData(true)

    private fun getButtons(): List<AddressInputButton> = when {
        inputText.isEmpty() -> listOf(AddressInputButton.Scan, AddressInputButton.Paste)
        else -> listOf(AddressInputButton.Delete)
    }

    private fun sync() {
        textUpdateKey++
        buttonsLiveData.postValue(getButtons())
        inputLiveData.postValue(Pair(textUpdateKey, inputText))
    }

    fun setText(text: String) {
        inputText = text
        sync()
    }

    fun setSpinner(visible: Boolean) {
        showSpinnerLiveData.postValue(visible)
    }

    fun setHint(hint: String) {
        inputHintLiveData.postValue(hint)
    }

    fun setInputEditable(editable: Boolean) {
        inputEditableLiveData.postValue(editable)
    }

    fun updateText(text: String) {
        inputText = text
        buttonsLiveData.postValue(getButtons())
        inputLiveData.postValue(Pair(textUpdateKey, inputText))
    }

}
