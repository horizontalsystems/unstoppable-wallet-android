package io.horizontalsystems.bankwallet.modules.walletconnect.main

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import com.google.zxing.integration.android.IntentIntegrator
import com.squareup.picasso.Picasso
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.managers.WalletConnectInteractor
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.modules.qrscanner.QRScannerActivity
import io.horizontalsystems.bankwallet.modules.walletconnect.WalletConnectErrorFragment
import io.horizontalsystems.bankwallet.modules.walletconnect.WalletConnectModule
import io.horizontalsystems.bankwallet.modules.walletconnect.WalletConnectSendEthereumTransactionRequest
import io.horizontalsystems.bankwallet.modules.walletconnect.WalletConnectViewModel
import io.horizontalsystems.bankwallet.modules.walletconnect.request.WalletConnectSendEthereumTransactionRequestFragment
import io.horizontalsystems.bankwallet.modules.walletconnect.scanqr.WalletConnectScanQrModule
import io.horizontalsystems.bankwallet.modules.walletconnect.scanqr.WalletConnectScanQrViewModel
import io.horizontalsystems.bankwallet.ui.extensions.ConfirmationDialog
import kotlinx.android.synthetic.main.fragment_wallet_connect_main.*

class WalletConnectMainFragment : BaseFragment() {

    private var closeVisible: Boolean = false
        set(value) {
            field = value

            requireActivity().invalidateOptionsMenu()
        }

    private val baseViewModel by navGraphViewModels<WalletConnectViewModel>(R.id.walletConnectMainFragment) { WalletConnectModule.Factory() }
    private val viewModelScan by viewModels<WalletConnectScanQrViewModel> { WalletConnectScanQrModule.Factory(baseViewModel.service) }
    private val viewModel by viewModels<WalletConnectMainViewModel> { WalletConnectMainModule.Factory(baseViewModel.service) }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.wallet_connect_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menuClose) {
            findNavController().popBackStack()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.findItem(R.id.menuClose)?.isVisible = closeVisible
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_wallet_connect_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.isVisible = false

        setHasOptionsMenu(true)
        (activity as? AppCompatActivity)?.setSupportActionBar(toolbar)

        when (baseViewModel.initialScreen) {
            WalletConnectViewModel.InitialScreen.NoEthereumKit -> { }
            WalletConnectViewModel.InitialScreen.ScanQrCode -> {
                QRScannerActivity.start(this)
            }
            WalletConnectViewModel.InitialScreen.Main -> {
                view.isVisible = true
            }
        }

        viewModelScan.openErrorLiveEvent.observe(this, Observer {
            val message = when (it) {
                is WalletConnectInteractor.SessionError.InvalidUri -> getString(R.string.WalletConnect_Error_InvalidUrl)
                else -> it.message ?: getString(R.string.default_error_msg)
            }

            findNavController().navigate(R.id.walletConnectMainFragment_to_walletConnectErrorFragment, bundleOf(WalletConnectErrorFragment.MESSAGE_KEY to message))
        })

        viewModelScan.openMainLiveEvent.observe(this, Observer {
            view.isVisible = true
        })


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

        viewModel.connectButtonLiveData.observe(viewLifecycleOwner, Observer {
            connectButton.isVisible = it.visible
            connectButton.isEnabled = it.enabled
        })

        viewModel.disconnectButtonLiveData.observe(viewLifecycleOwner, Observer {
            disconnectButton.isVisible = it.visible
            disconnectButton.isEnabled = it.enabled
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
            findNavController().popBackStack()
        })

        viewModel.openRequestLiveEvent.observe(viewLifecycleOwner, Observer {
            if (it is WalletConnectSendEthereumTransactionRequest) {
                baseViewModel.sharedSendEthereumTransactionRequest = it

                findNavController().navigate(R.id.walletConnectMainFragment_to_walletConnectSendEthereumTransactionRequestFragment, null, navOptions())
            }
        })

        findNavController().currentBackStackEntry?.savedStateHandle?.let { savedStateHandle ->
            savedStateHandle
                    .getLiveData<WalletConnectSendEthereumTransactionRequestFragment.ApproveResult>("ApproveResult")
                    .observe(viewLifecycleOwner, Observer { approveResult ->
                        baseViewModel.sharedSendEthereumTransactionRequest?.let { sendEthereumTransactionRequest ->
                            when (approveResult) {
                                is WalletConnectSendEthereumTransactionRequestFragment.ApproveResult.Approved -> {
                                    viewModel.approveRequest(sendEthereumTransactionRequest.id, approveResult.txHash)
                                }
                                WalletConnectSendEthereumTransactionRequestFragment.ApproveResult.Rejected -> {
                                    viewModel.rejectRequest(sendEthereumTransactionRequest.id)
                                }
                            }

                            baseViewModel.sharedSendEthereumTransactionRequest = null
                        }
                    })
        }

        connectButton.setOnSingleClickListener {
            viewModel.connect()
        }

        disconnectButton.setOnSingleClickListener {
            ConfirmationDialog.show(
                    icon = R.drawable.ic_wallet_connect,
                    title = getString(R.string.Button_Disconnect),
                    subtitle = dappTitle.text.toString(),
                    contentText = null,
                    actionButtonTitle = null,
                    destructiveButtonTitle = getString(R.string.Button_Disconnect),
                    cancelButtonTitle = getString(R.string.Button_Cancel),
                    activity = requireActivity(),
                    listener = object : ConfirmationDialog.Listener {
                        override fun onDestructiveButtonClick() {
                            viewModel.disconnect()
                        }
                    }
            )
        }

        cancelButton.setOnSingleClickListener {
            viewModel.cancel()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == IntentIntegrator.REQUEST_CODE) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    data?.getStringExtra(ModuleField.SCAN_ADDRESS)?.let {
                        Log.e("AAA", "Scanned string: $it")
                        viewModelScan.handleScanned(it)
                    }
                }
                Activity.RESULT_CANCELED -> {
                    findNavController().popBackStack()
                }
            }
        }
    }

}

