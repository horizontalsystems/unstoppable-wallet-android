package io.horizontalsystems.bankwallet.modules.restore.words

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.utils.Utils
import io.horizontalsystems.bankwallet.modules.restore.RestoreFragment
import io.horizontalsystems.bankwallet.modules.restore.words.RestoreWordsModule.RestoreAccountType
import io.horizontalsystems.bankwallet.modules.restore.words.RestoreWordsService.RestoreWordsException
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.core.helpers.KeyboardHelper
import kotlinx.android.synthetic.main.fragment_restore_words.*
import kotlinx.android.synthetic.main.view_input_address.view.*

class RestoreWordsFragment : BaseFragment() {

    private lateinit var viewModel: RestoreWordsViewModel

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_restore_words, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        setSupportActionBar(toolbar, true)

        val wordsCount = arguments?.getParcelable<RestoreAccountType>(restoreAccountTypeKey) ?: throw Exception("Invalid restore account type")
        val accountTypeTitleRes = arguments?.getInt(titleKey) ?: throw Exception("Invalid title")

        viewModel = ViewModelProvider(this, RestoreWordsModule.Factory(wordsCount))
                .get(RestoreWordsViewModel::class.java)

        description.text = getString(R.string.Restore_Enter_Key_Description_Mnemonic, getString(accountTypeTitleRes), viewModel.wordCount.toString())
        additionalInfo.isVisible = viewModel.hasAdditionalInfo

        setInputViewListeners()

        observe()

        activity?.let {
            KeyboardHelper.showKeyboardDelayed(it, wordsInput, 200)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.restore_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menuRestore -> {
                val words = wordsInput.text?.toString() ?: ""
                viewModel.onProceed(words, additionalInfo.input.text.toString())
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun observe() {
        viewModel.accountTypeLiveEvent.observe(viewLifecycleOwner, Observer { accountType ->
            hideKeyboard()
            setFragmentResult(RestoreFragment.accountTypeRequestKey, bundleOf(RestoreFragment.accountTypeBundleKey to accountType))
        })

        viewModel.errorLiveData.observe(viewLifecycleOwner, {
            val errorMessage = when (it) {
                is RestoreWordsException.InvalidBirthdayHeightException -> getString(R.string.Restore_BirthdayHeight_InvalidError)
                else -> getString(R.string.Restore_ValidationFailed)
            }
            HudHelper.showErrorMessage(this.requireView(), errorMessage)
        })
    }

    private fun setInputViewListeners() {
        wordsInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                isUsingNativeKeyboard()
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

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
        if (Utils.isUsingCustomKeyboard(requireContext())) {
            (activity as? BaseActivity)?.showCustomKeyboardAlert()
            return false
        }

        return true
    }

}
