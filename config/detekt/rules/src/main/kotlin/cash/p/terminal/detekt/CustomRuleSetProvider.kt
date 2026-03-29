package cash.p.terminal.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.RuleSet
import io.gitlab.arturbosch.detekt.api.RuleSetProvider

class CustomRuleSetProvider : RuleSetProvider {
    override val ruleSetId = "custom-rules"
    override fun instance(config: Config) = RuleSet(
        id = ruleSetId,
        rules = listOf(UnnecessaryFullyQualifiedNameRule(config))
    )
}
