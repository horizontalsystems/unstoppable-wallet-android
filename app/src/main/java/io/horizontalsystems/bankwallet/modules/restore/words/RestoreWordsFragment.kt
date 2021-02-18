package io.horizontalsystems.bankwallet.modules.restore.words

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.*
import android.text.style.ForegroundColorSpan
import android.view.*
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.utils.Utils
import io.horizontalsystems.bankwallet.modules.restore.RestoreFragment
import io.horizontalsystems.bankwallet.modules.restore.words.RestoreWordsModule.RestoreAccountType
import io.horizontalsystems.bankwallet.modules.restore.words.RestoreWordsService.RestoreWordsException
import io.horizontalsystems.core.CoreApp
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.core.helpers.KeyboardHelper
import kotlinx.android.synthetic.main.fragment_add_token.*
import kotlinx.android.synthetic.main.fragment_restore_words.*
import kotlinx.android.synthetic.main.fragment_restore_words.toolbar
import kotlinx.android.synthetic.main.view_input_address.view.*

class RestoreWordsFragment : BaseFragment() {

    private lateinit var viewModel: RestoreWordsViewModel

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
        return inflater.inflate(R.layout.fragment_restore_words, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menuRestore -> {
                    viewModel.onProceed(additionalInfo.input.text.toString())
                    true
                }
                else -> false
            }
        }

        val wordsCount = arguments?.getParcelable<RestoreAccountType>(restoreAccountTypeKey) ?: throw Exception("Invalid restore account type")
        val accountTypeTitleRes = arguments?.getInt(titleKey) ?: throw Exception("Invalid title")

        viewModel = ViewModelProvider(this, RestoreWordsModule.Factory(wordsCount))
                .get(RestoreWordsViewModel::class.java)

        description.text = getString(R.string.Restore_Enter_Key_Description_Mnemonic, getString(accountTypeTitleRes), viewModel.wordCount.toString())
        additionalInfo.isVisible = viewModel.birthdayHeightEnabled
        additionalInfo.onPasteText {
            additionalInfo.setText(it)
        }

        bindListeners()
        observeEvents()

        activity?.let {
            KeyboardHelper.showKeyboardDelayed(it, wordsInput, 200)
        }
    }

    private fun observeEvents() {

        viewModel.accountTypeLiveEvent.observe(viewLifecycleOwner, Observer { accountType ->
            hideKeyboard()
            setFragmentResult(RestoreFragment.accountTypeRequestKey, bundleOf(RestoreFragment.accountTypeBundleKey to accountType))
        })

        viewModel.invalidRanges.observe(viewLifecycleOwner, Observer { invalidRanges ->
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
                is RestoreWordsException.InvalidBirthdayHeightException -> getString(R.string.Restore_BirthdayHeight_InvalidError)
                is RestoreWordsException.InvalidWordCountException -> getString(R.string.Restore_InvalidWordCount, it.count, it.requiredCount)
                is RestoreWordsException.ChecksumException -> getString(R.string.Restore_InvalidChecksum)
                else -> getString(R.string.Restore_ValidationFailed)
            }
            HudHelper.showErrorMessage(this.requireView(), errorMessage)
        })
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
    }

    private fun isUsingNativeKeyboard(): Boolean {
        if (Utils.isUsingCustomKeyboard(requireContext()) && !CoreApp.thirdKeyboardStorage.isThirdPartyKeyboardAllowed) {
            showCustomKeyboardAlert()
            return false
        }

        return true
    }

    companion object {
        const val restoreAccountTypeKey = "restoreAccountTypeKey"
        const val titleKey = "titleKey"

        fun instance(restoreAccountType: RestoreAccountType, titleRes: Int): RestoreWordsFragment {
            return RestoreWordsFragment().apply {
                arguments = Bundle(2).apply {
                    putParcelable(restoreAccountTypeKey, restoreAccountType)
                    putInt(titleKey, titleRes)
                }
            }
        }
    }
}
