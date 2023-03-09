package cash.p.terminal.modules.nft.send

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.ext.collectWith
import cash.p.terminal.core.adapters.nft.INftAdapter
import cash.p.terminal.core.managers.NftMetadataManager
import cash.p.terminal.entities.Address
import cash.p.terminal.entities.DataState
import cash.p.terminal.entities.nft.NftUid
import cash.p.terminal.modules.send.evm.SendEvmAddressService
import cash.p.terminal.modules.send.evm.SendEvmData
import java.math.BigInteger

class SendEip1155ViewModel(
    private val nftUid: NftUid,
    private val nftBalance: Int,
    private val adapter: INftAdapter,
    private val addressService: SendEvmAddressService,
    nftMetadataManager: NftMetadataManager
) : ViewModel() {

    private var addressState = addressService.stateFlow.value
    private var amountState by mutableStateOf<DataState<Int>?>(DataState.Success(1))
    private val assetShortMetadata = nftMetadataManager.assetShortMetadata(nftUid)

    val availableNftBalance = "$nftBalance NFT"

    var uiState by mutableStateOf(
        SendNftModule.SendEip1155UiState(
            name = assetShortMetadata?.name ?: "",
            imageUrl = assetShortMetadata?.previewImageUrl ?: "",
            addressError = addressState.addressError,
            amountState = amountState,
            canBeSend = addressState.canBeSend,
        )
    )
        private set

    init {
        addressService.stateFlow.collectWith(viewModelScope) {
            addressState = it
            syncState()
        }
    }

    fun onAmountChange(amount: Int) {
        amountState = try {
            val validAmount = validEvmAmount(amount)
            DataState.Success(validAmount)
        } catch (e: InsufficientNftBalance) {
            DataState.Error(e)
        }
        syncState()
    }

    fun onEnterAddress(address: Address?) {
        addressService.setAddress(address)
    }

    fun getSendData(): SendEvmData? {
        val evmAddress = addressState.evmAddress ?: return null
        val amount = amountState?.dataOrNull?.toBigInteger() ?: return null
        if (amount <= BigInteger.ZERO) {
            return null
        }

        val transactionData = adapter.transferEip1155TransactionData(
            nftUid.contractAddress,
            evmAddress,
            nftUid.tokenId,
            amount
        ) ?: return null

        val nftShortMeta = assetShortMetadata?.let {
            SendEvmData.NftShortMeta(it.displayName, it.previewImageUrl)
        }

        val additionalInfo =
            SendEvmData.AdditionalInfo.Send(SendEvmData.SendInfo(nftShortMeta))

        return SendEvmData(transactionData, additionalInfo)
    }

    private fun syncState() {
        val sendEvmData = getSendData()
        emitState(
            canBeSend = sendEvmData != null,
            amountState = amountState,
            addressError = addressState.addressError
        )
    }

    private fun emitState(canBeSend: Boolean, amountState: DataState<Int>?, addressError: Throwable? = null) {
        uiState = SendNftModule.SendEip1155UiState(
            name = assetShortMetadata?.name ?: "",
            imageUrl = assetShortMetadata?.previewImageUrl ?: "",
            addressError = addressError,
            amountState = amountState,
            canBeSend = canBeSend,
        )
    }

    private fun validEvmAmount(amount: Int): Int {
        if (amount > nftBalance) {
            throw InsufficientNftBalance()
        }

        return amount
    }

    class InsufficientNftBalance : Exception()
}
