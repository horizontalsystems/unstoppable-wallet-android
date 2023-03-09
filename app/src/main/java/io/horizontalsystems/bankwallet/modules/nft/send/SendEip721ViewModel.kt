package io.horizontalsystems.bankwallet.modules.nft.send

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.ext.collectWith
import io.horizontalsystems.bankwallet.core.adapters.nft.INftAdapter
import io.horizontalsystems.bankwallet.core.managers.NftMetadataManager
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.nft.NftUid
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmAddressService
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmData

class SendEip721ViewModel(
    private val nftUid: NftUid,
    private val adapter: INftAdapter,
    private val addressService: SendEvmAddressService,
    nftMetadataManager: NftMetadataManager
) : ViewModel() {

    private var addressState = addressService.stateFlow.value

    private val assetShortMetadata = nftMetadataManager.assetShortMetadata(nftUid)

    var uiState by mutableStateOf(
        SendNftModule.SendEip721UiState(
            name = assetShortMetadata?.name ?: "",
            imageUrl = assetShortMetadata?.previewImageUrl ?: "",
            addressError = addressState.addressError,
            canBeSend = addressState.canBeSend,
        )
    )
        private set

    init {
        addressService.stateFlow.collectWith(viewModelScope) {
            handleUpdatedAddressState(it)
        }
    }

    fun onEnterAddress(address: Address?) {
        addressService.setAddress(address)
    }

    fun getSendData(): SendEvmData? {
        val evmAddress = addressState.evmAddress ?: return null

        val transactionData = adapter.transferEip721TransactionData(
            nftUid.contractAddress,
            evmAddress,
            nftUid.tokenId
        ) ?: return null

        val nftShortMeta = assetShortMetadata?.let {
            SendEvmData.NftShortMeta(it.displayName, it.previewImageUrl)
        }

        val additionalInfo = SendEvmData.AdditionalInfo.Send(SendEvmData.SendInfo(nftShortMeta))

        return SendEvmData(transactionData, additionalInfo)
    }

    private fun handleUpdatedAddressState(addressState: SendEvmAddressService.State) {
        this.addressState = addressState
        val sendEvmData = getSendData()
        emitState(
            canBeSend = sendEvmData != null,
            addressError = addressState.addressError
        )
    }

    private fun emitState(canBeSend: Boolean, addressError: Throwable? = null) {
        uiState = SendNftModule.SendEip721UiState(
            name = assetShortMetadata?.name ?: "",
            imageUrl = assetShortMetadata?.previewImageUrl ?: "",
            addressError = addressError,
            canBeSend = canBeSend,
        )
    }
}
