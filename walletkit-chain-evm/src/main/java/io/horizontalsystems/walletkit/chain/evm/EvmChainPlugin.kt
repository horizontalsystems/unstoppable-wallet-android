package io.horizontalsystems.walletkit.chain.evm

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.IAdapter
import io.horizontalsystems.walletkit.core.ISendEthereumAdapter
import io.horizontalsystems.walletkit.core.ITransactionsAdapter
import io.horizontalsystems.walletkit.core.EvmError
import io.horizontalsystems.walletkit.core.adapters.Eip20Adapter
import io.horizontalsystems.walletkit.core.adapters.EvmAdapter
import io.horizontalsystems.walletkit.core.adapters.EvmTransactionsAdapter
import io.horizontalsystems.walletkit.core.address.AddressChecker
import io.horizontalsystems.walletkit.core.address.Eip20AddressValidator
import io.horizontalsystems.walletkit.core.chain.ChainKeyRow
import io.horizontalsystems.walletkit.core.chain.ChainPlugin
import io.horizontalsystems.walletkit.core.chain.ChainSendScreenArgs
import io.horizontalsystems.walletkit.core.managers.EvmKitManagerRegistry
import io.horizontalsystems.walletkit.core.managers.evmTransactionSource
import io.horizontalsystems.walletkit.core.managers.RestoreSettings
import io.horizontalsystems.walletkit.core.stats.StatPage
import io.horizontalsystems.walletkit.core.toRawHexString
import io.horizontalsystems.walletkit.entities.Account
import io.horizontalsystems.walletkit.entities.AccountType
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.entities.evmAddress
import io.horizontalsystems.walletkit.modules.addtoken.AddEvmTokenBlockchainService
import io.horizontalsystems.walletkit.modules.addtoken.AddTokenModule
import io.horizontalsystems.walletkit.modules.address.AddressHandlerEvm
import io.horizontalsystems.walletkit.modules.address.IAddressHandler
import io.horizontalsystems.walletkit.modules.manageaccount.evmaddress.AddressPage
import io.horizontalsystems.walletkit.modules.manageaccount.evmprivatekey.PrivateKeyPage
import io.horizontalsystems.walletkit.modules.multiswap.action.ISwapProviderAction
import io.horizontalsystems.walletkit.modules.multiswap.providers.EvmSwapHelper
import io.horizontalsystems.walletkit.modules.multiswap.providers.IMultiSwapProvider
import io.horizontalsystems.walletkit.modules.multiswap.providers.OneInchProvider
import io.horizontalsystems.walletkit.modules.multiswap.providers.PancakeSwapV3Provider
import io.horizontalsystems.walletkit.modules.multiswap.providers.UniswapV3Provider
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.AbstractSendTransactionService
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.SendTransactionData
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.SendTransactionServiceEvm
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.toEvmTransactionData
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.modules.opencryptopay.OcpConfirmData
import io.horizontalsystems.walletkit.modules.opencryptopay.OpenCryptoPayEvmConfirmationPage
import io.horizontalsystems.walletkit.modules.send.address.EnterAddressValidator
import io.horizontalsystems.walletkit.modules.send.address.EvmAddressValidator
import io.horizontalsystems.walletkit.modules.send.evm.SendEvmModule
import io.horizontalsystems.walletkit.modules.send.evm.SendEvmScreen
import io.horizontalsystems.walletkit.modules.send.evm.SendEvmViewModel
import io.horizontalsystems.walletkit.modules.transactionInfo.options.SpeedUpCancelType
import io.horizontalsystems.walletkit.modules.transactionInfo.options.TransactionSpeedUpCancelPage
import io.horizontalsystems.walletkit.modules.transactions.TransactionSource
import io.horizontalsystems.walletkit.modules.walletconnect.IWCWalletRequestHandler
import io.horizontalsystems.walletkit.modules.walletconnect.WCWalletRequestHandler
import io.horizontalsystems.walletkit.modules.walletconnect.handler.IWCHandler
import io.horizontalsystems.walletkit.modules.walletconnect.handler.WCHandlerEvm
import io.horizontalsystems.walletkit.modules.walletconnect.request.WcRequestEvm
import io.horizontalsystems.ethereumkit.api.jsonrpc.JsonRpc
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.Address as EvmAddress
import io.horizontalsystems.ethereumkit.core.signer.Signer
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.rx2.asFlow
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.reflect.KClass

/**
 * Plugin for one EVM blockchain. Registered once per chain in
 * [io.horizontalsystems.walletkit.core.managers.EvmBlockchainManager.blockchainTypes];
 * hooks that exist once for the whole EVM family (WalletConnect handlers, DEX swap
 * providers, key rows, error conversion) are contributed by the Ethereum instance only.
 */
class EvmChainPlugin(override val blockchainType: BlockchainType) : ChainPlugin {

    private val isFamilyAnchor get() = blockchainType == BlockchainType.Ethereum

    private val evmKitManager get() = EvmKitManagerRegistry.getEvmKitManager(blockchainType)

