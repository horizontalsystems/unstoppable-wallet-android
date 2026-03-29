package cash.p.terminal.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtPackageDirective

class UnnecessaryFullyQualifiedNameRule(config: Config = Config.empty) : Rule(config) {

    override val issue = Issue(
        id = javaClass.simpleName,
        severity = Severity.Style,
        description = "Fully qualified type names should be imported and used by short name.",
        debt = Debt.FIVE_MINS
    )

    private val projectPackagePrefix = "cash.p.terminal."

    override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
        super.visitDotQualifiedExpression(expression)

        if (isInsideImportOrPackage(expression)) return
        if (expression.parent is KtDotQualifiedExpression) return

        val fullText = expression.text
        if (!fullText.startsWith(projectPackagePrefix)) return

        val afterPrefix = fullText.removePrefix(projectPackagePrefix)
        if (!afterPrefix.contains(Regex("[A-Z]"))) return

        report(
            CodeSmell(
                issue = issue,
                entity = Entity.from(expression),
                message = "Use an import instead of the fully qualified name: $fullText"
            )
        )
    }

    private fun isInsideImportOrPackage(expression: KtDotQualifiedExpression): Boolean {
        var parent = expression.parent
        while (parent != null) {
            if (parent is KtImportDirective || parent is KtPackageDirective) return true
            parent = parent.parent
        }
        return false
    }
}
