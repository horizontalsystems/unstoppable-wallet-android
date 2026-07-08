package io.horizontalsystems.walletkit.entities

import io.horizontalsystems.walletkit.core.factories.uriScheme
import io.horizontalsystems.walletkit.core.managers.EvmBlockchainManager
import io.horizontalsystems.walletkit.core.supported
import io.horizontalsystems.walletkit.serializers.BigDecimalSerializer
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.serialization.Serializable
import java.math.BigDecimal

class AddressUri(
    val scheme: String,
) {
    var address: String = ""

    var parameters: MutableMap<Field, String> = mutableMapOf()
    var unhandledParameters: MutableMap<String, String> = mutableMapOf()

    inline fun <reified T> value(field: Field): T? {
        val value: Any? = when (T::class) {
            String::class -> parameters[field]
            BigDecimal::class -> parameters[field]?.toBigDecimalOrNull()
            Int::class -> parameters[field]?.toInt()
            else -> null
        }

        @Suppress("UNCHECKED_CAST")
        return value as? T
    }

    val amount: Amount?
        get() = value<BigDecimal>(Field.Value)?.let { Amount.Points(it) }
            ?: (value<BigDecimal>(Field.Amount) ?: value<BigDecimal>(Field.TxAmount))?.let { Amount.Decimals(it) }

    val memo: String?
        get() = value(Field.TxDescription)

    // The `value` field carries base units (wei, satoshi, lamports) — the token must be known
    // before it can be rendered, so conversion is deferred to where decimals are available.
    @Serializable
    sealed class Amount {
        abstract fun humanReadable(decimals: Int): BigDecimal

        @Serializable
        data class Points(@Serializable(with = BigDecimalSerializer::class) val value: BigDecimal) : Amount() {
            override fun humanReadable(decimals: Int): BigDecimal = value.movePointLeft(decimals).stripTrailingZeros()
        }

        @Serializable
        data class Decimals(@Serializable(with = BigDecimalSerializer::class) val value: BigDecimal) : Amount() {
            override fun humanReadable(decimals: Int): BigDecimal = value
        }
    }

    enum class Field(val value: String) {
        Amount("amount"),
        Value("value"),
        TxAmount("tx_amount"),
        TxDescription("tx_description"),
        Label("label"),
        Message("message"),
        BlockchainUid("blockchain_uid"),
        TokenUid("token_uid");

        companion object {
            fun amountField(blockchainType: BlockchainType): Field {
                return when {
                    blockchainType == BlockchainType.Monero -> {
                        TxAmount
                    }
                    else -> {
                        Amount
                    }
                }
            }
        }
    }

    val allowedBlockchainTypes: List<BlockchainType>?
        get() {
            val concreteUid: String? = value(Field.BlockchainUid)
            concreteUid?.let {
                return listOf(BlockchainType.fromUid(concreteUid))
            }

            val type: BlockchainType? = BlockchainType.supported.firstOrNull { it.uriScheme == scheme }
            type?.let {
                return if (EvmBlockchainManager.blockchainTypes.contains(type)) {
                    EvmBlockchainManager.blockchainTypes
                } else {
                    listOf(type)
                }
            }

            return null
        }
}

