package io.horizontalsystems.bankwallet.modules.restore.restoremnemonic

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.utils.Utils
import io.horizontalsystems.bankwallet.modules.restore.restoreselectcoins.RestoreSelectCoinsFragment.Companion.ACCOUNT_TYPE_KEY
import io.horizontalsystems.core.CoreApp
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.core.helpers.KeyboardHelper
import io.horizontalsystems.hdwalletkit.Mnemonic
import kotlinx.android.synthetic.main.fragment_restore_mnemonic.*
import kotlinx.android.synthetic.main.fragment_restore_mnemonic.passphrase
import kotlinx.android.synthetic.main.fragment_restore_mnemonic.passphraseDescription
import kotlinx.android.synthetic.main.fragment_restore_mnemonic.passphraseToggle
import kotlinx.android.synthetic.main.fragment_restore_mnemonic.toolbar
import kotlinx.android.synthetic.main.view_input_address.view.*

class RestoreMnemonicFragment : BaseFragment() {
    private val viewModel by viewModels<RestoreMnemonicViewModel> { RestoreMnemonicModule.Factory() }

    private val textWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable) {
            if (s.isNotEmpty()) {
                viewModel.onTextChange(s.toString(), wordsInput.selectionStart)
            }
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            isUsingNativeKeyboard()
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_restore_mnemonic, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.next -> {
                    viewModel.onProceed()
                    true
                }
                else -> false
            }
        }

        bindListeners()
        observeEvents()

        KeyboardHelper.showKeyboardDelayed(requireActivity(), wordsInput, 200)
    }

    private fun observeEvents() {
        viewModel.proceedLiveEvent.observe(viewLifecycleOwner, Observer { accountType ->
            hideKeyboard()
            findNavController().navigate(R.id.restoreSelectCoinsFragment, bundleOf(ACCOUNT_TYPE_KEY to accountType), navOptions())
        })

        viewModel.invalidRangesLiveData.observe(viewLifecycleOwner, Observer { invalidRanges ->
            wordsInput.removeTextChangedListener(textWatcher)

            val cursor = wordsInput.selectionStart
            val spannableString = SpannableString(wordsInput.text.toString())

            invalidRanges.forEach { range ->
                val spannableColorSpan = ForegroundColorSpan(requireContext().getColor(R.color.lucian))
                if (range.last < wordsInput.text.length) {
                    spannableString.setSpan(spannableColorSpan, range.first, range.last + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }

            wordsInput.setText(spannableString)
            wordsInput.setSelection(cursor)
            wordsInput.addTextChangedListener(textWatcher)
        })

        viewModel.errorLiveData.observe(viewLifecycleOwner, Observer {
            val errorMessage = when (it) {
                is RestoreMnemonicService.ValidationError.InvalidWordCountException -> getString(R.string.Restore_Error_MnemonicWordCount, it.count)
                is Mnemonic.ChecksumException -> getString(R.string.Restore_InvalidChecksum)
                else -> getString(R.string.Restore_ValidationFailed)
            }
            HudHelper.showErrorMessage(this.requireView(), errorMessage)
        })

        viewModel.inputsVisibleLiveData.observe(viewLifecycleOwner) {
            passphraseToggle.setChecked(it)
            passphrase.isVisible = it
            passphraseDescription.isVisible = it
        }

        viewModel.passphraseCautionLiveData.observe(viewLifecycleOwner) {
            passphrase.setError(it)
        }

        viewModel.clearInputsLiveEvent.observe(viewLifecycleOwner) {
            passphrase.setText(null)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun bindListeners() {
        wordsInput.addTextChangedListener(textWatcher)

        //fixes scrolling in EditText when it's inside NestedScrollView
        wordsInput.setOnTouchListener { v, event ->
            if (wordsInput.hasFocus()) {
                v.parent.requestDisallowInterceptTouchEvent(true)
                when (event.action and MotionEvent.ACTION_MASK) {
                    MotionEvent.ACTION_SCROLL -> {
                        v.parent.requestDisallowInterceptTouchEvent(false)
                        return@setOnTouchListener true
                    }
                }
            }
            return@setOnTouchListener false
        }

        passphraseToggle.setOnCheckedChangeListenerSingle {
            viewModel.onTogglePassphrase(it)
        }

        passphrase.onTextChange { old, new ->
            if (viewModel.validatePassphrase(new)) {
                viewModel.onChangePassphrase(new ?: "")
            } else {
                passphrase.revertText(old)
            }
        }
    }

    private fun isUsingNativeKeyboard(): Boolean {
        if (Utils.isUsingCustomKeyboard(requireContext()) && !CoreApp.thirdKeyboardStorage.isThirdPartyKeyboardAllowed) {
            showCustomKeyboardAlert()
            return false
        }

        return true
    }
}
