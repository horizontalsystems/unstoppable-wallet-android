package io.horizontalsystems.walletkit.chain.tron

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.tronkit.network.Network
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.IAdapter
import io.horizontalsystems.walletkit.core.ITransactionsAdapter
import io.horizontalsystems.walletkit.core.address.AddressChecker
import io.horizontalsystems.walletkit.core.address.Trc20AddressValidator
import io.horizontalsystems.walletkit.core.adapters.Trc20Adapter
import io.horizontalsystems.walletkit.core.adapters.TronAdapter
import io.horizontalsystems.walletkit.core.adapters.TronTransactionConverter
import io.horizontalsystems.walletkit.core.adapters.TronTransactionsAdapter
import io.horizontalsystems.walletkit.core.chain.ChainKeyRow
import io.horizontalsystems.walletkit.core.chain.ChainPlugin
import io.horizontalsystems.walletkit.core.chain.ChainSendScreenArgs
import io.horizontalsystems.walletkit.core.managers.RestoreSettings
import io.horizontalsystems.walletkit.core.managers.TronAccountManager
import io.horizontalsystems.walletkit.core.managers.TronKitManager
import io.horizontalsystems.walletkit.core.stats.StatPage
import io.horizontalsystems.walletkit.core.toRawHexString
import io.horizontalsystems.walletkit.entities.Account
import io.horizontalsystems.walletkit.entities.AccountType
import io.horizontalsystems.walletkit.entities.Address
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.entities.transactionrecords.tron.TronApproveTransactionRecord
import io.horizontalsystems.walletkit.modules.addtoken.AddTokenModule
import io.horizontalsystems.walletkit.modules.addtoken.AddTronTokenBlockchainService
import io.horizontalsystems.walletkit.modules.address.AddressHandlerTron
import io.horizontalsystems.walletkit.modules.address.IAddressHandler
import io.horizontalsystems.walletkit.modules.balance.BalanceModule
import io.horizontalsystems.walletkit.modules.blockchainsettings.BlockchainSettingsModule
import io.horizontalsystems.walletkit.modules.manageaccount.evmaddress.AddressPage
import io.horizontalsystems.walletkit.modules.manageaccount.evmprivatekey.PrivateKeyPage
import io.horizontalsystems.walletkit.modules.multiswap.action.ActionApprove
import io.horizontalsystems.walletkit.modules.multiswap.action.ActionRevoke
import io.horizontalsystems.walletkit.modules.multiswap.action.ISwapProviderAction
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.AbstractSendTransactionService
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.SendTransactionServiceTron
import io.horizontalsystems.walletkit.modules.send.address.EnterAddressValidator
import io.horizontalsystems.walletkit.modules.send.address.TronAddressValidator
import io.horizontalsystems.walletkit.modules.send.tron.SendTronModule
import io.horizontalsystems.walletkit.modules.send.tron.SendTronScreen
import io.horizontalsystems.walletkit.modules.send.tron.SendTronViewModel
import io.horizontalsystems.walletkit.modules.transactions.TransactionSource
import java.math.BigDecimal
import java.math.BigInteger
import kotlinx.coroutines.flow.Flow
import io.horizontalsystems.tronkit.transaction.Signer as TronSigner

class TronChainPlugin : ChainPlugin {

    override val blockchainType = BlockchainType.Tron

    val kitManager by lazy { TronKitManager(App.evmSyncSourceManager, App.backgroundManager) }

    private val accountManager by lazy {
        TronAccountManager(
            App.accountManager,
            App.walletManager,
            App.marketKit,
            kitManager,
            App.tokenAutoEnableManager
        )
    }

    override suspend fun onAppStart() {
        accountManager.start()
    }

