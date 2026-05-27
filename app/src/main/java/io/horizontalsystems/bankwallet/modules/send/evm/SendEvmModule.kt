package io.horizontalsystems.bankwallet.modules.send.evm

import android.os.Parcelable
import io.horizontalsystems.bankwallet.serializers.BigIntegerSerializer
import io.horizontalsystems.ethereumkit.models.TransactionData
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import java.math.BigInteger


data class SendEvmData(
    val transactionData: TransactionData,
    val additionalInfo: AdditionalInfo? = null
) {
    @Serializable
    sealed class AdditionalInfo : Parcelable {
        @Serializable
        @Parcelize
        class Send(val info: SendInfo) : AdditionalInfo()

        val sendInfo: SendInfo?
            get() = (this as? Send)?.info
    }

    @Serializable
    @Parcelize
    data class SendInfo(
        val nftShortMeta: NftShortMeta? = null
    ) : Parcelable

    @Serializable
    @Parcelize
    data class NftShortMeta(
        val nftName: String,
        val previewImageUrl: String?
    ) : Parcelable
}

object SendEvmModule {

    @Serializable
    @Parcelize
    data class TransactionDataParcelable(
        val toAddress: String,
        @Serializable(with = BigIntegerSerializer::class) val value: BigInteger,
        val input: ByteArray
    ) : Parcelable {
        constructor(transactionData: TransactionData) : this(
            transactionData.to.hex,
            transactionData.value,
            transactionData.input
        )
    }
}
