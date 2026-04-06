package cash.p.terminal.modules.multiswap.providers

import cash.p.terminal.R
import cash.p.terminal.core.HSCaution
import cash.p.terminal.core.providers.AppConfigProvider
import cash.p.terminal.modules.multiswap.ISwapFinalQuote
import cash.p.terminal.modules.multiswap.ISwapQuote
import cash.p.terminal.modules.multiswap.StonFiGasParams
import cash.p.terminal.modules.multiswap.StonFiSwapData
import cash.p.terminal.modules.multiswap.SwapQuoteStonFi
import cash.p.terminal.modules.multiswap.sendtransaction.SendTransactionData
import cash.p.terminal.modules.multiswap.sendtransaction.SendTransactionSettings
import cash.p.terminal.modules.multiswap.settings.SwapSettingRecipient
import cash.p.terminal.modules.multiswap.settings.SwapSettingSlippage
import cash.p.terminal.modules.multiswap.ui.DataField
import cash.p.terminal.modules.multiswap.ui.DataFieldRecipient
import cash.p.terminal.modules.multiswap.ui.DataFieldSlippage
import cash.p.terminal.network.stonfi.domain.entity.SimulateSwap
import cash.p.terminal.network.stonfi.domain.repository.StonFiRepository
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.entities.TokenType
import cash.p.terminal.wallet.useCases.WalletUseCase
import com.tonapps.blockchain.ton.extensions.toByteArray
import io.horizontalsystems.core.entities.BlockchainType
import io.ktor.util.encodeBase64
import org.ton.block.AddrStd
import timber.log.Timber
import java.math.BigDecimal
import java.math.BigInteger

