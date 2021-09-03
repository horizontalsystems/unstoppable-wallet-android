package io.horizontalsystems.bankwallet.modules.walletconnect.main

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.managers.WalletConnectInteractor
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.modules.qrscanner.QRScannerActivity
import io.horizontalsystems.bankwallet.modules.walletconnect.*
import io.horizontalsystems.bankwallet.modules.walletconnect.scanqr.WalletConnectScanQrModule
import io.horizontalsystems.bankwallet.modules.walletconnect.scanqr.WalletConnectScanQrViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryRed
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.android.synthetic.main.fragment_wallet_connect_main.*

class WalletConnectMainFragment : BaseFragment() {

    private val baseViewModel by navGraphViewModels<WalletConnectViewModel>(R.id.walletConnectMainFragment) {
        WalletConnectModule.Factory(arguments?.getString(WalletConnectMainModule.REMOTE_PEER_ID_KEY))
    }
    private val viewModelScan by viewModels<WalletConnectScanQrViewModel> { WalletConnectScanQrModule.Factory(baseViewModel.service) }
    private val viewModel by viewModels<WalletConnectMainViewModel> { WalletConnectMainModule.Factory(baseViewModel.service) }
    private var closeMenuItem: MenuItem? = null
    private var containerView: View? = null

    private val qrScannerResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                result.data?.getStringExtra(ModuleField.SCAN_ADDRESS)?.let {
                    viewModelScan.handleScanned(it)
                }
            }
            Activity.RESULT_CANCELED -> {
                val sessionsCount = arguments?.getInt(WalletConnectMainModule.SESSIONS_COUNT_KEY) ?: 0
                if (sessionsCount == 0) {
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
        containerView = view

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

        containerView?.isVisible = false

        val deepLinkUri = activity?.intent?.data?.toString()

        if (deepLinkUri != null) {
            activity?.intent?.data = null

            viewModelScan.handleScanned(deepLinkUri)
        } else {
            when (baseViewModel.initialScreen) {
                WalletConnectViewModel.InitialScreen.ScanQrCode -> {
                    val intent = QRScannerActivity.getIntentForFragment(this)
                    qrScannerResultLauncher.launch(intent)
                }
                WalletConnectViewModel.InitialScreen.Main -> {
                    containerView?.isVisible = true
                }
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
            containerView?.isVisible = true
        })

        val dappInfoAdapter = DappInfoAdapter()
        dappInfo.adapter = dappInfoAdapter

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

        viewModel.buttonStatesLiveData.observe(viewLifecycleOwner, { buttonsStates ->
            setButtons(buttonsStates)
        })

        viewModel.closeVisibleLiveData.observe(viewLifecycleOwner, {
            closeMenuItem?.isVisible = it
        })

        viewModel.signedTransactionsVisibleLiveData.observe(viewLifecycleOwner, {
            dappInfoAdapter.signedTransactionsVisible = it
        })

        viewModel.hintLiveData.observe(viewLifecycleOwner, { hint ->
            dappHint.text = hint?.let { getString(it) }
            dappHint.isVisible = hint != null
        })

        viewModel.errorLiveData.observe(viewLifecycleOwner, { error ->
            error?.let { HudHelper.showErrorMessage(requireView(), it) }
        })

        viewModel.statusLiveData.observe(viewLifecycleOwner, { status ->
            dappInfoAdapter.status = status
        })

        viewModel.closeLiveEvent.observe(viewLifecycleOwner, {
            HudHelper.showSuccessMessage(
                requireActivity().findViewById(android.R.id.content),
                R.string.Hud_Text_Done
            )
            findNavController().popBackStack()
        })

        viewModel.openRequestLiveEvent.observe(viewLifecycleOwner, {
            when (it) {
                is WalletConnectSendEthereumTransactionRequest -> {
                    baseViewModel.sharedSendEthereumTransactionRequest = it

                    findNavController().navigate(R.id.walletConnectMainFragment_to_walletConnectSendEthereumTransactionRequestFragment, null, navOptionsFromBottom())
                }
                is WalletConnectSignMessageRequest -> {
                    baseViewModel.sharedSignMessageRequest = it

                    findNavController().navigate(R.id.walletConnectMainFragment_to_walletConnectSignMessageRequestFragment, null, navOptionsFromBottom())
                }
            }
        })

        buttonsCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
        )

    }

    private fun setButtons(buttonsStates: WalletConnectMainViewModel.ButtonStates) {
        buttonsCompose.setContent {
            ComposeAppTheme {
                Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                    if (buttonsStates.connect.visible) {
                        ButtonPrimaryYellow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, top = 16.dp, end = 16.dp),
                            title = getString(R.string.Button_Connect),
                            onClick = {
                                viewModel.connect()
                            },
                            enabled = buttonsStates.connect.enabled
                        )
                    }
                    if (buttonsStates.reconnect.visible) {
                        ButtonPrimaryYellow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, top = 16.dp, end = 16.dp),
                            title = getString(R.string.Button_Reconnect),
                            onClick = {
                                viewModel.reconnect()
                            },
                            enabled = buttonsStates.reconnect.enabled
                        )
                    }
                    if (buttonsStates.disconnect.visible) {
                        ButtonPrimaryRed(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, top = 16.dp, end = 16.dp),
                            title = getString(R.string.Button_Disconnect),
                            onClick = {
                                viewModel.disconnect()
                            },
                            enabled = buttonsStates.disconnect.enabled
                        )
                    }
                    if (buttonsStates.cancel.visible) {
                        ButtonPrimaryDefault(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, top = 16.dp, end = 16.dp),
                            title = getString(R.string.Button_Cancel),
                            onClick = {
                                viewModel.cancel()
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        closeMenuItem = null
        containerView = null
    }

}

