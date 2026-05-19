package cash.p.terminal.modules.multiswap

import cash.p.terminal.core.TestDispatcherProvider
import cash.p.terminal.modules.multiswap.providers.IMultiSwapProvider
import cash.p.terminal.wallet.MarketKitWrapper
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.entities.TokenType
import io.horizontalsystems.core.DispatcherProvider
import io.horizontalsystems.core.entities.Blockchain
import io.horizontalsystems.core.entities.BlockchainType
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class MultiSwapRouteResolverTest {

    private val marketKit = mockk<MarketKitWrapper>()
    private val dispatcherProvider = mockk<DispatcherProvider>()
    private val resolver = MultiSwapRouteResolver(marketKit, dispatcherProvider)

    private val bscBlockchain = Blockchain(BlockchainType.BinanceSmartChain, "BSC", null)
    private val tonBlockchain = Blockchain(BlockchainType.Ton, "TON", null)
    private val ethBlockchain = Blockchain(BlockchainType.Ethereum, "Ethereum", null)
    private val litecoinBlockchain = Blockchain(BlockchainType.Litecoin, "Litecoin", null)

    private val bnbNative = mockToken("bnb", bscBlockchain, TokenType.Native)
    private val tonNative = mockToken("ton", tonBlockchain, TokenType.Native)
    private val ethNative = mockToken("eth", ethBlockchain, TokenType.Native)
    private val litecoinPublic = mockToken(
        "ltc-public",
        litecoinBlockchain,
        TokenType.Derived(TokenType.Derivation.Bip84)
    )
    private val litecoinMweb = mockToken("ltc-mweb", litecoinBlockchain, TokenType.Mweb)

    private val pirateJetton = mockToken("pirate", tonBlockchain, TokenType.Jetton("addr1"))
    private val cosaBep20 = mockToken("cosa", bscBlockchain, TokenType.Eip20("addr2"))
    private val usdtErc20 = mockToken("usdt", ethBlockchain, TokenType.Eip20("addr3"))

    private fun mockToken(coinUid: String, blockchain: Blockchain, type: TokenType): Token =
        mockk {
            every { this@mockk.coin } returns mockk { every { uid } returns coinUid }
            every { this@mockk.blockchain } returns blockchain
            every { this@mockk.blockchainType } returns blockchain.type
            every { this@mockk.type } returns type
        }

    // --- Both native → empty (guarded by findRoute, but also by buildCandidateIntermediates) ---

    @Test
    fun buildCandidateIntermediates_bothNative_returnsEmpty() {
        // TON native → BNB native: both are native, no intermediate makes sense
        // (tokenIn is native → skip intermediateA, tokenOut is native → skip intermediateB)
        val result = resolver.buildCandidateIntermediates(tonNative, bnbNative)
        assertTrue(result.isEmpty())
    }

    // --- tokenIn is native → skip intermediateA, try intermediateB ---

    @Test
    fun buildCandidateIntermediates_tokenInNative_onlyTriesTokenOutBlockchain() {
        // TON (native) → COSA (BEP-20): skip TON intermediate, try BNB
        every { marketKit.nativeToken(BlockchainType.BinanceSmartChain) } returns bnbNative

        val result = resolver.buildCandidateIntermediates(tonNative, cosaBep20)
        assertEquals(listOf(bnbNative), result)
    }

    // --- tokenOut is native → skip intermediateB, try intermediateA ---

    @Test
    fun buildCandidateIntermediates_tokenOutNative_onlyTriesTokenInBlockchain() {
        // PIRATE (Jetton) → BNB (native): try TON intermediate, skip BNB
        every { marketKit.nativeToken(BlockchainType.Ton) } returns tonNative

        val result = resolver.buildCandidateIntermediates(pirateJetton, bnbNative)
        assertEquals(listOf(tonNative), result)
    }

    // --- Both non-native, different blockchains → two candidates ---

    @Test
    fun buildCandidateIntermediates_differentBlockchains_returnsTwoCandidates() {
        // PIRATE (Jetton/TON) → COSA (BEP-20/BSC): try TON and BNB
        every { marketKit.nativeToken(BlockchainType.Ton) } returns tonNative
        every { marketKit.nativeToken(BlockchainType.BinanceSmartChain) } returns bnbNative

        val result = resolver.buildCandidateIntermediates(pirateJetton, cosaBep20)
        assertEquals(listOf(tonNative, bnbNative), result)
    }

    // --- Both non-native, same blockchain → one candidate (deduplicated) ---

    @Test
    fun buildCandidateIntermediates_sameBlockchain_returnsOnlyOne() {
        // Two BEP-20 tokens on BSC: only BNB as intermediate
        val wdashBep20 = mockToken("wdash", bscBlockchain, TokenType.Eip20("addr4"))
        every { marketKit.nativeToken(BlockchainType.BinanceSmartChain) } returns bnbNative

        val result = resolver.buildCandidateIntermediates(cosaBep20, wdashBep20)
        assertEquals(listOf(bnbNative), result)
    }

    // --- intermediateA equals tokenOut → skip it ---

    @Test
    fun buildCandidateIntermediates_intermediateAEqualsTokenOut_skipsIt() {
        // USDT (ERC-20) → ETH (native): intermediateA = ETH = tokenOut, skip it
        // intermediateB skipped because tokenOut is native
        every { marketKit.nativeToken(BlockchainType.Ethereum) } returns ethNative

        val result = resolver.buildCandidateIntermediates(usdtErc20, ethNative)
        assertTrue(result.isEmpty())
    }

    // --- intermediateB equals tokenIn → skip it ---

    @Test
    fun buildCandidateIntermediates_intermediateBEqualsTokenIn_skipsIt() {
        // ETH (native) → USDT (ERC-20): tokenIn is native → skip intermediateA
        // intermediateB = ETH = tokenIn, skip it
        every { marketKit.nativeToken(BlockchainType.Ethereum) } returns ethNative

        val result = resolver.buildCandidateIntermediates(ethNative, usdtErc20)
        assertTrue(result.isEmpty())
    }

    // --- marketKit returns null → skip ---

    @Test
    fun buildCandidateIntermediates_marketKitReturnsNull_skips() {
        every { marketKit.nativeToken(BlockchainType.Ton) } returns null
        every { marketKit.nativeToken(BlockchainType.BinanceSmartChain) } returns null

        val result = resolver.buildCandidateIntermediates(pirateJetton, cosaBep20)
        assertTrue(result.isEmpty())
    }

    @Test
    fun buildCandidateIntermediates_oneNullOneValid_returnsValid() {
        every { marketKit.nativeToken(BlockchainType.Ton) } returns null
        every { marketKit.nativeToken(BlockchainType.BinanceSmartChain) } returns bnbNative

        val result = resolver.buildCandidateIntermediates(pirateJetton, cosaBep20)
        assertEquals(listOf(bnbNative), result)
    }

    @Test
    fun buildCandidateIntermediates_mwebInput_treatsMwebAsRouteNonNative() {
        every { marketKit.nativeToken(BlockchainType.Litecoin) } returns litecoinPublic
        every { marketKit.nativeToken(BlockchainType.Ethereum) } returns ethNative

        val result = resolver.buildCandidateIntermediates(litecoinMweb, usdtErc20)

        assertEquals(listOf(litecoinPublic, ethNative), result)
    }

    // --- findRoute fallback to second candidate ---

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun findRoute_firstCandidateNoQuotes_fallsBackToSecond() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val testScope = CoroutineScope(testDispatcher)
        val testResolver = MultiSwapRouteResolver(marketKit, TestDispatcherProvider(testDispatcher, testScope))

        every { marketKit.nativeToken(BlockchainType.Ton) } returns tonNative
        every { marketKit.nativeToken(BlockchainType.BinanceSmartChain) } returns bnbNative

        // Provider A supports the route but cannot quote the first leg.
        val providerA = mockk<IMultiSwapProvider>(relaxed = true) {
            coEvery { supports(pirateJetton, tonNative) } returns true
            coEvery { supports(tonNative, cosaBep20) } returns true
            coEvery { supports(pirateJetton, bnbNative) } returns false
            coEvery { supports(bnbNative, cosaBep20) } returns false
            coEvery { fetchQuote(pirateJetton, tonNative, any(), any()) } throws RuntimeException("no quote")
        }

        // Provider B is the fallback route with valid quotes for both legs.
        val providerB = mockk<IMultiSwapProvider>(relaxed = true) {
            coEvery { supports(pirateJetton, tonNative) } returns false
            coEvery { supports(tonNative, cosaBep20) } returns false
            coEvery { supports(pirateJetton, bnbNative) } returns true
            coEvery { supports(bnbNative, cosaBep20) } returns true
            coEvery { fetchQuote(pirateJetton, bnbNative, any(), any()) } returns mockk(relaxed = true) {
                every { amountOut } returns BigDecimal("10")
            }
            coEvery { fetchQuote(bnbNative, cosaBep20, any(), any()) } returns mockk(relaxed = true) {
                every { amountOut } returns BigDecimal("500")
            }
        }

        val route = testResolver.findRoute(
            providers = listOf(providerA, providerB),
            tokenIn = pirateJetton,
            tokenOut = cosaBep20,
            amountIn = BigDecimal("100"),
            settings = emptyMap(),
        )

        assertNotNull(route)
        assertEquals(bnbNative, route?.intermediateCoin)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun findRoute_twoBuildableCandidates_selectsBestFinalOutput() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val testScope = CoroutineScope(testDispatcher)
        val testResolver = MultiSwapRouteResolver(marketKit, TestDispatcherProvider(testDispatcher, testScope))

        every { marketKit.nativeToken(BlockchainType.Ton) } returns tonNative
        every { marketKit.nativeToken(BlockchainType.BinanceSmartChain) } returns bnbNative

        // TON route is buildable but produces a smaller final output.
        val providerA = mockk<IMultiSwapProvider>(relaxed = true) {
            coEvery { supports(pirateJetton, tonNative) } returns true
            coEvery { supports(tonNative, cosaBep20) } returns true
            coEvery { supports(pirateJetton, bnbNative) } returns false
            coEvery { supports(bnbNative, cosaBep20) } returns false
            coEvery { fetchQuote(pirateJetton, tonNative, any(), any()) } returns mockk(relaxed = true) {
                every { amountOut } returns BigDecimal("5")
            }
            coEvery { fetchQuote(tonNative, cosaBep20, any(), any()) } returns mockk(relaxed = true) {
                every { amountOut } returns BigDecimal("200")
            }
        }

        // BNB route is also buildable and should win by final output.
        val providerB = mockk<IMultiSwapProvider>(relaxed = true) {
            coEvery { supports(pirateJetton, tonNative) } returns false
            coEvery { supports(tonNative, cosaBep20) } returns false
            coEvery { supports(pirateJetton, bnbNative) } returns true
            coEvery { supports(bnbNative, cosaBep20) } returns true
            coEvery { fetchQuote(pirateJetton, bnbNative, any(), any()) } returns mockk(relaxed = true) {
                every { amountOut } returns BigDecimal("10")
            }
            coEvery { fetchQuote(bnbNative, cosaBep20, any(), any()) } returns mockk(relaxed = true) {
                every { amountOut } returns BigDecimal("500")
            }
        }

        val route = testResolver.findRoute(
            providers = listOf(providerA, providerB),
            tokenIn = pirateJetton,
            tokenOut = cosaBep20,
            amountIn = BigDecimal("100"),
            settings = emptyMap(),
        )

        assertNotNull(route)
        assertEquals(bnbNative, route?.intermediateCoin)
        assertEquals(BigDecimal("500"), route?.selectedLeg2Quote?.amountOut)
    }
}
