package io.horizontalsystems.walletkit.chain.bitcoin

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.IAdapter
import io.horizontalsystems.walletkit.core.ISendBitcoinAdapter
import io.horizontalsystems.walletkit.core.adapters.BitcoinAdapter
import io.horizontalsystems.walletkit.core.adapters.BitcoinCashAdapter
import io.horizontalsystems.walletkit.core.adapters.DashAdapter
import io.horizontalsystems.walletkit.core.adapters.ECashAdapter
import io.horizontalsystems.walletkit.core.adapters.LitecoinAdapter
import io.horizontalsystems.walletkit.core.chain.ChainKeyRow
import io.horizontalsystems.walletkit.core.chain.ChainPlugin
import io.horizontalsystems.walletkit.core.chain.ChainSendScreenArgs
import io.horizontalsystems.walletkit.core.factories.FeeRateProviderFactory
import io.horizontalsystems.walletkit.core.managers.RestoreSettings
import io.horizontalsystems.walletkit.core.managers.syncMode
import io.horizontalsystems.walletkit.core.providers.Translator
import io.horizontalsystems.walletkit.core.stats.StatEvent
import io.horizontalsystems.walletkit.core.stats.StatPage
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.entities.Account
import io.horizontalsystems.walletkit.entities.AccountType
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.modules.address.AddressHandlerBase58
import io.horizontalsystems.walletkit.modules.address.AddressHandlerBech32
import io.horizontalsystems.walletkit.modules.address.AddressHandlerBitcoinCash
import io.horizontalsystems.walletkit.modules.address.IAddressHandler
import io.horizontalsystems.walletkit.modules.blockchainsettings.BlockchainSettingsModule
import io.horizontalsystems.walletkit.modules.btcblockchainsettings.BtcBlockchainSettingsPage
import io.horizontalsystems.walletkit.modules.manageaccount.showextendedkey.ShowExtendedKeyModule
import io.horizontalsystems.walletkit.modules.manageaccount.showextendedkey.ShowExtendedKeyPage
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.AbstractSendTransactionService
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.SendTransactionServiceBtc
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.modules.send.address.BitcoinAddressValidator
import io.horizontalsystems.walletkit.modules.send.address.EnterAddressValidator
import io.horizontalsystems.walletkit.modules.send.bitcoin.SendBitcoinModule
import io.horizontalsystems.walletkit.modules.send.bitcoin.SendBitcoinScreen
import io.horizontalsystems.walletkit.modules.send.bitcoin.SendBitcoinViewModel
import io.horizontalsystems.walletkit.modules.transactionInfo.options.SpeedUpCancelType
import io.horizontalsystems.walletkit.modules.transactionInfo.resendbitcoin.ResendBitcoinPage
import io.horizontalsystems.bitcoincash.MainNetBitcoinCash
import io.horizontalsystems.bitcoinkit.MainNet
import io.horizontalsystems.dashkit.MainNetDash
import io.horizontalsystems.ecash.MainNetECash
import io.horizontalsystems.hdwalletkit.HDExtendedKey
import io.horizontalsystems.hdwalletkit.HDWallet
import io.horizontalsystems.hdwalletkit.Mnemonic
import io.horizontalsystems.litecoinkit.MainNetLitecoin
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.rx2.asFlow
import java.math.BigDecimal

abstract class BitcoinFamilyPlugin : ChainPlugin {

    protected val btcBlockchainManager get() = App.btcBlockchainManager

    override fun createAdapter(wallet: Wallet, restoreSettings: RestoreSettings): IAdapter? {
        val syncMode = btcBlockchainManager.syncMode(blockchainType, wallet.account.origin)
        return when (val tokenType = wallet.token.type) {
            is TokenType.Derived -> when (blockchainType) {
                BlockchainType.Bitcoin -> BitcoinAdapter(wallet, syncMode, App.backgroundManager, tokenType.derivation)
                BlockchainType.Litecoin -> LitecoinAdapter(wallet, syncMode, App.backgroundManager, tokenType.derivation)
                else -> null
            }
            is TokenType.AddressTyped ->
                if (blockchainType == BlockchainType.BitcoinCash) {
                    BitcoinCashAdapter(wallet, syncMode, App.backgroundManager, tokenType.type)
                } else {
                    null
                }
            TokenType.Native -> when (blockchainType) {
                BlockchainType.ECash -> ECashAdapter(wallet, syncMode, App.backgroundManager)
                BlockchainType.Dash -> DashAdapter(wallet, syncMode, App.backgroundManager)
                else -> null
            }
            else -> null
        }
    }