    override fun createAdapter(wallet: Wallet, restoreSettings: RestoreSettings): IAdapter? =
        when (val tokenType = wallet.token.type) {
            TokenType.Native -> {
                val evmKitWrapper = evmKitManager.getEvmKitWrapper(wallet.account, blockchainType)
                EvmAdapter(evmKitWrapper, App.coinManager)
            }

            is TokenType.Eip20 -> {
                val evmKitWrapper = evmKitManager.getEvmKitWrapper(wallet.account, blockchainType)
                App.evmBlockchainManager.getBaseToken(blockchainType)?.let { baseToken ->
                    Eip20Adapter(
                        App.instance,
                        evmKitWrapper,
                        tokenType.address,
                        baseToken,
                        App.coinManager,
                        wallet,
                        App.evmLabelManager,
                    )
                }
            }

            else -> null
        }

    override fun createTransactionsAdapter(source: TransactionSource): ITransactionsAdapter? {
        val evmKitWrapper = evmKitManager.getEvmKitWrapper(source.account, blockchainType)
        val baseCoin = App.evmBlockchainManager.getBaseToken(blockchainType) ?: return null

        return EvmTransactionsAdapter(
            evmKitWrapper,
            baseCoin,
            App.coinManager,
            source,
            evmTransactionSource(blockchainType, App.appConfigProvider),
            App.evmLabelManager,
        )
    }

    override fun unlink(account: Account) {
        evmKitManager.unlink(account)
    }

    override val walletReloadTrigger: Flow<*>
        get() = evmKitManager.evmKitUpdatedObservable.asFlow()

    override suspend fun refreshKit() {
        evmKitManager.evmKitWrapper?.evmKit?.refresh()
    }

    override fun statusInfo(): Map<String, Any>? = evmKitManager.statusInfo

    override suspend fun onAppStart() {
        if (isFamilyAnchor) {
            EthereumKit.init()
        }
    }

    override suspend fun swapDestinationAddress(account: Account): String? =
        account.type.evmAddress(EvmKitManagerRegistry.getChain(blockchainType))?.eip55

    override fun analyticsAddress(accountType: AccountType): String? =
        if (isFamilyAnchor) {
            accountType.evmAddress(EvmKitManagerRegistry.getChain(blockchainType))?.hex
        } else {
            null
        }

    override suspend fun eip20Allowance(token: Token, spenderAddress: String): BigDecimal? =
        EvmSwapHelper.getAllowance(token, spenderAddress)

    override suspend fun eip20ApproveAction(
        allowance: BigDecimal?,
        amountIn: BigDecimal,
        spenderAddress: String,
        token: Token,
    ): ISwapProviderAction? = EvmSwapHelper.actionApprove(allowance, amountIn, spenderAddress, token)

    override fun sendTransactionService(token: Token): AbstractSendTransactionService =
        SendTransactionServiceEvm(blockchainType)

    override fun depositTransferData(
        token: Token,
        amount: BigDecimal,
        address: String,
    ): SendTransactionData? {
        val adapter = App.adapterManager.getAdapterForToken<ISendEthereumAdapter>(token) ?: return null
        val transactionData = adapter.getTransactionData(amount, EvmAddress(address))

        return SendTransactionData.Evm(transactionData.toEvmTransactionData(), gasLimit = null)
    }

    override fun swapProviders(): List<IMultiSwapProvider> =
        if (isFamilyAnchor) {
            listOf(OneInchProvider(), UniswapV3Provider, PancakeSwapV3Provider)
        } else {
            emptyList()
        }

    override fun wcHandlers(): List<IWCHandler> =
        if (isFamilyAnchor) listOf(WCHandlerEvm()) else emptyList()

    override fun wcWalletRequestHandler(): IWCWalletRequestHandler? =
        if (isFamilyAnchor) WCWalletRequestHandler(App.evmBlockchainManager) else null

    @Composable
    override fun WcRequestSheetScreen(navigation: HSNavigation): Boolean {
        WcRequestEvm(navigation)
        return true
    }

    override fun addressHandlers(): List<IAddressHandler> = listOf(AddressHandlerEvm(blockchainType))

    override fun addressValidator(token: Token): EnterAddressValidator = EvmAddressValidator()

    override fun blacklistAddressChecker(): AddressChecker? =
        if (isFamilyAnchor) Eip20BlacklistAddressChecker(Eip20AddressValidator(App.evmSyncSourceManager)) else null

    override val supportsCustomTokens: Boolean get() = true

    override fun addTokenBlockchainService(blockchain: Blockchain): AddTokenModule.IAddTokenBlockchainService =
        AddEvmTokenBlockchainService.getInstance(blockchain)

