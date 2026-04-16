package io.horizontalsystems.bankwallet.modules.restoreaccount

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.nativeTokenQueries
import io.horizontalsystems.bankwallet.core.order
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.core.stats.statAccountType
import io.horizontalsystems.bankwallet.core.supported
import io.horizontalsystems.bankwallet.core.supports
import io.horizontalsystems.bankwallet.entities.AccountOrigin
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.enablecoin.restoresettings.BirthdayHeightConfig
import io.horizontalsystems.marketkit.models.BlockchainType

class RestoreViewModel(): ViewModelUiState<RestoreViewModel.UiState>() {
    private val marketKit = App.marketKit
    private val accountFactory = App.accountFactory
    private val accountManager = App.accountManager
    private val tokenAutoEnableManager = App.tokenAutoEnableManager
    private val walletManager = App.walletManager

    var accountTypes: List<AccountType> = listOf()

    var accountType: AccountType? = null
        private set

    var accountName: String = ""
        private set

    var manualBackup: Boolean = false
        private set

    var fileBackup: Boolean = false
        private set

    var birthdayHeightConfig: BirthdayHeightConfig? = null
        private set

    var statPage: StatPage? = null
        private set

    var cancelBirthdayHeightConfig: Boolean = false

    private var openSelectCoinsScreen = false
    private var restored = false

    constructor(
        accountType: AccountType? = null,
        accountName: String = "",
        manualBackup: Boolean = false,
        fileBackup: Boolean = false,
        statPage: StatPage? = null
    ) : this() {
        this.accountType = accountType
        this.accountName = accountName
        this.manualBackup = manualBackup
        this.fileBackup = fileBackup
        this.statPage = statPage
    }

    override fun createState() = UiState(
        openSelectCoinsScreen = openSelectCoinsScreen,
        restored = restored,
    )

    fun setAccountData(accountType: AccountType?, accountName: String, manualBackup: Boolean, fileBackup: Boolean, statPage: StatPage) {
        this.accountType = accountType
        this.accountName = accountName
        this.manualBackup = manualBackup
        this.fileBackup = fileBackup
        this.statPage = statPage
    }

    fun setAccountType(accountType: AccountType) {
        this.accountType = accountType
    }

    fun setBirthdayHeightConfig(config: BirthdayHeightConfig?) {
        birthdayHeightConfig = config
    }

    fun requestOpenSelectCoinsScreen() {
        when (val tmpAccountType = accountType) {
            is AccountType.TronPrivateKey,
            is AccountType.StellarSecretKey -> {
                restoreWithSingleCoin(tmpAccountType)
                restored = true
                emitState()
            }

            else -> {
                openSelectCoinsScreen = true
                emitState()
            }
        }
    }

    fun openSelectCoinsScreenHandled() {
        openSelectCoinsScreen = false
        emitState()
    }

    private fun restoreWithSingleCoin(accountType: AccountType) {
        val allowedBlockchainTypes = BlockchainType.supported.filter { it.supports(accountType) }
        val tokenQueries = allowedBlockchainTypes
            .map { it.nativeTokenQueries }
            .flatten()

        val tokens = marketKit.tokens(tokenQueries)
            .filter { it.supports(accountType) }
            .sortedBy { it.type.order }

        val blockchains = tokens.map { it.blockchain }.toSet()

        val account = accountFactory.account(
            accountName,
            accountType,
            AccountOrigin.Restored,
            manualBackup,
            fileBackup,
        )
        accountManager.save(account)

        blockchains.forEach { blockchain ->
            tokenAutoEnableManager.markAutoEnable(account, blockchain.type)
        }

        val wallets = tokens.map { Wallet(it, account) }
        walletManager.save(wallets)

        statPage?.let { stat(page = it, event = StatEvent.ImportWallet(accountType.statAccountType)) }
    }

    data class UiState(
        val openSelectCoinsScreen: Boolean,
        val restored: Boolean
    )
}