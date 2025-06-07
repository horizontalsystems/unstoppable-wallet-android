package cash.p.terminal.tangem.domain.derivation

import cash.p.terminal.tangem.domain.address.AddressType
import cash.p.terminal.tangem.domain.replacePurpose
import com.tangem.crypto.hdWallet.DerivationPath
import io.horizontalsystems.core.entities.BlockchainType

/**
 * Derivation config for Wallet 2.0
 *
 * Types:
 * - `Stellar`, `Solana`, `TON`. According to `SEP0005`
 * https://github.com/stellar/stellar-protocol/blob/master/ecosystem/sep-0005.md
 * - `Polkadot`, `Kusama` used to all nodes in the path is hardened
 * https://wiki.polkadot.network/docs/learn-account-advanced#derivation-paths
 * - `Cardano`. According to  `CIP1852`
 * https://cips.cardano.org/cips/cip1852/
 * - `Bitcoin`, `Litecoin`. Default address is `SegWit`. According to `BIP-84`
 * https://github.com/bitcoin/bips/blob/master/bip-0084.mediawiki
 * - `EVM-like` without `Ethereum classic` with `Ethereum` coinType(60).
 * - `All else`. According to `BIP44`
 * https://github.com/satoshilabs/slips/blob/master/slip-0044.md
 */

object DerivationConfigV3 : DerivationConfig() {

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    override fun derivations(blockchainType: BlockchainType, customPurpose: String): Map<AddressType, DerivationPath> {
        return when (blockchainType) {
            BlockchainType.Bitcoin -> mapOf(AddressType.Default to DerivationPath("m/84'/0'/0'"))
            BlockchainType.Litecoin -> mapOf(AddressType.Default to DerivationPath("m/84'/2'/0'"))
            BlockchainType.Solana -> mapOf(AddressType.Default to DerivationPath("m/44'/501'/0'"))
            BlockchainType.BitcoinCash -> mapOf(AddressType.Default to DerivationPath("m/44'/145'/0'"))

            BlockchainType.BinanceSmartChain,
            BlockchainType.Ethereum,
            BlockchainType.Polygon,
            BlockchainType.Avalanche,
            BlockchainType.Fantom,
            BlockchainType.ArbitrumOne,
            BlockchainType.Gnosis,
            BlockchainType.Optimism,
            BlockchainType.ZkSync,
            BlockchainType.Base,
                -> mapOf(AddressType.Default to DerivationPath("m/44'/60'/0'/0/0"))


            BlockchainType.BinanceChain -> mapOf(AddressType.Default to DerivationPath("m/44'/714'/0'/0/0"))

            BlockchainType.Dogecoin -> mapOf(AddressType.Default to DerivationPath("m/44'/3'/0'"))
            BlockchainType.Tron -> mapOf(AddressType.Default to DerivationPath("m/44'/195'/0'/0/0"))
            BlockchainType.Dash -> mapOf(AddressType.Default to DerivationPath("m/44'/5'/0'"))
            BlockchainType.Ton -> mapOf(AddressType.Default to DerivationPath("m/44'/607'/0'"))

            BlockchainType.Cosanta -> mapOf(AddressType.Default to DerivationPath("m/44'/770'/0'"))
            BlockchainType.PirateCash -> mapOf(AddressType.Default to DerivationPath("m/44'/660'/0'"))

            BlockchainType.ECash,
            BlockchainType.Zcash,
            is BlockchainType.Unsupported ->
                throw IllegalArgumentException("Unsupported blockchain type: $blockchainType")
        }.mapValues {
            it.value.replacePurpose(customPurpose) ?: it.value
        }
    }
}
