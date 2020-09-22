package io.horizontalsystems.bankwallet.modules.createwallet

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.ui.extensions.CoinListBaseFragment
import io.horizontalsystems.views.TopMenuItem
import kotlinx.android.synthetic.main.manage_wallets_fragment.*

class CreateWalletFragment : CoinListBaseFragment() {

    companion object {
        const val fragmentTag = "createWalletFragment"
        fun instance(predefinedAccountType: PredefinedAccountType? = null): CreateWalletFragment {
            return CreateWalletFragment().apply {
                arguments = Bundle(1).apply {
                    putParcelable("predefinedAccountType", predefinedAccountType)
                }
            }
        }
    }

    private lateinit var viewModel: CreateWalletViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        shadowlessToolbar.bind(
                getString(R.string.ManageCoins_title),
                leftBtnItem = TopMenuItem(R.drawable.ic_back) { onBackPress() },
                rightBtnItem = TopMenuItem(text = R.string.Button_Create, onClick = {
                    hideKeyboard()
                    Handler().postDelayed({
                        viewModel.onCreate()
                    }, 100)
                })
        )
        //disable create button
        shadowlessToolbar.setRightButtonEnabled(false)

        val predefinedAccountType = arguments?.getParcelable<PredefinedAccountType>("predefinedAccountType")

        viewModel = ViewModelProvider(this, CreateWalletModule.Factory(predefinedAccountType))
                .get(CreateWalletViewModel::class.java)

        observe()
    }

    // ManageWalletItemsAdapter.Listener

    override fun enable(coin: Coin) {
        viewModel.enable(coin)
    }

    override fun disable(coin: Coin) {
        viewModel.disable(coin)
    }

    override fun select(coin: Coin) {
        //not used here
    }

    // CoinListBaseFragment

    override fun updateFilter(query: String) {
        viewModel.updateFilter(query)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        requireActivity().onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                onBackPress()
            }
        })
    }

    private fun onBackPress() {
        hideKeyboard()
        closeCreateWalletFragment(CreateWalletActivity.Result.Cancelation)
    }

    private fun closeCreateWalletFragment(result: CreateWalletActivity.Result) {
        if (activity is CreateWalletActivity) {
            (activity as? CreateWalletActivity)?.close(result)
        } else {
            parentFragmentManager.popBackStack(fragmentTag, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }
    }

    private fun observe() {
        viewModel.viewItemsLiveData.observe(viewLifecycleOwner, Observer { items ->
            setItems(items)
        })

        viewModel.canCreateLiveData.observe(viewLifecycleOwner, Observer { enabled ->
            shadowlessToolbar.setRightButtonEnabled(enabled)
        })

        viewModel.finishLiveEvent.observe(viewLifecycleOwner, Observer {
            closeCreateWalletFragment(CreateWalletActivity.Result.Success)
        })
    }

}
