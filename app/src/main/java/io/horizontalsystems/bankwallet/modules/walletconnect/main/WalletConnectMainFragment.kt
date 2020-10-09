package io.horizontalsystems.bankwallet.modules.walletconnect.main

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.squareup.picasso.Picasso
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.modules.walletconnect.WalletConnectViewModel
import kotlinx.android.synthetic.main.fragment_wallet_connect_main.*

class WalletConnectMainFragment : Fragment(R.layout.fragment_wallet_connect_main) {

    private val baseViewModel by activityViewModels<WalletConnectViewModel>()
    private val presenter by lazy {
        baseViewModel.mainPresenter
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dappInfoAdapter = DappInfoAdapter()
        dappInfo.adapter = dappInfoAdapter

        presenter.connectingLiveData.observe(viewLifecycleOwner, Observer {
            connecting.isVisible = it
        })

        presenter.peerMetaLiveData.observe(viewLifecycleOwner, Observer { peerMetaViewItem ->
            dappGroup.isVisible = peerMetaViewItem != null

            peerMetaViewItem?.let {
                dappTitle.text = it.name
                it.icon?.let { Picasso.get().load(it).into(dappIcon) }

                dappInfoAdapter.url = it.url
            }
        })

        presenter.cancelVisibleLiveData.observe(viewLifecycleOwner, Observer {
            cancelButton.isVisible = it
        })

        presenter.approveAndRejectVisibleLiveData.observe(viewLifecycleOwner, Observer {
            approveButton.isVisible = it
            rejectButton.isVisible = it
        })

        presenter.disconnectVisibleLiveData.observe(viewLifecycleOwner, Observer {
            disconnectButton.isVisible = it
        })

        presenter.signedTransactionsVisibleLiveData.observe(viewLifecycleOwner, Observer {
            dappInfoAdapter.signedTransactionsVisible = it
        })

        presenter.hintLiveData.observe(viewLifecycleOwner, Observer { hint ->
            dappHint.text = hint?.let { getString(it) }
        })

        presenter.statusLiveData.observe(viewLifecycleOwner, Observer { status ->
            dappInfoAdapter.status = status
        })

        presenter.closeLiveEvent.observe(viewLifecycleOwner, Observer { hint ->
            requireActivity().finish()
        })

        approveButton.setOnSingleClickListener {
            presenter.approve()
        }

        rejectButton.setOnSingleClickListener {
            presenter.reject()
        }

        disconnectButton.setOnSingleClickListener {
            presenter.disconnect()
        }

        cancelButton.setOnSingleClickListener {
            parentFragmentManager.popBackStack()
        }
    }
}

