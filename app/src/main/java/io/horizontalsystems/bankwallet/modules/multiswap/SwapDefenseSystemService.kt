package io.horizontalsystems.bankwallet.modules.multiswap

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.ServiceState
import io.horizontalsystems.bankwallet.core.managers.PaidActionSettingsManager
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.uiv3.components.message.DefenseAlertLevel
import io.horizontalsystems.bankwallet.uiv3.components.message.DefenseSystemMessage
import io.horizontalsystems.subscriptions.core.SwapProtection
import io.horizontalsystems.subscriptions.core.UserSubscriptionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.math.BigDecimal

class SwapDefenseSystemService(
    private val supportsMevProtection: Boolean,
    private val paidActionSettingsManager: PaidActionSettingsManager
) : ServiceState<SwapDefenseSystemService.State>() {
    private var fiatPriceImpact: BigDecimal? = null
    private var fiatPriceImpactLevel: PriceImpactLevel? = null
    private var sendable = false

    private var systemMessage: DefenseSystemMessage? = null
    private var actionAllowed: Boolean = false
    private var actionEnabled: Boolean = false

    fun start(coroutineScope: CoroutineScope) {
        coroutineScope.launch(Dispatchers.Default) {
            UserSubscriptionManager.activeSubscriptionStateFlow.collect {
                refreshMevProtectionEnabled()
                refreshSystemMessage()

                emitState()
            }
        }

        coroutineScope.launch(Dispatchers.Default) {
            paidActionSettingsManager.disabledActionsFlow.collect {
                refreshMevProtectionEnabled()
                refreshSystemMessage()

                emitState()
            }
        }
    }

    private fun refreshMevProtectionEnabled() {
        actionAllowed = UserSubscriptionManager.isActionAllowed(SwapProtection)
        actionEnabled = paidActionSettingsManager.isActionEnabled(SwapProtection)
    }

    override fun createState() = State(
        systemMessage = systemMessage,
        mevProtectionEnabled = actionAllowed && actionEnabled,
    )

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

        if (systemMessage == null && supportsMevProtection) {
            systemMessage = when {
                !actionAllowed -> DefenseSystemMessage(
                    level = DefenseAlertLevel.WARNING,
                    title = TranslatableString.ResString(R.string.SwapDefense_Attention_Title),
                    body = TranslatableString.ResString(R.string.SwapDefense_Attention_Description),
                    actionText = TranslatableString.ResString(R.string.Button_Activate),
                    requiredPaidAction = SwapProtection
                )

                !actionEnabled -> null

                else -> DefenseSystemMessage(
                    level = DefenseAlertLevel.SAFE,
                    title = TranslatableString.ResString(R.string.SwapDefense_Safe_Title),
                    body = TranslatableString.ResString(R.string.SwapDefense_Safe_Description),
                )
            }
        }
    }

    data class State(
        val systemMessage: DefenseSystemMessage?,
        val mevProtectionEnabled: Boolean
    )
}
