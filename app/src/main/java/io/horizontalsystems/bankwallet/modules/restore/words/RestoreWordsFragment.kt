package io.horizontalsystems.bankwallet.modules.restore.words

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.extensions.NavDestinationChangeListener
import io.horizontalsystems.bankwallet.core.utils.Utils
import io.horizontalsystems.bankwallet.modules.restore.RestoreActivity
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.core.helpers.KeyboardHelper
import kotlinx.android.synthetic.main.fragment_restore_words.*


class RestoreWordsFragment : BaseFragment() {

    private lateinit var viewModel: RestoreWordsViewModel

    companion object {
        const val wordsCountKey = "wordsCountKey"
        const val titleKey = "titleKey"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_restore_words, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val navController = findNavController()
        val appBarConfiguration = AppBarConfiguration(navController.graph)

        val navDestinationChangeListener = NavDestinationChangeListener(toolbar, appBarConfiguration, true)
        navController.addOnDestinationChangedListener(navDestinationChangeListener)
        toolbar.setNavigationOnClickListener {
            hideKeyboard()
            activity?.onBackPressed()
        }

        toolbar.inflateMenu(R.menu.restore_menu)
        toolbar.setOnMenuItemClickListener { menuItem ->
            if (menuItem.itemId == R.id.menuRestore){
                val words = wordsInput.text?.toString() ?: ""
                viewModel.onProceed(words)
                true
            } else {
                super.onOptionsItemSelected(menuItem)
            }
        }

        val wordsCount = arguments?.getInt(wordsCountKey) ?: throw Exception("Invalid words count")
        val accountTypeTitleRes = arguments?.getInt(titleKey) ?: throw Exception("Invalid title")

        viewModel = ViewModelProvider(this, RestoreWordsModule.Factory(wordsCount))
                .get(RestoreWordsViewModel::class.java)

        description.text = getString(R.string.Restore_Enter_Key_Description_Mnemonic, getString(accountTypeTitleRes), viewModel.wordCount.toString())

        setInputViewListeners()

        observe()

        activity?.let {
            KeyboardHelper.showKeyboardDelayed(it, wordsInput, 500)
        }
    }

    private fun observe() {
        viewModel.accountTypeLiveEvent.observe(viewLifecycleOwner, Observer { accountType ->
            hideKeyboard()
            (activity as? RestoreActivity)?.onRestore(accountType)
        })

        viewModel.errorLiveData.observe(viewLifecycleOwner, Observer {
            hideKeyboard()
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
