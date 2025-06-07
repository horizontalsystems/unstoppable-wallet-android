package cash.p.terminal.tangem.domain.usecase

import android.util.Log
import cash.p.terminal.tangem.domain.getDerivationStyle
import cash.p.terminal.tangem.domain.getPurpose
import cash.p.terminal.tangem.domain.model.ScanResponse
import cash.p.terminal.wallet.entities.HardwarePublicKey
import cash.p.terminal.wallet.entities.HardwarePublicKeyType
import cash.p.terminal.wallet.entities.SecretString
import cash.p.terminal.wallet.entities.TokenQuery
import cash.p.terminal.wallet.entities.TokenType
import com.tangem.common.extensions.toHexString
import io.horizontalsystems.core.entities.BlockchainType
import io.horizontalsystems.hdwalletkit.ExtendedKeyCoinType
import io.horizontalsystems.hdwalletkit.HDExtendedKey
import io.horizontalsystems.hdwalletkit.HDExtendedKeyVersion
import io.horizontalsystems.hdwalletkit.HDKey

class BuildHardwarePublicKeyUseCase {

    operator fun invoke(
        scanResponse: ScanResponse,
        accountId: String,
        blockchainTypeList: List<TokenQuery>
    ): List<HardwarePublicKey> = blockchainTypeList.mapNotNull { (blockchainType, tokenType) ->
        getKey(scanResponse, blockchainType, tokenType)?.let { (type, walletPublicKey) ->
            val derivationPath = scanResponse.card.getDerivationStyle()?.getConfig()
                ?.derivations(blockchainType, tokenType.getPurpose())?.values?.firstOrNull()
                ?: return@mapNotNull null
            HardwarePublicKey(
                accountId = accountId,
                blockchainType = blockchainType.uid,
                type = type,
                tokenType = tokenType,
                key = SecretString(walletPublicKey.externalPublicKey),
                publicKey = walletPublicKey.bytes,
                derivationPath = derivationPath.rawPath,
            )
        }
    }

    private fun getKey(
        scanResponse: ScanResponse,
        blockchainType: BlockchainType,
        tokenType: TokenType
    ): Pair<HardwarePublicKeyType, WalletPublicKey>? =
        when (blockchainType) {
            BlockchainType.Solana,
            BlockchainType.Ton -> getAddress(
                scanResponse,
                blockchainType,
                tokenType
            )?.let {
                HardwarePublicKeyType.ADDRESS to it
            }

            else -> buildExtendedKey(scanResponse, blockchainType, tokenType)?.let {
                HardwarePublicKeyType.PUBLIC_KEY to it
            }
        }

    private fun getAddress(
        scanResponse: ScanResponse,
        blockchainType: BlockchainType,
        tokenType: TokenType
    ): WalletPublicKey? {
        val derivationPath = scanResponse.card.getDerivationStyle()?.getConfig()
            ?.derivations(blockchainType, tokenType.getPurpose())?.values?.firstOrNull()
            ?: return null

        for ((publicKey, extendedPublicKeyMap) in scanResponse.derivedKeys) {
            extendedPublicKeyMap[derivationPath]?.let {
                return WalletPublicKey(
                    bytes = publicKey.bytes,
                    externalPublicKey = it.publicKey.toHexString()
                )
            }
        }
        return null
    }

    private fun buildExtendedKey(
        scanResponse: ScanResponse,
        blockchainType: BlockchainType,
        tokenType: TokenType
    ): WalletPublicKey? {
        val derivationPath = scanResponse.card.getDerivationStyle()?.getConfig()
            ?.derivations(blockchainType, tokenType.getPurpose())?.values?.firstOrNull()
            ?: return null
        for ((publicKey, extendedPublicKeyMap) in scanResponse.derivedKeys) {
            extendedPublicKeyMap[derivationPath]?.let { extendedPublicKey ->
//                val derivedKeyData = scanResponse.derivedKeys.values.first().values.first()

                val tangemDepth = derivationPath.nodes.size
                val tangemParentFingerprint = 0
                val tangemChildNumber = extendedPublicKey.childNumber.toInt()

                val isHardenedPathSegment = derivationPath.nodes.lastOrNull()?.isHardened == true
                val purpose = derivationPath.getPurpose()
                if (purpose == null) {
                    Log.d(
                        "BuildHardwarePublicKeyUseCase",
                        "Unsupported purpose in derivation path: ${derivationPath.rawPath}"
                    )
                    return null
                }
                val coinType = when (blockchainType) {
                    BlockchainType.Litecoin -> ExtendedKeyCoinType.Litecoin
                    else -> ExtendedKeyCoinType.Bitcoin
                }

                val hdKeyInstance = HDKey(
                    /* pubKey = */ extendedPublicKey.publicKey,
                    /* chainCode = */ extendedPublicKey.chainCode,
                    /* parent = */ null,
                    /* parentFingerprint = */ tangemParentFingerprint,
                    /* depth = */ tangemDepth,
                    /* childNumber = */ tangemChildNumber,
                    /* isHardened = */ isHardenedPathSegment
                )
                val hdKeyVersion = HDExtendedKeyVersion.initFrom(
                    purpose = purpose,
                    coinType = coinType,
                    isPrivate = true
                )

                return WalletPublicKey(
                    bytes = publicKey.bytes,
                    externalPublicKey = HDExtendedKey(hdKeyInstance, hdKeyVersion).serialize()
                )
            }
        }

        return null
    }
}

private class WalletPublicKey(
    val bytes: ByteArray,
    val externalPublicKey: String
)