    override fun createAdapter(wallet: Wallet, restoreSettings: RestoreSettings): IAdapter? =
        when (val tokenType = wallet.token.type) {
            TokenType.Native -> TronAdapter(kitManager.getTronKitWrapper(wallet.account))
            is TokenType.Eip20 -> {
                baseToken()?.let { baseToken ->
                    Trc20Adapter(
                        kitManager.getTronKitWrapper(wallet.account),
                        tokenType.address,
                        wallet,
                        App.coinManager,
                        baseToken,
                        App.evmLabelManager
                    )
                }
            }
            else -> null
        }

    override fun createTransactionsAdapter(source: TransactionSource): ITransactionsAdapter? {
        val baseToken = baseToken() ?: return null
        val tronKitWrapper = kitManager.getTronKitWrapper(source.account)
        val converter = TronTransactionConverter(App.coinManager, tronKitWrapper, source, baseToken, App.evmLabelManager)
        return TronTransactionsAdapter(tronKitWrapper, converter)
    }

    private fun baseToken(): Token? =
        App.coinManager.getToken(TokenQuery(BlockchainType.Tron, TokenType.Native))

    override fun unlink(account: Account) {
        kitManager.unlink(account)
    }

    override fun clearAccountData(accountId: String) {
        TronAdapter.clear(accountId)
    }

    override fun statusInfo(): Map<String, Any>? = kitManager.statusInfo

    override suspend fun refreshKit() {
        kitManager.tronKitWrapper?.tronKit?.refresh()
    }

    override val walletReloadTrigger: Flow<*>
        get() = kitManager.kitStoppedFlow

    override suspend fun swapDestinationAddress(account: Account): String? =
        kitManager.getAddress(account)

    override suspend fun balanceWarning(wallet: Wallet): BalanceModule.BalanceWarning? {
        val adapter = App.adapterManager.getAdapterForWallet<io.horizontalsystems.walletkit.core.adapters.BaseTronAdapter>(wallet)
            ?: return null
        return if (!adapter.tronKit.accountActive) {
            BalanceModule.BalanceWarning.TronInactiveAccountWarning
        } else {
            null
        }
    }

    override fun addressHandlers(): List<IAddressHandler> = listOf(AddressHandlerTron())

    override fun addressValidator(
        token: Token,
        allowOwnAddress: Boolean,
        transparentOnly: Boolean,
    ): EnterAddressValidator = TronAddressValidator(token, App.adapterManager, allowOwnAddress)

    override fun blacklistAddressChecker(): AddressChecker = Trc20BlacklistAddressChecker(Trc20AddressValidator())

    override val supportsCustomTokens: Boolean get() = true

    override fun addTokenBlockchainService(blockchain: Blockchain): AddTokenModule.IAddTokenBlockchainService =
        AddTronTokenBlockchainService.getInstance(blockchain)

    override fun blockchainSettingsItem(): BlockchainSettingsModule.BlockchainItem? =
        App.marketKit.blockchain(BlockchainType.Tron.uid)?.let { blockchain ->
            BlockchainSettingsModule.BlockchainItem.Evm(
                blockchain,
                App.evmSyncSourceManager.getSyncSource(BlockchainType.Tron)
            )
        }

    override fun sendTransactionService(token: Token): AbstractSendTransactionService =
        SendTransactionServiceTron(token)

    @Composable
    override fun SendScreen(args: ChainSendScreenArgs) {
        val factory = SendTronModule.Factory(args.wallet, args.address, args.hideAddress)
        val sendTronViewModel = viewModel<SendTronViewModel>(factory = factory)
        SendTronScreen(
            title = args.title,
            navigation = args.navigation,
            viewModel = sendTronViewModel,
            amountInputModeViewModel = args.amountInputModeViewModel,
            sendEntryPointDestId = args.sendEntryPointDestId,
            amount = args.amount,
            riskyAddress = args.riskyAddress
        )
    }

    override suspend fun eip20Allowance(token: Token, spenderAddress: String): BigDecimal? {
        if (token.type !is TokenType.Eip20) return null

        val trc20Adapter = App.adapterManager.getAdapterForToken<Trc20Adapter>(token) ?: return null
        return trc20Adapter.allowance(spenderAddress)
    }

