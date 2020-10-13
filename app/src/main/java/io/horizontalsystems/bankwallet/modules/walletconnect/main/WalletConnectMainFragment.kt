package io.horizontalsystems.bankwallet.modules.walletconnect.main

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.squareup.picasso.Picasso
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.modules.walletconnect.WalletConnectActivity
import io.horizontalsystems.bankwallet.modules.walletconnect.WalletConnectViewModel
import io.horizontalsystems.bankwallet.modules.walletconnect.request.WalletConnectRequestFragment
import kotlinx.android.synthetic.main.fragment_wallet_connect_main.*
import kotlinx.android.synthetic.main.fragment_wallet_connect_main.toolbar

class WalletConnectMainFragment : Fragment(R.layout.fragment_wallet_connect_main) {

    private var closeVisible: Boolean = false
        set(value) {
            field = value

            requireActivity().invalidateOptionsMenu()
        }

    private val baseViewModel by activityViewModels<WalletConnectViewModel>()
    private val viewModel by viewModels<WalletConnectMainViewModel> { WalletConnectMainModule.Factory(baseViewModel.service) }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.wallet_connect_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menuClose) {
            requireActivity().onBackPressed()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.findItem(R.id.menuClose)?.isVisible = closeVisible
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)
        (activity as? AppCompatActivity)?.setSupportActionBar(toolbar)

        val dappInfoAdapter = DappInfoAdapter()
        dappInfo.adapter = dappInfoAdapter

        viewModel.connectingLiveData.observe(viewLifecycleOwner, Observer {
            connecting.isVisible = it
        })

        viewModel.peerMetaLiveData.observe(viewLifecycleOwner, Observer { peerMetaViewItem ->
            dappGroup.isVisible = peerMetaViewItem != null

            peerMetaViewItem?.let {
                dappTitle.text = it.name
                it.icon?.let { Picasso.get().load(it).into(dappIcon) }

                dappInfoAdapter.url = it.url
            }
        })

        viewModel.cancelVisibleLiveData.observe(viewLifecycleOwner, Observer {
            cancelButton.isVisible = it
        })

        viewModel.approveAndRejectVisibleLiveData.observe(viewLifecycleOwner, Observer {
            approveButton.isVisible = it
            rejectButton.isVisible = it
        })

        viewModel.disconnectVisibleLiveData.observe(viewLifecycleOwner, Observer {
            disconnectButton.isVisible = it
        })

        viewModel.closeVisibleLiveData.observe(viewLifecycleOwner, Observer {
            closeVisible = it
        })

        viewModel.signedTransactionsVisibleLiveData.observe(viewLifecycleOwner, Observer {
            dappInfoAdapter.signedTransactionsVisible = it
        })

        viewModel.hintLiveData.observe(viewLifecycleOwner, Observer { hint ->
            dappHint.text = hint?.let { getString(it) }
        })

        viewModel.statusLiveData.observe(viewLifecycleOwner, Observer { status ->
            dappInfoAdapter.status = status
        })

        viewModel.closeLiveEvent.observe(viewLifecycleOwner, Observer {
            requireActivity().finish()
        })

        viewModel.openRequestLiveEvent.observe(viewLifecycleOwner, Observer { id ->
            (requireActivity() as WalletConnectActivity).showBottomSheetFragment(WalletConnectRequestFragment.newInstance(id))
        })

        approveButton.setOnSingleClickListener {
            viewModel.approve()
        }

        rejectButton.setOnSingleClickListener {
            viewModel.reject()
        }

        disconnectButton.setOnSingleClickListener {
            viewModel.disconnect()
        }

        cancelButton.setOnSingleClickListener {
            requireActivity().finish()
        }
    }
}

