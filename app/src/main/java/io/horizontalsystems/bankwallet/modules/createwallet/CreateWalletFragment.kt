package io.horizontalsystems.bankwallet.modules.createwallet

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.managewallets.view.ManageWalletItemsAdapter
import io.horizontalsystems.views.TopMenuItem
import kotlinx.android.synthetic.main.manage_wallets_fragment.*

class CreateWalletFragment : BaseFragment(), ManageWalletItemsAdapter.Listener {

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
    private lateinit var adapter: ManageWalletItemsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.manage_wallets_fragment, container, false)
    }

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

        adapter = ManageWalletItemsAdapter(this)
        recyclerView.adapter = adapter

        searchView.bind(
                hint = getString(R.string.ManageCoins_Search),
                onTextChanged = { query ->
                    viewModel.updateFilter(query)
                })

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
            adapter.viewItems = items
            adapter.notifyDataSetChanged()

            progressLoading.isVisible = false
        })

        viewModel.canCreateLiveData.observe(viewLifecycleOwner, Observer { enabled ->
            shadowlessToolbar.setRightButtonEnabled(enabled)
        })

        viewModel.finishLiveEvent.observe(viewLifecycleOwner, Observer {
            closeCreateWalletFragment(CreateWalletActivity.Result.Success)
        })
    }

}
