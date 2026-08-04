package io.horizontalsystems.walletkit.core

import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.factories.removeScheme
import io.horizontalsystems.walletkit.core.factories.uriScheme
import io.horizontalsystems.walletkit.entities.AccountType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.math.BigInteger

/**
 * Golden-file snapshot of per-chain behavior currently encoded in `when (blockchainType)`
 * dispatches (MarketKitExtensions.kt, Extensions.kt, AddressParserFactory.kt).
 *
 * The modularization refactor (see docs/Walletkit-Modularization-Plan.md) replaces those
 * dispatches with ChainRegistry lookups; this test pins the observable behavior so every
 * conversion step is provably behavior-preserving.
 *
 * To regenerate the fixture after an INTENTIONAL behavior change:
 *   ./gradlew :walletkit:testDebugUnitTest --tests "*ChainBehaviorParityTest*" -PupdateParityFixture=true
 */
class ChainBehaviorParityTest {

    private val sampleTokenTypes: List<Pair<String, TokenType>> = listOf(
        "Native" to TokenType.Native,
        "Eip20" to TokenType.Eip20("0x0000000000000000000000000000000000000000"),
        "Spl" to TokenType.Spl("mint"),
        "Jetton" to TokenType.Jetton("jetton"),
        "Asset" to TokenType.Asset("CODE", "issuer"),
        "ThorchainAsset" to TokenType.ThorchainAsset("denom"),
        "ZanoAsset" to TokenType.ZanoAsset("ref"),
        "Derived(Bip84)" to TokenType.Derived(TokenType.Derivation.Bip84),
        "AddressTyped(Type145)" to TokenType.AddressTyped(TokenType.AddressType.Type145),
    )

    private val sampleAccountTypes: List<Pair<String, AccountType>> = listOf(
        "Mnemonic" to AccountType.Mnemonic(List(12) { "abandon" }, ""),
        "MoneroMnemonic" to AccountType.MoneroMnemonic(List(25) { "abandon" }, ""),
        "EvmAddress" to AccountType.EvmAddress("0x0000000000000000000000000000000000000000"),
        "EvmPrivateKey" to AccountType.EvmPrivateKey(BigInteger.ONE),
        "TronAddress" to AccountType.TronAddress("T0000"),
        "TronPrivateKey" to AccountType.TronPrivateKey(BigInteger.ONE),
        "SolanaAddress" to AccountType.SolanaAddress("sol"),
        "TonAddress" to AccountType.TonAddress("ton"),
        "StellarAddress" to AccountType.StellarAddress("stellar"),
        "StellarSecretKey" to AccountType.StellarSecretKey("secret"),
        "BitcoinAddress(Bitcoin)" to AccountType.BitcoinAddress(
            "addr",
            BlockchainType.Bitcoin,
            TokenType.Derived(TokenType.Derivation.Bip84)
        ),
        "MoneroWatchAccount" to AccountType.MoneroWatchAccount("addr", "viewKey", 1L),
        "Passkey" to AccountType.Passkey("credential"),
    )

    private val drawableNames: Map<Int, String> by lazy {
        R.drawable::class.java.fields.associate { it.getInt(null) to it.name }
    }

    @Test
    fun chainBehaviorMatchesFixture() {
        val actual = dump()
        val fixtureFile = File("src/test/resources/chain-behavior-parity.txt")

        if (System.getProperty("updateParityFixture") == "true") {
            fixtureFile.parentFile.mkdirs()
            fixtureFile.writeText(actual)
            return
        }

        check(fixtureFile.exists()) {
            "Fixture missing. Generate it with -DupdateParityFixture=true"
        }
        assertEquals(fixtureFile.readText(), actual)
    }

    private fun dump(): String = buildString {
        appendLine("supported=" + BlockchainType.supported.joinToString(",") { it.uid })
        appendLine()

        for (chain in BlockchainType.supported) {
            appendLine("chain ${chain.uid}")
            appendLine("  title=${chain.title}")
            appendLine("  description=${Blockchain(chain, chain.title, null).description}")
            appendLine("  order=${chain.order}")
            appendLine("  isEvm=${chain.isEvm}")
            appendLine("  blockTime=${chain.blockTime}")
            appendLine("  uriScheme=${chain.uriScheme}")
            appendLine("  removeScheme=${chain.removeScheme}")
            appendLine("  chainId=${chain.chainId}")
            appendLine("  brandColor=${chain.brandColor?.value?.toString(16)}")
            appendLine("  feePriceScale=${chain.feePriceScale.name}")
            appendLine("  tokenIconPlaceholder=${drawableNames[chain.tokenIconPlaceholder]}")
            appendLine("  restoreSettingTypes=${chain.restoreSettingTypes.joinToString(",")}")
            appendLine("  nativeTokenQueries=${chain.nativeTokenQueries.joinToString(",") { it.id }}")
            appendLine("  defaultTokenQuery=${chain.defaultTokenQuery.id}")
            appendLine("  supportedNftTypes=${chain.supportedNftTypes.joinToString(",")}")

            val supportedTokens = sampleTokenTypes
                .filter { (_, tokenType) -> TokenQuery(chain, tokenType).isSupported }
                .joinToString(",") { it.first }
            appendLine("  supportedTokenTypes=$supportedTokens")

            appendLine("  protocolType[Native]=${TokenQuery(chain, TokenType.Native).protocolType}")
            appendLine("  protocolType[Eip20]=${TokenQuery(chain, sampleTokenTypes[1].second).protocolType}")

            val supportedAccounts = sampleAccountTypes
                .filter { (_, accountType) -> chain.supports(accountType) }
                .joinToString(",") { it.first }
            appendLine("  supports=$supportedAccounts")
            appendLine()
        }
    }
}