    // Same signal the settings service listened to before the split: restore-mode and
    // sort-mode changes for this chain refresh the settings row; restore-mode changes
    // also reload wallets via WalletManager (which still owns that collector in core).
    override val settingsRefreshTrigger: Flow<*>
        get() = merge(
            btcBlockchainManager.restoreModeUpdatedObservable.asFlow(),
            btcBlockchainManager.transactionSortModeUpdatedObservable.asFlow(),
        ).filter { it == blockchainType }

    override fun blockchainSettingsItem(): BlockchainSettingsModule.BlockchainItem.Chain? {
        val blockchain = btcBlockchainManager.blockchain(blockchainType) ?: return null
        return BlockchainSettingsModule.BlockchainItem.Chain(
            blockchain = blockchain,
            subtitle = Translator.getString(btcBlockchainManager.restoreMode(blockchainType).title),
            btcLike = true,
            page = BtcBlockchainSettingsPage(blockchain),
            statEvent = StatEvent.OpenBlockchainSettingsBtc(blockchain.uid),
        )
    }

    override suspend fun swapSourceAddresses(token: Token, amountIn: BigDecimal): List<String>? {
        val adapter = App.adapterManager.getAdapterForToken<ISendBitcoinAdapter>(token) ?: return emptyList()
        val feeRate = try {
            FeeRateProviderFactory.provider(token.blockchainType)?.getFeeRates()?.recommended
        } catch (_: Throwable) {
            null
        }

        return adapter.selectUnspentOutputs(amountIn, feeRate ?: 1).mapNotNull { it.address }.distinct()
    }

    override fun clearAccountData(accountId: String) {
        when (blockchainType) {
            BlockchainType.Bitcoin -> BitcoinAdapter.clear(accountId)
            BlockchainType.BitcoinCash -> BitcoinCashAdapter.clear(accountId)
            BlockchainType.ECash -> ECashAdapter.clear(accountId)
            BlockchainType.Dash -> DashAdapter.clear(accountId)
            BlockchainType.Litecoin -> LitecoinAdapter.clear(accountId)
            else -> Unit
        }
    }

    override fun sendTransactionService(token: Token): AbstractSendTransactionService =
        SendTransactionServiceBtc(token)

    override fun addressValidator(token: Token): EnterAddressValidator =
        BitcoinAddressValidator(token, App.adapterManager)

    override fun resendTransactionPage(type: SpeedUpCancelType): HSPage =
        ResendBitcoinPage(ResendBitcoinPage.Input(type))

    @Composable
    override fun SendScreen(args: ChainSendScreenArgs) {
        val factory = SendBitcoinModule.Factory(args.wallet, args.address, args.hideAddress)
        val sendBitcoinViewModel = viewModel<SendBitcoinViewModel>(factory = factory)
        SendBitcoinScreen(
            args.title,
            args.navigation,
            sendBitcoinViewModel,
            args.amountInputModeViewModel,
            args.sendEntryPointDestId,
            args.amount,
            riskyAddress = args.riskyAddress,
        )
    }
}

class BitcoinChainPlugin : BitcoinFamilyPlugin() {
    override val blockchainType: BlockchainType = BlockchainType.Bitcoin

    override fun addressHandlers(): List<IAddressHandler> {
        val network = MainNet()
        return listOf(
            AddressHandlerBase58(network, blockchainType),
            AddressHandlerBech32(network, blockchainType),
        )
    }

    override suspend fun swapDestinationAddress(account: Account, token: Token): String =
        BitcoinAdapter.firstAddress(account.type, token.type)

