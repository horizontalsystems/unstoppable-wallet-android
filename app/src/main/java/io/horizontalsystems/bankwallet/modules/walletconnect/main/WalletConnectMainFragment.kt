package io.horizontalsystems.bankwallet.modules.walletconnect.main

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.Group
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.RecyclerView
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
import io.horizontalsystems.bankwallet.modules.walletconnect.scanqr.WalletConnectScanQrModule
import io.horizontalsystems.bankwallet.modules.walletconnect.scanqr.WalletConnectScanQrViewModel
import io.horizontalsystems.bankwallet.ui.extensions.ConfirmationDialog
import io.horizontalsystems.bankwallet.ui.extensions.PicassoRoundedImageView

class WalletConnectMainFragment : BaseFragment() {

    private val baseViewModel by navGraphViewModels<WalletConnectViewModel>(R.id.walletConnectMainFragment) {
        WalletConnectModule.Factory(arguments?.getString(WalletConnectMainModule.REMOTE_PEER_ID_KEY))
    }
    private val viewModelScan by viewModels<WalletConnectScanQrViewModel> { WalletConnectScanQrModule.Factory(baseViewModel.service) }
    private val viewModel by viewModels<WalletConnectMainViewModel> { WalletConnectMainModule.Factory(baseViewModel.service) }
    private var closeMenuItem: MenuItem? = null

    private val qrScannerResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                result.data?.getStringExtra(ModuleField.SCAN_ADDRESS)?.let {
                    viewModelScan.handleScanned(it)
                }
            }
            Activity.RESULT_CANCELED -> {
                val sessionsCount = arguments?.getInt(WalletConnectMainModule.SESSIONS_COUNT_KEY) ?: 0
                if (sessionsCount == 0){
                    findNavController().popBackStack(R.id.mainFragment, false)
                } else {
                    findNavController().popBackStack()
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_wallet_connect_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar = view.findViewById<Toolbar>(R.id.toolbar)
        val connecting = view.findViewById<ProgressBar>(R.id.connecting)
        val dappGroup = view.findViewById<Group>(R.id.dappGroup)
        val dappTitle = view.findViewById<TextView>(R.id.dappTitle)
        val dappIcon = view.findViewById<PicassoRoundedImageView>(R.id.dappIcon)
        val cancelButton = view.findViewById<Button>(R.id.cancelButton)
        val connectButton = view.findViewById<Button>(R.id.connectButton)
        val disconnectButton = view.findViewById<Button>(R.id.disconnectButton)
        val dappHint = view.findViewById<TextView>(R.id.dappHint)

        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menuClose -> {
                    findNavController().popBackStack()
                    true
                }
                else -> {
                    false
                }
            }
        }
        closeMenuItem = toolbar.menu.findItem(R.id.menuClose)

        view.isVisible = false

        when (baseViewModel.initialScreen) {
            WalletConnectViewModel.InitialScreen.ScanQrCode -> {
                val intent = QRScannerActivity.getIntentForFragment(this)
                qrScannerResultLauncher.launch(intent)
            }
            WalletConnectViewModel.InitialScreen.Main -> {
                view.isVisible = true
            }
        }

        viewModelScan.openErrorLiveEvent.observe(this, {
            val message = when (it) {
                is WalletConnectInteractor.SessionError.InvalidUri -> getString(R.string.WalletConnect_Error_InvalidUrl)
                else -> it.message ?: getString(R.string.default_error_msg)
            }

            findNavController().navigate(R.id.walletConnectMainFragment_to_walletConnectErrorFragment, bundleOf(WalletConnectErrorFragment.MESSAGE_KEY to message))
        })

        viewModelScan.openMainLiveEvent.observe(this, {
            view.isVisible = true
        })


        val dappInfoAdapter = DappInfoAdapter()
        view.findViewById<RecyclerView>(R.id.dappInfo).adapter = dappInfoAdapter

        viewModel.connectingLiveData.observe(viewLifecycleOwner, {
            connecting.isVisible = it
        })

        viewModel.peerMetaLiveData.observe(viewLifecycleOwner, { peerMetaViewItem ->
            dappGroup.isVisible = peerMetaViewItem != null

            peerMetaViewItem?.let {
                dappTitle.text = it.name
                dappIcon.loadImage(it.icon)
                dappInfoAdapter.url = it.url
            }
        })

        viewModel.cancelVisibleLiveData.observe(viewLifecycleOwner, {
            cancelButton.isVisible = it
        })

        viewModel.connectButtonLiveData.observe(viewLifecycleOwner, {
            connectButton.isVisible = it.visible
            connectButton.isEnabled = it.enabled
        })

        viewModel.disconnectButtonLiveData.observe(viewLifecycleOwner, {
            disconnectButton.isVisible = it.visible
            disconnectButton.isEnabled = it.enabled
        })

        viewModel.closeVisibleLiveData.observe(viewLifecycleOwner, {
            closeMenuItem?.isVisible = it
        })

        viewModel.signedTransactionsVisibleLiveData.observe(viewLifecycleOwner, {
            dappInfoAdapter.signedTransactionsVisible = it
        })

        viewModel.hintLiveData.observe(viewLifecycleOwner, { hint ->
            dappHint.text = hint?.let { getString(it) }
        })

        viewModel.statusLiveData.observe(viewLifecycleOwner, { status ->
            dappInfoAdapter.status = status
        })

        viewModel.closeLiveEvent.observe(viewLifecycleOwner, {
            findNavController().popBackStack()
        })

        viewModel.openRequestLiveEvent.observe(viewLifecycleOwner, {
            if (it is WalletConnectSendEthereumTransactionRequest) {
                baseViewModel.sharedSendEthereumTransactionRequest = it

                findNavController().navigate(R.id.walletConnectMainFragment_to_walletConnectSendEthereumTransactionRequestFragment, null, navOptionsFromBottom())
            }
        })

        connectButton.setOnSingleClickListener {
            viewModel.connect()
        }

        disconnectButton.setOnSingleClickListener {
            ConfirmationDialog.show(
                    icon = R.drawable.ic_wallet_connect_24,
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

}

