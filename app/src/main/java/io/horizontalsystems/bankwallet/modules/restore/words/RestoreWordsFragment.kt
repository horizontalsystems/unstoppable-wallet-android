package io.horizontalsystems.bankwallet.modules.restore.words

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.utils.Utils
import io.horizontalsystems.bankwallet.modules.restore.RestoreFragment
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.android.synthetic.main.fragment_restore_words.*
import kotlinx.android.synthetic.main.fragment_restore_words.toolbar

class RestoreWordsFragment : BaseFragment() {

    private lateinit var viewModel: RestoreWordsViewModel

    companion object {
        const val wordsCountKey = "wordsCountKey"
        const val titleKey = "titleKey"

        fun instance(wordsCount: Int, titleRes: Int): RestoreWordsFragment {
            return RestoreWordsFragment().apply {
                arguments = Bundle(2).apply {
                    putInt(RestoreWordsFragment.wordsCountKey, wordsCount)
                    putInt(RestoreWordsFragment.titleKey, titleRes)
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

        (activity as? AppCompatActivity)?.let {
            it.setSupportActionBar(toolbar)
            it.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }

        val wordsCount = arguments?.getInt(wordsCountKey) ?: throw Exception("Invalid words count")
        val accountTypeTitleRes = arguments?.getInt(titleKey) ?: throw Exception("Invalid title")

        viewModel = ViewModelProvider(this, RestoreWordsModule.Factory(wordsCount))
                .get(RestoreWordsViewModel::class.java)

        description.text = getString(R.string.Restore_Enter_Key_Description_Mnemonic, getString(accountTypeTitleRes), viewModel.wordCount.toString())

        setInputViewListeners()

        observe()
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
                viewModel.onProceed(words)
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

        viewModel.errorLiveData.observe(viewLifecycleOwner, Observer {
            HudHelper.showErrorMessage(this.requireView(), getString(R.string.Restore_ValidationFailed))
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