    // The BIP32 root and account-extended keys are HD concepts owned by the bitcoin
    // family; only this plugin contributes them so the rows appear once per account.
    override fun privateKeyRows(account: Account): List<ChainKeyRow> {
        val hdExtendedKey = (account.type as? AccountType.HdExtendedKey)?.hdExtendedKey

        val accountType = account.type
        val bip32RootKey = if (accountType is AccountType.Mnemonic) {
            val seed = Mnemonic().toSeed(accountType.words, accountType.passphrase)
            HDExtendedKey(seed, HDWallet.Purpose.BIP44)
        } else if (hdExtendedKey?.derivedType == HDExtendedKey.DerivedType.Master) {
            hdExtendedKey
        } else {
            null
        }

        var accountExtendedDisplayType = ShowExtendedKeyModule.DisplayKeyType.AccountPrivateKey(true)
        val accountExtendedPrivateKey = bip32RootKey
            ?: if (hdExtendedKey?.derivedType == HDExtendedKey.DerivedType.Account && !hdExtendedKey.isPublic) {
                accountExtendedDisplayType = ShowExtendedKeyModule.DisplayKeyType.AccountPrivateKey(false)
                hdExtendedKey
            } else {
                null
            }

        return listOfNotNull(
            bip32RootKey?.let {
                ChainKeyRow(
                    titleRes = R.string.PrivateKeys_Bip32RootKey,
                    descriptionRes = R.string.PrivateKeys_Bip32RootKeyDescription,
                    page = ShowExtendedKeyPage(ShowExtendedKeyPage.Input(it, ShowExtendedKeyModule.DisplayKeyType.Bip32RootKey)),
                    statPage = StatPage.Bip32RootKey,
                )
            },
            accountExtendedPrivateKey?.let {
                ChainKeyRow(
                    titleRes = R.string.PrivateKeys_AccountExtendedPrivateKey,
                    descriptionRes = R.string.PrivateKeys_AccountExtendedPrivateKeyDescription,
                    page = ShowExtendedKeyPage(ShowExtendedKeyPage.Input(it, accountExtendedDisplayType)),
                    statPage = StatPage.AccountExtendedPrivateKey,
                )
            },
        )
    }

    override fun publicKeyRows(account: Account): List<ChainKeyRow> {
        val hdExtendedKey = (account.type as? AccountType.HdExtendedKey)?.hdExtendedKey
        var accountPublicKey = ShowExtendedKeyModule.DisplayKeyType.AccountPublicKey(false)

        val accountType = account.type
        val publicKey = if (accountType is AccountType.Mnemonic) {
            accountPublicKey = ShowExtendedKeyModule.DisplayKeyType.AccountPublicKey(true)
            val seed = Mnemonic().toSeed(accountType.words, accountType.passphrase)
            HDExtendedKey(seed, HDWallet.Purpose.BIP44)
        } else if (hdExtendedKey?.derivedType == HDExtendedKey.DerivedType.Master) {
            accountPublicKey = ShowExtendedKeyModule.DisplayKeyType.AccountPublicKey(true)
            hdExtendedKey
        } else if (hdExtendedKey?.derivedType == HDExtendedKey.DerivedType.Account) {
            hdExtendedKey
        } else {
            null
        } ?: return emptyList()

        return listOf(
            ChainKeyRow(
                titleRes = R.string.PublicKeys_AccountExtendedPublicKey,
                descriptionRes = R.string.PublicKeys_AccountExtendedPublicKeyDescription,
                page = ShowExtendedKeyPage(ShowExtendedKeyPage.Input(publicKey, accountPublicKey)),
                statPage = StatPage.AccountExtendedPublicKey,
            )
        )
    }
}

class BitcoinCashChainPlugin : BitcoinFamilyPlugin() {
    override val blockchainType: BlockchainType = BlockchainType.BitcoinCash

    override fun addressHandlers(): List<IAddressHandler> {
        val network = MainNetBitcoinCash()
        return listOf(
            AddressHandlerBase58(network, blockchainType),
            AddressHandlerBitcoinCash(network, blockchainType),
        )
    }

    override suspend fun swapDestinationAddress(account: Account, token: Token): String =
        BitcoinCashAdapter.firstAddress(account.type, token.type)
}

class ECashChainPlugin : BitcoinFamilyPlugin() {
    override val blockchainType: BlockchainType = BlockchainType.ECash

    override fun addressHandlers(): List<IAddressHandler> {
        val network = MainNetECash()
        return listOf(
            AddressHandlerBase58(network, blockchainType),
            AddressHandlerBitcoinCash(network, blockchainType),
        )
    }

    override suspend fun swapDestinationAddress(account: Account, token: Token): String =
        ECashAdapter.firstAddress(account.type)
}

class LitecoinChainPlugin : BitcoinFamilyPlugin() {
    override val blockchainType: BlockchainType = BlockchainType.Litecoin

    override fun addressHandlers(): List<IAddressHandler> {
        val network = MainNetLitecoin()
        return listOf(
            AddressHandlerBase58(network, blockchainType),
            AddressHandlerBech32(network, blockchainType),
        )
    }

    override suspend fun swapDestinationAddress(account: Account, token: Token): String =
        LitecoinAdapter.firstAddress(account.type, token.type)
}

class DashChainPlugin : BitcoinFamilyPlugin() {
    override val blockchainType: BlockchainType = BlockchainType.Dash

    override fun addressHandlers(): List<IAddressHandler> {
        val network = MainNetDash()
        return listOf(AddressHandlerBase58(network, blockchainType))
    }

    override suspend fun swapDestinationAddress(account: Account, token: Token): String =
        DashAdapter.firstAddress(account.type)
}
