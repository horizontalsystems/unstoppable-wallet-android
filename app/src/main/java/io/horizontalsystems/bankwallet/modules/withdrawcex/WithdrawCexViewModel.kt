package io.horizontalsystems.bankwallet.modules.withdrawcex

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.HSCaution
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.core.providers.CexAsset
import io.horizontalsystems.bankwallet.core.providers.CexWithdrawNetwork
import io.horizontalsystems.bankwallet.core.providers.CoinzixCexProvider
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.coinzixverify.CoinzixVerificationMode
import io.horizontalsystems.bankwallet.modules.contacts.ContactsRepository
import io.horizontalsystems.bankwallet.modules.contacts.model.Contact
import io.horizontalsystems.bankwallet.modules.fee.FeeItem
import io.horizontalsystems.bankwallet.modules.xrate.XRateService
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.launch
import java.math.BigDecimal

class WithdrawCexViewModel(
    val cexAsset: CexAsset,
    private var network: CexWithdrawNetwork,
    private val xRateService: XRateService,
    private val amountService: CexWithdrawAmountService,
    private val addressService: CexWithdrawAddressService,
    private val cexProvider: CoinzixCexProvider,
    private val contactsRepository: ContactsRepository
) : ViewModel() {
    private val coinUid = cexAsset.coin?.uid

    val fiatMaxAllowedDecimals = App.appConfigProvider.fiatDecimal
    val coinMaxAllowedDecimals = cexAsset.decimals
    val networks = cexAsset.withdrawNetworks
    val networkSelectionEnabled = networks.size > 1

    val blockchainType get() = network.blockchain?.type
    fun hasContacts() = blockchainType?.let { blockchainType ->
        contactsRepository.getContactsFiltered(blockchainType).isNotEmpty()
    } ?: false

    var coinRate by mutableStateOf(coinUid?.let { xRateService.getRate(it) })
        private set
    var value by mutableStateOf("")
        private set

    private var amountState = amountService.stateFlow.value
    private var addressState = addressService.stateFlow.value
    private var feeItem = FeeItem("", null)
    private var feeFromAmount = false

    var uiState by mutableStateOf(
        WithdrawCexUiState(
            networkName = network.networkName,
            availableBalance = amountState.availableBalance,
            amountCaution = amountState.amountCaution,
            addressState = addressState,
            feeItem = feeItem,
            feeFromAmount = feeFromAmount,
            canBeSend = amountState.canBeSend && addressState?.dataOrNull != null
        )
    )
        private set

    init {
        coinUid?.let {
            viewModelScope.launch {
                xRateService.getRateFlow(it).collect {
                    coinRate = it
                }
            }
        }

        viewModelScope.launch {
            amountService.stateFlow.collect {
                handleUpdatedAmountState(it)
            }
        }

        viewModelScope.launch {
            addressService.stateFlow.collect {
                handleUpdatedAddressState(it)
            }
        }
    }

    private fun handleUpdatedAmountState(amountState: CexWithdrawAmountService.State) {
        this.amountState = amountState
        feeItem = feeItem(amountState)

        emitState()
    }

    private fun feeItem(amountState: CexWithdrawAmountService.State): FeeItem {
        val coinAmount = App.numberFormatter.formatCoinFull(
            amountState.fee,
            cexAsset.id,
            coinMaxAllowedDecimals
        )
        val currencyAmount = coinRate?.let { rate ->
            rate.copy(value = amountState.fee.times(rate.value)).getFormattedFull()
        }

        return FeeItem(coinAmount, currencyAmount)
    }

    private fun handleUpdatedAddressState(addressState: DataState<Address>?) {
        this.addressState = addressState

        emitState()
    }

    private fun emitState() {
        viewModelScope.launch {
            uiState = WithdrawCexUiState(
                networkName = network.networkName,
                availableBalance = amountState.availableBalance,
                amountCaution = amountState.amountCaution,
                addressState = addressState,
                feeItem = feeItem,
                feeFromAmount = feeFromAmount,
                canBeSend = amountState.canBeSend && addressState?.dataOrNull != null
            )
        }
    }

    fun onEnterAmount(v: BigDecimal?) {
        amountService.setAmount(v)
    }

    fun onEnterAddress(v: String) {
        value = v
        viewModelScope.launch {
            addressService.setAddress(v)
        }
    }

    fun onSelectNetwork(network: CexWithdrawNetwork) {
        this.network = network
        amountService.setNetwork(network)
        viewModelScope.launch {
            addressService.setBlockchain(network.blockchain)
        }
    }

    fun onSelectFeeFromAmount(feeFromAmount: Boolean) {
        this.feeFromAmount = feeFromAmount
        amountService.setFeeFromAmount(feeFromAmount)
    }

    fun getConfirmationData(): ConfirmationData {
        val amount = amountState.amount ?: BigDecimal.ZERO
        var receivedAmount = if (amountState.feeFromAmount) {
            amount - amountState.fee
        } else {
            amount
        }
        receivedAmount = receivedAmount.coerceAtLeast(BigDecimal.ZERO)

        val coinAmount = App.numberFormatter.formatCoinFull(
            receivedAmount,
            cexAsset.id,
            coinMaxAllowedDecimals
        )
        val currencyAmount = coinRate?.let { rate ->
            rate.copy(value = receivedAmount.times(rate.value))
                .getFormattedFull()
        }

        val address = addressState?.dataOrNull!!
        val contact = contactsRepository.getContactsFiltered(
            blockchainType,
            addressQuery = address.hex
        ).firstOrNull()

        return ConfirmationData(
            assetName = cexAsset.name,
            coinAmount = coinAmount,
            currencyAmount = currencyAmount,
            coinIconUrl = cexAsset.coin?.imageUrl,
            address = address,
            contact = contact,
            blockchainType = network.blockchain?.type,
            networkName = network.networkName,
            feeItem = feeItem
        )
    }

    suspend fun confirm(): CoinzixVerificationMode.Withdraw {
        return cexProvider.withdraw(
            cexAsset.id,
            network.id,
            addressState?.dataOrNull!!.hex,
            amountState.amount!!,
            feeFromAmount
        )
    }
}

data class ConfirmationData(
    val assetName: String,
    val coinAmount: String,
    val currencyAmount: String?,
    val coinIconUrl: String?,
    val address: Address,
    val contact: Contact?,
    val blockchainType: BlockchainType?,
    val networkName: String?,
    val feeItem: FeeItem
)

data class WithdrawCexUiState(
    val networkName: String?,
    val availableBalance: BigDecimal?,
    val amountCaution: HSCaution?,
    val feeItem: FeeItem,
    val feeFromAmount: Boolean,
    val addressState: DataState<Address>?,
    val canBeSend: Boolean
)