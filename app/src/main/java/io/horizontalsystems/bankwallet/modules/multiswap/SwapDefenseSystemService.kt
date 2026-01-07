package io.horizontalsystems.bankwallet.modules.multiswap

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.ServiceState
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.uiv3.components.message.DefenseAlertLevel
import io.horizontalsystems.bankwallet.uiv3.components.message.DefenseSystemMessage
import io.horizontalsystems.subscriptions.core.LossProtection
import io.horizontalsystems.subscriptions.core.UserSubscriptionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.math.BigDecimal

class SwapDefenseSystemService(
    private val supportsMevProtection: Boolean
) : ServiceState<SwapDefenseSystemService.State>() {
    private var fiatPriceImpact: BigDecimal? = null
    private var fiatPriceImpactLevel: PriceImpactLevel? = null

    private var systemMessage: DefenseSystemMessage? = null
    private var mevProtectionEnabled = false

    fun start(coroutineScope: CoroutineScope) {
        coroutineScope.launch(Dispatchers.Default) {
            UserSubscriptionManager.activeSubscriptionStateFlow.collect {
                mevProtectionEnabled = UserSubscriptionManager.isActionAllowed(LossProtection)

                refresh()

                emitState()
            }
        }
    }

    override fun createState() = State(
        systemMessage = systemMessage,
        mevProtectionEnabled = mevProtectionEnabled,
    )

    fun setPriceImpact(fiatPriceImpact: BigDecimal?, fiatPriceImpactLevel: PriceImpactLevel?) {
        this.fiatPriceImpact = fiatPriceImpact
        this.fiatPriceImpactLevel = fiatPriceImpactLevel

        refresh()

        emitState()
    }

    private fun refresh() {
        val fiatPriceImpact = fiatPriceImpact
        val fiatPriceImpactLevel = fiatPriceImpactLevel

        systemMessage = null

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
            systemMessage = if (mevProtectionEnabled) {
                DefenseSystemMessage(
                    level = DefenseAlertLevel.SAFE,
                    title = TranslatableString.ResString(R.string.SwapDefense_Safe_Title),
                    body = TranslatableString.ResString(R.string.SwapDefense_Safe_Description),
                )
            } else {
                DefenseSystemMessage(
                    level = DefenseAlertLevel.WARNING,
                    title = TranslatableString.ResString(R.string.SwapDefense_Attention_Title),
                    body = TranslatableString.ResString(R.string.SwapDefense_Attention_Description),
                    actionText = TranslatableString.ResString(R.string.Button_Activate),
                    requiredPaidAction = LossProtection,
                )
            }
        }
    }

    data class State(
        val systemMessage: DefenseSystemMessage?,
        val mevProtectionEnabled: Boolean
    )
}
