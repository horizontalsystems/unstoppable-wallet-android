package cash.p.terminal.modules.managewallets

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.p.terminal.core.App
import cash.p.terminal.core.R
import cash.p.terminal.core.iconPlaceholder
import cash.p.terminal.core.storage.HardwarePublicKeyStorage
import cash.p.terminal.modules.restoreaccount.restoreblockchains.CoinViewItem
import cash.p.terminal.tangem.domain.TangemConfig
import cash.p.terminal.tangem.domain.usecase.BuildHardwarePublicKeyUseCase
import cash.p.terminal.tangem.domain.usecase.TangemBlockchainTypeExistUseCase
import cash.p.terminal.tangem.domain.usecase.TangemScanUseCase
import cash.p.terminal.ui_compose.components.ImageSource
import cash.p.terminal.wallet.AccountType
import cash.p.terminal.wallet.Clearable
import cash.p.terminal.wallet.IAccountManager
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.alternativeImageUrl
import cash.p.terminal.wallet.badge
import cash.p.terminal.wallet.entities.TokenQuery
import cash.p.terminal.wallet.entities.TokenType
import cash.p.terminal.wallet.imageUrl
import com.tangem.common.core.TangemSdkError.UserCancelled
import com.tangem.common.doOnFailure
import com.tangem.common.doOnSuccess
import io.horizontalsystems.core.SnackbarDuration
import io.horizontalsystems.core.entities.BlockchainType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject

class ManageWalletsViewModel(
    private val service: ManageWalletsService,
    private val clearables: List<Clearable>
) : ViewModel() {
    private val accountManager: IAccountManager by inject(IAccountManager::class.java)
    private val tangemBlockchainTypeExistUseCase: TangemBlockchainTypeExistUseCase by inject<TangemBlockchainTypeExistUseCase>(
        TangemBlockchainTypeExistUseCase::class.java
    )
    private val tangemScanUseCase: TangemScanUseCase by inject(TangemScanUseCase::class.java)
    private val hardwarePublicKeyStorage: HardwarePublicKeyStorage by inject(
        HardwarePublicKeyStorage::class.java
    )

    val viewItemsLiveData = MutableLiveData<List<CoinViewItem<Token>>>()

    private val awaitingEnabledTokens = mutableSetOf<Token>()
    var showScanToAddButton by mutableStateOf(false)
        private set

    var errorMsg by mutableStateOf<String?>(null)
        private set

    var closeScreen by mutableStateOf(false)
        private set

    init {
        loadData()
    }

    private fun loadData() {
        if (isHardwareCard()) {
            viewModelScope.launch {
                accountManager.activeAccount?.let { account ->
                    tangemBlockchainTypeExistUseCase.loadKeys(account.id)
                }
            }
        }
        viewModelScope.launch {
            service.itemsFlow.collect {
                sync(it)
            }
        }
    }

    fun requestScanToAddTokens(closeAfterSuccess: Boolean) = viewModelScope.launch {
        val account = accountManager.activeAccount
        val cardId = (account?.type as? AccountType.HardwareCard?)?.cardId
        if (account == null || cardId == null) {
            showError(App.instance.getString(R.string.error_no_active_account))
            return@launch
        }
        val blockchainTypesToDerive = awaitingEnabledTokens.map {
            TokenQuery(
                blockchainType = it.blockchainType,
                tokenType = it.type
            )
        }.distinct()
        tangemScanUseCase.scanProduct(
            blockchainsToDerive = blockchainTypesToDerive,
            cardId = cardId,
        ).doOnSuccess { scanResponse ->
            val publicKeys =
                BuildHardwarePublicKeyUseCase().invoke(
                    scanResponse = scanResponse,
                    accountId = account.id,
                    blockchainTypeList = blockchainTypesToDerive
                )
            val (addedTokens, notFoundedTokens) = awaitingEnabledTokens.partition { token ->
                publicKeys.find { it.blockchainType == token.blockchainType.uid } != null
            }

            val newAwaited = awaitingEnabledTokens - addedTokens

            with(awaitingEnabledTokens) {
                clear()
                addAll(newAwaited)
            }
            updateNeedToShowScanToAddButton()

            errorMsg = if (notFoundedTokens.isNotEmpty()) {
                "Some tokens were not found"
            } else {
                null
            }

            hardwarePublicKeyStorage.save(publicKeys)

            addedTokens.forEach {
                service.enable(it)
            }

            if (errorMsg == null && closeAfterSuccess) {
                closeScreen = true
            }
        }.doOnFailure {
            if (it is UserCancelled) return@launch
            showError(it.customMessage)
        }
    }

    private fun showError(msg: String?) {
        errorMsg = msg
        if (msg == null) return

        viewModelScope.launch {
            delay(SnackbarDuration.LONG.value.toLong())
            errorMsg = null
        }
    }

    private fun sync(items: List<ManageWalletsService.Item>) {
        val viewItems = items.map { viewItem(it) }
        viewItemsLiveData.postValue(viewItems)
    }

    private fun viewItem(
        item: ManageWalletsService.Item,
    ) = CoinViewItem(
        item = item.token,
        imageSource = ImageSource.Remote(
            item.token.coin.imageUrl,
            item.token.iconPlaceholder,
            item.token.coin.alternativeImageUrl
        ),
        title = item.token.coin.code,
        subtitle = item.token.coin.name,
        enabled = item.enabled || awaitingEnabledTokens.contains(item.token),
        hasInfo = item.hasInfo,
        label = item.token.badge
    )

    fun enable(token: Token) {
        if (!isHardwareCard() || tangemBlockchainTypeExistUseCase(token)) {
            if (isMonero(token)) {
                showError(App.instance.getString(R.string.monero_not_supported_for_wallet))
                return
            }
            service.enable(token)
        } else {
            if (TangemConfig.isExcludedForHardwareCard(token)) {
                showError(App.instance.getString(R.string.error_hardware_wallet_not_supported))
                return
            }
            awaitingEnabledTokens.add(token)
            updateNeedToShowScanToAddButton()

            // Update switch indicator based on `awaitingEnabledTokens` values
            sync(service.itemsFlow.value)
        }
    }

    private fun isMonero(token: Token) =
        (token.tokenQuery.blockchainType == BlockchainType.Monero &&
                token.tokenQuery.tokenType == TokenType.Native)

    fun disable(token: Token) {
        service.disable(token)
        if (isHardwareCard()) {
            if (awaitingEnabledTokens.remove(token)) {
                // Update switch indicator based on `awaitingEnabledTokens` values
                sync(service.itemsFlow.value)
            }
            updateNeedToShowScanToAddButton()
        }
    }

    fun updateFilter(filter: String) {
        service.setFilter(filter)
    }

    private fun updateNeedToShowScanToAddButton() {
        showScanToAddButton = isHardwareCard() && awaitingEnabledTokens.isNotEmpty()
    }

    val addTokenEnabled: Boolean
        get() = service.accountType?.canAddTokens == true

    override fun onCleared() {
        clearables.forEach(Clearable::clear)
    }

    private fun isHardwareCard() = accountManager.activeAccount?.type is AccountType.HardwareCard
}
