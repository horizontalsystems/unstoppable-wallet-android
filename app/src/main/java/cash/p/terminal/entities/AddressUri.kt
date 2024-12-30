package cash.p.terminal.entities

import cash.p.terminal.core.factories.uriScheme
import cash.p.terminal.core.managers.EvmBlockchainManager
import cash.p.terminal.core.supported
import io.horizontalsystems.core.entities.BlockchainType
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

    val amount: BigDecimal?
        get() = value<BigDecimal>(Field.Amount) ?: value(Field.Value)


    enum class Field(val value: String) {
        Amount("amount"),
        Value("value"),
        Label("label"),
        Message("message"),
        BlockchainUid("blockchain_uid"),
        TokenUid("token_uid");

        companion object {
            fun amountField(blockchainType: BlockchainType): Field {
                return if (EvmBlockchainManager.blockchainTypes.contains(blockchainType)) {
                    Value
                } else {
                    Amount
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

