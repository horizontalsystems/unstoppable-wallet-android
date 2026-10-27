package io.horizontalsystems.walletkit.modules.multiswap

import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.ServiceState
import io.horizontalsystems.walletkit.core.managers.PaidActionSettingsManager
import io.horizontalsystems.walletkit.ui.compose.TranslatableString
import io.horizontalsystems.subscriptions.core.SwapProtection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.math.BigDecimal

class SwapDefenseSystemService(
    private val paidActionSettingsManager: PaidActionSettingsManager
) : ServiceState<SwapDefenseSystemService.State>() {
    // Whether the current quote can be sent MEV-protected: the route allows it and the sell
    // chain has a protected sender. Unknown until the confirmation quote arrives.
    private var supportsMevProtection = false
    private var fiatPriceImpact: BigDecimal? = null
    private var fiatPriceImpactLevel: PriceImpactLevel? = null
    private var sendable = false

    private var systemMessage: DefenseSystemMessage? = null
    private var actionEnabled: Boolean = false

    fun start(coroutineScope: CoroutineScope) {
        coroutineScope.launch(Dispatchers.Default) {
            paidActionSettingsManager.enabledActionsFlow.collect {
                refreshMevProtectionEnabled()
                refreshSystemMessage()

                emitState()
            }
        }
    }

    private fun refreshMevProtectionEnabled() {
        actionEnabled = paidActionSettingsManager.isActionEnabled(SwapProtection)
    }

    fun setSwapProtectionEnabled(enabled: Boolean) {
        paidActionSettingsManager.setActionEnabled(SwapProtection, enabled)
    }

    override fun createState() = State(
        systemMessage = systemMessage,
        mevProtectionEnabled = supportsMevProtection && actionEnabled,
    )

    fun setSupportsMevProtection(supported: Boolean) {
        supportsMevProtection = supported

        refreshSystemMessage()

        emitState()
    }

    fun setPriceImpact(fiatPriceImpact: BigDecimal?, fiatPriceImpactLevel: PriceImpactLevel?) {
        this.fiatPriceImpact = fiatPriceImpact
        this.fiatPriceImpactLevel = fiatPriceImpactLevel

        refreshSystemMessage()

        emitState()
    }

    fun setSendable(sendable: Boolean) {
        this.sendable = sendable

        refreshSystemMessage()

        emitState()
    }

    private fun refreshSystemMessage() {
        systemMessage = null

        if (!sendable) return

        val fiatPriceImpact = fiatPriceImpact
        val fiatPriceImpactLevel = fiatPriceImpactLevel

        if (fiatPriceImpact != null && fiatPriceImpactLevel != null) {
            systemMessage = when (fiatPriceImpactLevel) {
                PriceImpactLevel.High -> {
                    DefenseSystemMessage(
                        level = DefenseAlertLevel.DANGER,
                        title = TranslatableString.ResString(R.string.SwapDefense_PriceImpact_High_Title),
                        body = TranslatableString.ResString(
                            R.string.SwapDefense_PriceImpact_High_Description,
                            fiatPriceImpact
                        ),
                    )
                }

                PriceImpactLevel.Forbidden -> {
                    DefenseSystemMessage(
                        level = DefenseAlertLevel.DANGER,
                        title = TranslatableString.ResString(R.string.SwapDefense_PriceImpact_Forbidden_Title),
                        body = TranslatableString.ResString(
                            R.string.SwapDefense_PriceImpact_Forbidden_Description,
                            fiatPriceImpact
                        ),
                    )
                }

                else -> null
            }
        }

        if (systemMessage == null && supportsMevProtection && actionEnabled) {
            systemMessage = DefenseSystemMessage(
                level = DefenseAlertLevel.SAFE,
                title = TranslatableString.ResString(R.string.SwapDefense_Safe_Title),
                body = TranslatableString.ResString(R.string.SwapDefense_Safe_Description),
            )
        }
    }

    data class State(
        val systemMessage: DefenseSystemMessage?,
        val mevProtectionEnabled: Boolean,
    )
}

data class DefenseSystemMessage(
    val level: DefenseAlertLevel,
    val title: TranslatableString,
    val body: TranslatableString,
)

enum class DefenseAlertLevel {
    WARNING,
    IDLE,
    DANGER,
    SAFE
}
