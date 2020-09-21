package io.horizontalsystems.bankwallet.modules.restore

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.FragmentResultListener
import androidx.fragment.app.commit
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType
import io.horizontalsystems.bankwallet.modules.restore.eos.RestoreEosFragment
import io.horizontalsystems.bankwallet.modules.restore.restoreselectcoins.RestoreSelectCoinsFragment
import io.horizontalsystems.bankwallet.modules.restore.restoreselectpredefinedaccounttype.RestoreSelectPredefinedAccountTypeFragment
import io.horizontalsystems.bankwallet.modules.restore.words.RestoreWordsFragment

class RestoreFragment : BaseFragment() {

    private lateinit var viewModel: RestoreViewModel

    companion object {
        const val selectPredefinedAccountTypeRequestKey = "selectPredefinedAccountTypeRequestKey"
        const val predefinedAccountTypeBundleKey = "predefinedAccountTypeBundleKey"
        const val accountTypeRequestKey = "accountTypeRequestKey"
        const val accountTypeBundleKey = "accountTypeBundleKey"
        const val selectCoinsRequestKey = "selectCoinsRequestKey"
        const val selectCoinsBundleKey = "selectCoinsBundleKey"

        fun instance(predefinedAccountType: PredefinedAccountType? = null, selectCoins: Boolean = true): RestoreFragment {
            return RestoreFragment().apply {
                arguments = Bundle(2).apply {
                    putParcelable("predefinedAccountType", predefinedAccountType)
                    putBoolean("selectCoins", selectCoins)
                }
            }
        }

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_restore, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val selectCoins = arguments?.getBoolean("selectCoins") ?: throw Exception("Parameter missing")
        val predefinedAccountType = arguments?.getParcelable<PredefinedAccountType>("predefinedAccountType")

        viewModel = ViewModelProvider(this, RestoreModule.Factory(selectCoins, predefinedAccountType))
                .get(RestoreViewModel::class.java)

        openScreen(viewModel.initialScreen)

        observe()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        requireActivity().onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (childFragmentManager.backStackEntryCount > 0) {
                    childFragmentManager.popBackStack()
                }
            }
        })

        childFragmentManager.addOnBackStackChangedListener {
            if (childFragmentManager.backStackEntryCount == 0){
                requireActivity().onBackPressed()
            }
        }
    }

    private fun observe() {
        viewModel.openScreenLiveEvent.observe(viewLifecycleOwner, Observer {
            openScreen(it)
        })

        viewModel.finishLiveEvent.observe(viewLifecycleOwner, Observer {
            parentFragmentManager.popBackStack()
        })
    }

    private fun openScreen(screen: RestoreViewModel.Screen) {
        val fragment = getFragmentByScreen(screen)

        showChildFragment(fragment)
    }

    private fun getFragmentByScreen(screen: RestoreViewModel.Screen): BaseFragment {
        return when (screen) {
            RestoreViewModel.Screen.SelectPredefinedAccountType -> {
                setPredefinedAccountTypeListener()

                RestoreSelectPredefinedAccountTypeFragment()
            }
            is RestoreViewModel.Screen.RestoreAccountType -> {
                setAccountTypeListener()

                when (screen.predefinedAccountType) {
                    PredefinedAccountType.Standard,
                    PredefinedAccountType.Binance -> {
                        val wordsCount = if (screen.predefinedAccountType == PredefinedAccountType.Standard) 12 else 24
                        RestoreWordsFragment.instance(wordsCount, screen.predefinedAccountType.title)
                    }
                    PredefinedAccountType.Eos -> {
                        RestoreEosFragment()
                    }
                }
            }
            is RestoreViewModel.Screen.SelectCoins -> {
                setSelectCoinsListener()

                RestoreSelectCoinsFragment.instance(screen.predefinedAccountType)
            }
        }
    }

    private fun showChildFragment(fragment: BaseFragment) {
        childFragmentManager.commit {
            add(R.id.fragmentContainerView, fragment)
            addToBackStack(null)
        }
    }

    private fun setPredefinedAccountTypeListener() {
        childFragmentManager.setFragmentResultListener(selectPredefinedAccountTypeRequestKey, viewLifecycleOwner, FragmentResultListener { requestKey, result ->
            val predefinedAccountType = result.getParcelable<PredefinedAccountType>(predefinedAccountTypeBundleKey)
                    ?: return@FragmentResultListener
            viewModel.onSelect(predefinedAccountType)
        })
    }

    private fun setAccountTypeListener() {
        childFragmentManager.setFragmentResultListener(accountTypeRequestKey, viewLifecycleOwner, FragmentResultListener { requestKey, result ->
            val accountType = result.getParcelable<AccountType>(accountTypeBundleKey)
                    ?: return@FragmentResultListener
            viewModel.onEnter(accountType)
        })
    }

    private fun setSelectCoinsListener() {
        childFragmentManager.setFragmentResultListener(selectCoinsRequestKey, viewLifecycleOwner, FragmentResultListener { requestKey, result ->
            val selectedCoins = result.getParcelableArrayList<Coin>(selectCoinsBundleKey)
                    ?: return@FragmentResultListener
            viewModel.onSelect(selectedCoins)
        })
    }

}