    override suspend fun eip20ApproveAction(
        allowance: BigDecimal?,
        amountIn: BigDecimal,
        spenderAddress: String,
        token: Token,
    ): ISwapProviderAction? {
        if (allowance == null || allowance >= amountIn) return null
        val trc20Adapter = App.adapterManager.getAdapterForToken<Trc20Adapter>(token) ?: return null

        val approveTransaction = trc20Adapter.getPendingTransactions()
            .filterIsInstance<TronApproveTransactionRecord>()
            .filter { it.spender.equals(spenderAddress, true) }
            .maxByOrNull { it.timestamp }

        val revoke = allowance > BigDecimal.ZERO && isUsdt(token)

        return if (revoke) {
            val revokeInProgress = approveTransaction != null && approveTransaction.value.zeroValue
            ActionRevoke(
                token,
                spenderAddress,
                revokeInProgress,
                allowance
            )
        } else {
            val approveInProgress = approveTransaction != null && !approveTransaction.value.zeroValue
            ActionApprove(
                amountIn,
                spenderAddress,
                token,
                approveInProgress
            )
        }
    }

    private fun isUsdt(token: Token): Boolean {
        val tokenType = token.type

        return token.blockchainType is BlockchainType.Tron
                && tokenType is TokenType.Eip20
                && tokenType.address.lowercase() == "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t".lowercase()
    }

    override fun privateKeyRows(account: Account): List<ChainKeyRow> {
        val privateKey = when (val accountType = account.type) {
            is AccountType.Mnemonic -> privateKeyHex(TronSigner.privateKey(accountType.seed, Network.Mainnet))
            is AccountType.TronPrivateKey -> privateKeyHex(accountType.key)
            else -> null
        } ?: return emptyList()

        return listOf(
            ChainKeyRow(
                titleRes = R.string.PrivateKeys_TronPrivateKey,
                descriptionRes = R.string.PrivateKeys_TronPrivateKeyDescription,
                page = PrivateKeyPage(PrivateKeyPage.Input(privateKey, PrivateKeyPage.Type.Tron)),
                statPage = StatPage.TronPrivateKey,
            )
        )
    }

    override fun publicKeyRows(account: Account): List<ChainKeyRow> {
        val address = when (val accountType = account.type) {
            is AccountType.Mnemonic -> {
                val privateKey = TronSigner.privateKey(accountType.seed, Network.Mainnet)
                TronSigner.address(privateKey, Network.Mainnet).base58
            }

            is AccountType.TronPrivateKey -> TronSigner.address(accountType.key, Network.Mainnet).base58

            else -> null
        } ?: return emptyList()

        return listOf(
            ChainKeyRow(
                titleRes = R.string.PublicKeys_TronAddress,
                descriptionRes = R.string.PublicKeys_TronAddress_Description,
                page = AddressPage(AddressPage.Input(address, AddressPage.Type.Tron)),
                statPage = StatPage.TronAddress,
            )
        )
    }

    // Renders a private key as its canonical 64-char hex form: BigInteger.toByteArray()
    // drops leading zero bytes and may prepend a sign byte, so the value is normalized
    // to exactly 32 bytes first.
    private fun privateKeyHex(key: BigInteger): String {
        val bytes = key.toByteArray()
        val normalized = when {
            bytes.size > 32 -> bytes.copyOfRange(bytes.size - 32, bytes.size)
            bytes.size < 32 -> ByteArray(32 - bytes.size) + bytes
            else -> bytes
        }
        return normalized.toRawHexString()
    }
}

private class Trc20BlacklistAddressChecker(
    private val validator: Trc20AddressValidator
) : AddressChecker {
    override suspend fun isClear(address: Address, token: Token): Boolean = validator.isClear(address, token)
    override fun supports(token: Token): Boolean = validator.supports(token)
}