class StonFiProvider(
    private val stonFiRepository: StonFiRepository,
    override val walletUseCase: WalletUseCase,
) : IMultiSwapProvider {
    override val id = "stonfi"
    override val title = "STON.fi"
    override val icon = R.drawable.ic_ston_fi

    override val mevProtectionAvailable: Boolean = false
    // TON native token address
    companion object {
        private const val TON_NATIVE_ADDRESS = "EQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAM9c"
        private const val REF_FEE_BPS: Int = 10 // 0.1%
        private val REF_ADDRESS_TON = AppConfigProvider.donateAddresses[BlockchainType.Ton]
        private val SLIPPAGE = BigDecimal("0.5") // 0.5%
    }

    override suspend fun supports(token: Token): Boolean {
        if (token.blockchainType != BlockchainType.Ton) {
            return false
        }

        return try {
            val tokenAddress = getTokenAddress(token)
            stonFiRepository.getAssetByAddress(tokenAddress) != null
        } catch (e: Exception) {
            Timber.d(e, "StonFiProvider: failed to get asset for token ${token.coin.code}")
            false
        }
    }

    override suspend fun fetchQuote(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
        settings: Map<String, Any?>
    ): ISwapQuote {
        val settingRecipient = SwapSettingRecipient(settings, tokenOut)
        val settingSlippage =
            SwapSettingSlippage(settings, SLIPPAGE)

        val offerAddress = getTokenAddress(tokenIn)
        val askAddress = getTokenAddress(tokenOut)
        val units = amountIn.movePointRight(tokenIn.decimals).toBigInteger().toString()
        val referralAddressTon = REF_ADDRESS_TON
        val referralFeeBps = referralAddressTon?.let { REF_FEE_BPS }

        val simulation = simulateSwapWithFallback(
            offerAddress = offerAddress,
            askAddress = askAddress,
            units = units,
            slippageTolerance = settingSlippage.valueOrDefault(),
            poolAddress = null,
            referralAddress = referralAddressTon,
            referralFeeBps = referralFeeBps,
            preferredVersions = listOf(2, 1)
        )

        val response = simulation.swap
        val dexVersionUsed = simulation.dexVersion

        val amountOut = BigDecimal(response.askUnits).movePointLeft(tokenOut.decimals)
        val priceImpact = BigDecimal(response.priceImpact)

        val swapData = StonFiSwapData(
            offerAddress = response.offerAddress,
            askAddress = response.askAddress,
            offerJettonWallet = response.offerJettonWallet,
            askJettonWallet = response.askJettonWallet,
            routerAddress = response.routerAddress,
            poolAddress = response.poolAddress,
            offerUnits = response.offerUnits,
            askUnits = response.askUnits,
            slippageTolerance = response.slippageTolerance,
            minAskUnits = response.minAskUnits,
            swapRate = response.swapRate,
            priceImpact = response.priceImpact,
            feeAddress = response.feeAddress,
            feeUnits = response.feeUnits,
            feePercent = response.feePercent,
            gasParams = StonFiGasParams(
                forwardGas = response.gasParams.forwardGas,
                estimatedGasConsumption = response.gasParams.estimatedGasConsumption,
                gasBudget = response.gasParams.gasBudget
            ),
            dexVersion = dexVersionUsed
        )

        val fields = buildList {
            settingRecipient.value?.let { add(DataFieldRecipient(it)) }
            settingSlippage.value?.let { add(DataFieldSlippage(it)) }
        }

        return SwapQuoteStonFi(
            amountOut = amountOut,
            priceImpact = priceImpact,
            fields = fields,
            settings = listOf(settingRecipient, settingSlippage),
            tokenIn = tokenIn,
            tokenOut = tokenOut,
            amountIn = amountIn,
            actionRequired = getCreateTokenActionRequired(tokenIn, tokenOut),
            swapData = swapData
        )
    }

    override suspend fun fetchFinalQuote(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
        swapSettings: Map<String, Any?>,
        sendTransactionSettings: SendTransactionSettings?,
        swapQuote: ISwapQuote
    ): ISwapFinalQuote {
        check(swapQuote is SwapQuoteStonFi)

        val settingRecipient = SwapSettingRecipient(swapSettings, tokenOut)
        val settingSlippage = SwapSettingSlippage(swapSettings, SLIPPAGE)

        // Get fresh quote for final transaction
        val offerAddress = getTokenAddress(tokenIn)
        val askAddress = getTokenAddress(tokenOut)
        val units = amountIn.movePointRight(tokenIn.decimals).toBigInteger().toString()

        val preferredDexVersion = swapQuote.swapData.dexVersion
        val versionsToTry = if (preferredDexVersion == 2) listOf(2, 1) else listOf(1, 2)

        val finalSimulation = simulateSwapWithFallback(
            offerAddress = offerAddress,
            askAddress = askAddress,
            units = units,
            slippageTolerance = settingSlippage.valueOrDefault(),
            poolAddress = swapQuote.swapData.poolAddress.takeIf { it.isNotBlank() },
            referralAddress = REF_ADDRESS_TON,
            referralFeeBps = REF_FEE_BPS,
            preferredVersions = versionsToTry
        )

        val response = finalSimulation.swap

        val amountOut = BigDecimal(response.askUnits).movePointLeft(tokenOut.decimals)
        val minAmountOut = BigDecimal(response.minAskUnits).movePointLeft(tokenOut.decimals)

        val fields = buildList {
            settingRecipient.value?.let { add(DataFieldRecipient(it)) }
            settingSlippage.value?.let { add(DataFieldSlippage(it)) }
        }

        val addressFrom = walletUseCase.getReceiveAddress(tokenIn)
        val walletAddressTo = walletUseCase.getReceiveAddress(tokenOut)
        val receiverOwnerAddress = settingRecipient.value?.hex ?: walletAddressTo

        val routerInfo = stonFiRepository.getRouter(response.routerAddress)

        val ptonWalletAddress = when {
            // jetton -> ...
            tokenIn.type is TokenType.Jetton -> runCatching {
                stonFiRepository.getJettonAddress(
                    contractAddress = (tokenIn.type as TokenType.Jetton).address,
                    ownerAddress = receiverOwnerAddress
                )
            }.getOrNull()

            // ton -> ...
            else -> routerInfo.ptonWalletAddress
        }

        val destinationAddress = when {
            finalSimulation.dexVersion == 1 && tokenIn.type == TokenType.Native -> response.offerJettonWallet
                .takeUnless { it.isBlank() }
                ?: throw IllegalStateException("STON.fi v1: missing offer jetton wallet")
            else -> ptonWalletAddress
        }

        val tonTransferQueryId = System.currentTimeMillis()

        var gasBudget = response.gasParams.gasBudget

        val swapPayload = when {
            tokenIn.type is TokenType.Jetton -> {
                when (finalSimulation.dexVersion) {
                    1 -> {
                        gasBudget = response.offerUnits + response.gasParams.forwardGas + BigInteger("100000000") // 0.1 TON

                        buildJettonToTonPayloadV1(
                            router = AddrStd(response.routerAddress),
                            refundAddress = AddrStd(addressFrom),
                            routerPtonWallet = AddrStd(routerInfo.ptonWalletAddress),
                            amount = amountIn.movePointRight(tokenIn.decimals).toBigInteger(),
                            minOut = BigInteger(response.minAskUnits),
                            queryId = tonTransferQueryId,
                            referralAddress = REF_ADDRESS_TON?.let { AddrStd(it) },
                            forwardTonAmount = response.gasParams.forwardGas
                        )
                    }

                    2 -> {
                        buildJettonToTonPayloadV2(
                            amount = amountIn.movePointRight(tokenIn.decimals).toBigInteger(),
                            router = AddrStd(response.routerAddress),
                            ptonWallet = AddrStd(response.askJettonWallet),
                            refundAddress = AddrStd(addressFrom),
                            minOut = BigInteger(response.minAskUnits),
                            forwardGas = response.gasParams.forwardGas,
                            queryId = tonTransferQueryId,
                            refFee = REF_FEE_BPS,
                            referralAddress = REF_ADDRESS_TON?.let { AddrStd(it) }
                        )
                    }

                    else -> {
                        throw IllegalStateException("Unsupported dex version: ${finalSimulation.dexVersion}")
                    }
                }
            }

            else -> {

                when (finalSimulation.dexVersion) {
                    1 -> {
                        // amount to send
                        gasBudget = response.offerUnits + BigInteger("185000000") // 0.185 TON
                        val routerJettonWallet = response.askJettonWallet
                            .takeUnless { it.isNullOrBlank() }
                            ?: throw IllegalStateException("STON.fi v1: missing ask jetton wallet")
                        buildStonfiSwapTonToJettonTransferV1(
                            amount = amountIn.movePointRight(tokenIn.decimals).toBigInteger(),
                            routerAddress = AddrStd(response.routerAddress),
                            routerJettonWallet = AddrStd(routerJettonWallet),
                            receiver = AddrStd(receiverOwnerAddress),
                            minOut = BigInteger(response.minAskUnits),
                            referralAddress = REF_ADDRESS_TON?.let { AddrStd(it) },
                            forwardTonAmount = response.gasParams.forwardGas,
                            queryId = tonTransferQueryId,
                        )
                    }

                    2 -> {
                        buildStonfiSwapTonToJettonPayloadV2(
                            tonAmount = response.offerUnits,
                            tokenWallet = AddrStd(response.askJettonWallet),
                            refundAddress = AddrStd(addressFrom),
                            minOut = BigInteger(response.minAskUnits),
                            receiver = AddrStd(receiverOwnerAddress),
                            refFee = REF_FEE_BPS,
                            fwdGas = response.gasParams.forwardGas,
                            referralAddress = REF_ADDRESS_TON?.let { AddrStd(it) }
                        )
                    } else -> {
                        throw IllegalStateException("Unsupported dex version: ${finalSimulation.dexVersion}")
                    }
                }

            }
        }

        val sendTransactionData = SendTransactionData.TonSwap(
            offerUnits = response.offerUnits,
            forwardGas = response.gasParams.forwardGas,
            routerAddress = response.routerAddress,
            routerMasterAddress = routerInfo.ptonMasterAddress,
            destinationAddress = destinationAddress,
            queryId = tonTransferQueryId,
            slippage = settingSlippage.valueOrDefault(),
            payload = swapPayload.toByteArray().encodeBase64(),
            gasBudget = gasBudget
        )

        return SwapFinalQuoteTon(
            tokenIn = tokenIn,
            tokenOut = tokenOut,
            amountIn = amountIn,
            amountOut = amountOut,
            amountOutMin = minAmountOut,
            sendTransactionData = sendTransactionData,
            priceImpact = BigDecimal(response.priceImpact),
            fields = fields
        )
    }

    private suspend fun simulateSwapWithFallback(
        offerAddress: String,
        askAddress: String,
        units: String,
        slippageTolerance: BigDecimal,
        poolAddress: String?,
        referralAddress: String?,
        referralFeeBps: Int?,
        preferredVersions: List<Int>
    ): SimulationResult {
        val errors = mutableListOf<Throwable>()

        preferredVersions.forEachIndexed { index, dexVersion ->
            val poolForAttempt = if (index == 0) poolAddress else null

            val result = runCatching {
                stonFiRepository.simulateSwap(
                    offerAddress = offerAddress,
                    askAddress = askAddress,
                    units = units,
                    slippageTolerance = slippageTolerance,
                    poolAddress = poolForAttempt,
                    referralAddress = referralAddress,
                    referralFeeBps = referralFeeBps,
                    dexVersion = dexVersion
                )
            }.getOrElse {
                errors.add(it)
                null
            }

            if (result != null) {
                if (result.hasPositiveOutput()) {
                    return SimulationResult(result, dexVersion)
                }

                errors.add(IllegalStateException("STON.fi returned zero output for dex_version=$dexVersion"))
            }
        }

        val cause = errors.lastOrNull()
        throw cause ?: IllegalStateException("Failed to simulate swap on STON.fi")
    }

    private fun SimulateSwap.hasPositiveOutput(): Boolean {
        val askValue = parsePositiveBigDecimal(askUnits)
        val minAskValue = parseNonNegativeBigDecimal(minAskUnits)
        return askValue != null && minAskValue != null
    }

    private fun parsePositiveBigDecimal(value: String): BigDecimal? =
        runCatching { BigDecimal(value) }.getOrNull()?.takeIf { it.signum() > 0 }

    private fun parseNonNegativeBigDecimal(value: String): BigDecimal? =
        runCatching { BigDecimal(value) }.getOrNull()?.takeIf { it.signum() >= 0 }

    private data class SimulationResult(
        val swap: SimulateSwap,
        val dexVersion: Int
    )

    private fun getTokenAddress(token: Token): String {
        return when (val tokenType = token.type) {
            TokenType.Native -> TON_NATIVE_ADDRESS
            is TokenType.Jetton -> tokenType.address
            else -> throw IllegalArgumentException("Unsupported token type for STON.fi: $tokenType")
        }
    }
}

class SwapFinalQuoteTon(
    override val tokenIn: Token,
    override val tokenOut: Token,
    override val amountIn: BigDecimal,
    override val amountOut: BigDecimal,
    override val amountOutMin: BigDecimal?,
    override val sendTransactionData: SendTransactionData,
    override val priceImpact: BigDecimal?,
    override val fields: List<DataField>,
    override val cautions: List<HSCaution> = listOf()
) : ISwapFinalQuote
