package cash.p.terminal.trezor.domain.usecase

import cash.p.terminal.trezor.domain.TrezorDeepLinkManager
import cash.p.terminal.trezor.domain.model.TrezorMethod
import cash.p.terminal.wallet.entities.HardwarePublicKey
import cash.p.terminal.wallet.entities.HardwarePublicKeyType
import cash.p.terminal.wallet.entities.SecretString
import cash.p.terminal.wallet.entities.TokenQuery
import io.horizontalsystems.core.entities.BlockchainType
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber

internal class FetchTrezorPublicKeysUseCaseImpl(
    private val deepLinkManager: TrezorDeepLinkManager
) : FetchTrezorPublicKeysUseCase {

    @OptIn(ExperimentalStdlibApi::class)
    override suspend fun invoke(
        tokenQueries: List<TokenQuery>,
        accountId: String
    ): List<HardwarePublicKey> {
        val queriesBySpec = tokenQueries.mapNotNull { query ->
            getBlockchainSpec(query.blockchainType)?.let { query to it }
        }
        val uniqueSpecs = queriesBySpec.map { it.second }.distinct()
        val methodGroups = uniqueSpecs.groupBy { it.method }

        val keyCache = mutableMapOf<BlockchainSpec, KeyData?>()
        for ((method, specs) in methodGroups) {
            Timber.d("Trezor: calling ${method.value} with ${specs.size} spec(s)")
            if (specs.size == 1) {
                val spec = specs.single()
                keyCache[spec] = fetchSingle(spec)
            } else {
                val results = fetchBundle(method, specs)
                specs.forEachIndexed { i, spec -> keyCache[spec] = results.getOrNull(i) }
            }
        }

        return queriesBySpec.mapNotNull { (query, spec) ->
            val data = keyCache[spec] ?: return@mapNotNull null
            HardwarePublicKey(
                accountId = accountId,
                blockchainType = query.blockchainType.uid,
                type = HardwarePublicKeyType.PUBLIC_KEY,
                tokenType = query.tokenType,
                key = SecretString(data.xpub),
                derivationPath = spec.path,
                publicKey = data.publicKey,
                derivedPublicKey = data.publicKey
            )
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private suspend fun fetchSingle(spec: BlockchainSpec): KeyData? {
        val params = JsonObject(
            buildMap {
                spec.coin?.let { put("coin", JsonPrimitive(it)) }
                put("path", JsonPrimitive(spec.path))
            }
        )
        val response = deepLinkManager.call(spec.method, params)
        Timber.d("Trezor: ${spec.method.value} coin=${spec.coin} path=${spec.path} success=${response.success}")
        if (!response.success) return null
        return parseKeyData(response.payload?.jsonObject, spec.keyField)
    }

    @OptIn(ExperimentalStdlibApi::class)
    private suspend fun fetchBundle(
        method: TrezorMethod,
        specs: List<BlockchainSpec>
    ): List<KeyData?> {
        val bundleItems = specs.map { spec ->
            JsonObject(
                buildMap {
                    spec.coin?.let { put("coin", JsonPrimitive(it)) }
                    put("path", JsonPrimitive(spec.path))
                    put("showOnTrezor", JsonPrimitive(false))
                }
            )
        }
        val params = JsonObject(mapOf("bundle" to JsonArray(bundleItems)))
        val response = deepLinkManager.call(method, params)
        Timber.d("Trezor: ${method.value} bundle success=${response.success}")
        if (!response.success) return specs.map { null }

        val payloadArray = response.payload?.jsonArray ?: return specs.map { null }
        return specs.mapIndexed { i, spec ->
            val item = payloadArray.getOrNull(i)?.jsonObject
            parseKeyData(item, spec.keyField)
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun parseKeyData(payload: JsonObject?, keyField: String): KeyData? {
        val keyValue = payload?.get(keyField)?.jsonPrimitive?.content ?: return null
        val publicKeyHex = payload["publicKey"]?.jsonPrimitive?.content ?: ""
        val publicKeyBytes = if (publicKeyHex.isNotEmpty()) publicKeyHex.hexToByteArray() else ByteArray(0)
        val chainCodeHex = payload["chainCode"]?.jsonPrimitive?.content ?: ""
        val chainCodeBytes = if (chainCodeHex.isNotEmpty()) chainCodeHex.hexToByteArray() else ByteArray(0)
        return KeyData(keyValue, publicKeyBytes, chainCodeBytes)
    }

    private fun getBlockchainSpec(blockchainType: BlockchainType): BlockchainSpec? =
        when (blockchainType) {
            BlockchainType.Bitcoin -> BlockchainSpec("btc", "m/84'/0'/0'", TrezorMethod.BtcGetPublicKey)
            BlockchainType.Litecoin -> BlockchainSpec("ltc", "m/84'/2'/0'", TrezorMethod.BtcGetPublicKey)
            BlockchainType.BitcoinCash -> BlockchainSpec("bch", "m/44'/145'/0'", TrezorMethod.BtcGetPublicKey)
            BlockchainType.Dash -> BlockchainSpec("dash", "m/44'/5'/0'", TrezorMethod.BtcGetPublicKey)
            BlockchainType.Dogecoin -> BlockchainSpec("doge", "m/44'/3'/0'", TrezorMethod.BtcGetPublicKey)
            BlockchainType.Zcash -> BlockchainSpec("zec", "m/44'/133'/0'", TrezorMethod.BtcGetPublicKey)
            BlockchainType.Ethereum -> BlockchainSpec(null, "m/44'/60'/0'/0/0", TrezorMethod.EthGetPublicKey)
            BlockchainType.BinanceSmartChain -> BlockchainSpec(null, "m/44'/60'/0'/0/0", TrezorMethod.EthGetPublicKey)
            BlockchainType.Polygon -> BlockchainSpec(null, "m/44'/60'/0'/0/0", TrezorMethod.EthGetPublicKey)
            BlockchainType.ArbitrumOne -> BlockchainSpec(null, "m/44'/60'/0'/0/0", TrezorMethod.EthGetPublicKey)
            BlockchainType.Optimism -> BlockchainSpec(null, "m/44'/60'/0'/0/0", TrezorMethod.EthGetPublicKey)
            BlockchainType.Base -> BlockchainSpec(null, "m/44'/60'/0'/0/0", TrezorMethod.EthGetPublicKey)
            BlockchainType.Solana -> BlockchainSpec(null, "m/44'/501'/0'/0'", TrezorMethod.SolGetPublicKey, keyField = "publicKey")
            BlockchainType.Stellar -> BlockchainSpec(null, "m/44'/148'/0'", TrezorMethod.XlmGetAddress, keyField = "address")
            else -> null
        }

    private data class BlockchainSpec(
        val coin: String?,
        val path: String,
        val method: TrezorMethod,
        val keyField: String = "xpub"
    )

    private class KeyData(
        val xpub: String,
        val publicKey: ByteArray,
        val chainCode: ByteArray
    )
}
