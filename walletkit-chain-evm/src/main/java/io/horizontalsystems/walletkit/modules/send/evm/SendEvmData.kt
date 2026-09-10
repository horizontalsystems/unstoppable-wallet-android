package io.horizontalsystems.walletkit.modules.send.evm

import io.horizontalsystems.ethereumkit.models.TransactionData
import kotlinx.serialization.Serializable

data class SendEvmData(
    val transactionData: TransactionData,
    val additionalInfo: AdditionalInfo? = null
) {
    @Serializable
    sealed class AdditionalInfo {
        @Serializable
        class Send(val info: SendInfo) : AdditionalInfo()

        val sendInfo: SendInfo?
            get() = (this as? Send)?.info
    }

    @Serializable
    data class SendInfo(
        val nftShortMeta: NftShortMeta? = null
    )

    @Serializable
    data class NftShortMeta(
        val nftName: String,
        val previewImageUrl: String?
    )
}