    override fun openCryptoPayConfirmationPage(
        data: OcpConfirmData,
        sendEntryPointDestId: KClass<out HSPage>,
    ): HSPage = OpenCryptoPayEvmConfirmationPage(
        OpenCryptoPayEvmConfirmationPage.Input(
            wallet = data.wallet,
            callbackUrl = data.callbackUrl,
            quoteId = data.quoteId,
            paymentId = data.paymentId,
            method = data.method,
            asset = data.asset,
            assetAmount = data.assetAmount,
            blockchainType = data.blockchainType,
            merchant = data.merchant,
            expirationIso = data.expirationIso,
            minFee = data.minFee,
            sendEntryPointDestId = sendEntryPointDestId,
        )
    )

    override fun resendTransactionPage(type: SpeedUpCancelType, transactionHash: String): HSPage =
        TransactionSpeedUpCancelPage(
            TransactionSpeedUpCancelPage.Input(blockchainType, type, transactionHash)
        )

    override fun convertError(throwable: Throwable): Throwable? {
        if (!isFamilyAnchor) return null
        if (throwable !is JsonRpc.ResponseError.RpcError) return null

        val message = throwable.error.message
        return if (message.contains("insufficient funds for transfer") ||
            message.contains("gas required exceeds allowance")
        ) {
            EvmError.InsufficientBalanceWithFee
        } else if (message.contains("max fee per gas less than block base fee") ||
            message.contains("fee cap less than block base fee")
        ) {
            EvmError.LowerThanBaseGasLimit
        } else if (message.contains("execution reverted")) {
            EvmError.ExecutionReverted(message)
        } else {
            EvmError.RpcError(message)
        }
    }

    override fun privateKeyRows(account: Account): List<ChainKeyRow> {
        if (!isFamilyAnchor) return emptyList()

        val privateKey = when (val accountType = account.type) {
            is AccountType.Mnemonic -> {
                val chain = EvmKitManagerRegistry.getChain(BlockchainType.Ethereum)
                toHexString(Signer.privateKey(accountType.words, accountType.passphrase, chain))
            }

            is AccountType.EvmPrivateKey -> toHexString(accountType.key)
            else -> null
        } ?: return emptyList()

        return listOf(
            ChainKeyRow(
                titleRes = R.string.PrivateKeys_EvmPrivateKey,
                descriptionRes = R.string.PrivateKeys_EvmPrivateKeyDescription,
                page = PrivateKeyPage(PrivateKeyPage.Input(privateKey, PrivateKeyPage.Type.Evm)),
                statPage = StatPage.EvmPrivateKey,
            )
        )
    }

    override fun publicKeyRows(account: Account): List<ChainKeyRow> {
        if (!isFamilyAnchor) return emptyList()

        val evmAddress = when (val accountType = account.type) {
            is AccountType.Mnemonic -> {
                val chain = EvmKitManagerRegistry.getChain(BlockchainType.Ethereum)
                Signer.address(accountType.words, accountType.passphrase, chain).eip55
            }

            is AccountType.EvmPrivateKey -> Signer.address(accountType.key).eip55
            else -> null
        } ?: return emptyList()

        return listOf(
            ChainKeyRow(
                titleRes = R.string.PublicKeys_EvmAddress,
                descriptionRes = R.string.PublicKeys_EvmAddress_Description,
                page = AddressPage(AddressPage.Input(evmAddress, AddressPage.Type.Evm)),
                statPage = StatPage.EvmAddress,
            )
        )
    }

    @Composable
    override fun SendScreen(args: ChainSendScreenArgs) {
        val adapter = App.adapterManager.getAdapterForWallet<ISendEthereumAdapter>(args.wallet)
            ?: throw IllegalArgumentException("SendEthereumAdapter is null")

        val sendEvmViewModel = viewModel<SendEvmViewModel>(
            factory = SendEvmModule.Factory(args.wallet, args.address, args.hideAddress, adapter)
        )

        SendEvmScreen(
            title = args.title,
            navigation = args.navigation,
            amountInputModeViewModel = args.amountInputModeViewModel,
            viewModel = sendEvmViewModel,
            address = args.address,
            wallet = args.wallet,
            amount = args.amount,
            hideAddress = args.hideAddress,
            riskyAddress = args.riskyAddress,
            sendEntryPointDestId = args.sendEntryPointDestId,
        )
    }

    override fun clearAccountData(accountId: String) {
        if (isFamilyAnchor) {
            EvmAdapter.clear(accountId)
            Eip20Adapter.clear(accountId)
        }
    }

    private fun toHexString(key: BigInteger): String {
        return key.toByteArray().let {
            if (it.size > 32) {
                it.copyOfRange(1, it.size)
            } else {
                it
            }.toRawHexString()
        }
    }
}

/** Consults the USDT/USDC/PYUSD blacklist and freeze contract methods for EVM tokens. */
private class Eip20BlacklistAddressChecker(
    private val eip20AddressValidator: Eip20AddressValidator,
) : AddressChecker {

    override suspend fun isClear(
        address: io.horizontalsystems.walletkit.entities.Address,
        token: Token,
    ): Boolean = eip20AddressValidator.isClear(address, token)

    override fun supports(token: Token): Boolean = eip20AddressValidator.supports(token)
}
