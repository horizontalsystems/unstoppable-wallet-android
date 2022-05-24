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
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.core.utils.Utils
import io.horizontalsystems.bankwallet.databinding.FragmentRestoreMnemonicBinding
import io.horizontalsystems.bankwallet.modules.restore.restoreblockchains.RestoreBlockchainsFragment.Companion.ACCOUNT_NAME_KEY
import io.horizontalsystems.bankwallet.modules.restore.restoreblockchains.RestoreBlockchainsFragment.Companion.ACCOUNT_TYPE_KEY
import io.horizontalsystems.core.CoreApp
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.core.helpers.KeyboardHelper
import io.horizontalsystems.hdwalletkit.Mnemonic

class RestoreMnemonicFragment : BaseFragment() {
    private val viewModel by viewModels<RestoreMnemonicViewModel> { RestoreMnemonicModule.Factory() }

    private val accountNameTextWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable) {
            if (s.isNotEmpty()) {
                viewModel.onNameChange(s.toString())
            }
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }
    }

    private val textWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable) {
            if (s.isNotEmpty()) {
                viewModel.onTextChange(s.toString(), binding.wordsInput.selectionStart)
            }
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            isUsingNativeKeyboard()
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }
    }

    private var _binding: FragmentRestoreMnemonicBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRestoreMnemonicBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        binding.toolbar.setOnMenuItemClickListener { item ->
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

        KeyboardHelper.showKeyboardDelayed(requireActivity(), binding.wordsInput, 200)
    }

    private fun observeEvents() {
        viewModel.proceedLiveEvent.observe(viewLifecycleOwner) { (accountName, accountType) ->
            hideKeyboard()
            findNavController().slideFromRight(
                R.id.restoreSelectCoinsFragment,
                bundleOf(
                    ACCOUNT_NAME_KEY to accountName,
                    ACCOUNT_TYPE_KEY to accountType
                )
            )
        }

        viewModel.invalidRangesLiveData.observe(viewLifecycleOwner, Observer { invalidRanges ->
            binding.wordsInput.removeTextChangedListener(textWatcher)

            val cursor = binding.wordsInput.selectionStart
            val spannableString = SpannableString(binding.wordsInput.text.toString())

            invalidRanges.forEach { range ->
                val spannableColorSpan =
                    ForegroundColorSpan(requireContext().getColor(R.color.lucian))
                if (range.last < binding.wordsInput.text.length) {
                    spannableString.setSpan(
                        spannableColorSpan,
                        range.first,
                        range.last + 1,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }

            binding.wordsInput.setText(spannableString)
            binding.wordsInput.setSelection(cursor)
            binding.wordsInput.addTextChangedListener(textWatcher)
        })

        viewModel.errorLiveData.observe(viewLifecycleOwner, Observer {
            val errorMessage = when (it) {
                is RestoreMnemonicService.ValidationError.InvalidWordCountException -> getString(
                    R.string.Restore_Error_MnemonicWordCount,
                    it.count
                )
                is Mnemonic.ChecksumException -> getString(R.string.Restore_InvalidChecksum)
                else -> getString(R.string.Restore_ValidationFailed)
            }
            HudHelper.showErrorMessage(this.requireView(), errorMessage)
        })

        viewModel.inputsVisibleLiveData.observe(viewLifecycleOwner) {
            binding.passphraseToggle.setChecked(it)
            binding.passphrase.isVisible = it
            binding.passphraseDescription.isVisible = it
        }

        viewModel.passphraseCautionLiveData.observe(viewLifecycleOwner) {
            binding.passphrase.setError(it)
        }

        viewModel.clearInputsLiveEvent.observe(viewLifecycleOwner) {
            binding.passphrase.setText(null)
        }

        viewModel.placeholderLiveData.observe(viewLifecycleOwner) {
            binding.accountNameInput.hint = it
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun bindListeners() {
        binding.accountNameInput.addTextChangedListener(accountNameTextWatcher)
        binding.wordsInput.addTextChangedListener(textWatcher)

        //fixes scrolling in EditText when it's inside NestedScrollView
        binding.wordsInput.setOnTouchListener { v, event ->
            if (binding.wordsInput.hasFocus()) {
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

        binding.passphraseToggle.setOnCheckedChangeListenerSingle {
            viewModel.onTogglePassphrase(it)
        }

        binding.passphrase.onTextChange { old, new ->
            if (viewModel.validatePassphrase(new)) {
                viewModel.onChangePassphrase(new ?: "")
            } else {
                binding.passphrase.revertText(old)
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
