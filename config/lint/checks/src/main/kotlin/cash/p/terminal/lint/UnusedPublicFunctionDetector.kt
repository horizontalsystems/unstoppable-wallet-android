package cash.p.terminal.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.USimpleNameReferenceExpression
import java.util.EnumSet

/**
 * Pass 1: collects public non-override function qualified names.
 * Pass 2: collects call references. Then re-visits declarations and reports unreferenced ones.
 */
class UnusedPublicFunctionDetector : Detector(), SourceCodeScanner {

    private val declaredNames = mutableSetOf<String>()
    private val referencedNames = mutableSetOf<String>()
    private val referencedQualifiedNames = mutableSetOf<String>()
    private var secondPass = false

    override fun getApplicableUastTypes(): List<Class<out UElement>> = listOf(
        UMethod::class.java,
        UCallExpression::class.java,
        USimpleNameReferenceExpression::class.java,
    )

    override fun createUastHandler(context: JavaContext): UElementHandler {
        return object : UElementHandler() {
            override fun visitMethod(node: UMethod) {
                if (!secondPass) {
                    if (isPublicNonOverride(node) && !isExcluded(node)) {
                        val containingClass = node.containingClass ?: return
                        val qualifiedName = "${containingClass.qualifiedName}.${node.name}"
                        declaredNames.add(qualifiedName)
                    }
                    return
                }

                if (!isPublicNonOverride(node) || isExcluded(node)) return
                val containingClass = node.containingClass ?: return
                val qualifiedName = "${containingClass.qualifiedName}.${node.name}"

                if (qualifiedName !in referencedQualifiedNames && node.name !in referencedNames) {
                    context.report(
                        ISSUE,
                        node,
                        context.getNameLocation(node),
                        "Public function `${node.name}` appears to be unused within the module",
                    )
                }
            }

            override fun visitCallExpression(node: UCallExpression) {
                val methodName = node.methodName ?: return
                referencedNames.add(methodName)

                val resolved = node.resolve()
                if (resolved != null) {
                    val qualifiedName = "${resolved.containingClass?.qualifiedName}.${resolved.name}"
                    referencedQualifiedNames.add(qualifiedName)
                }
            }

            override fun visitSimpleNameReferenceExpression(node: USimpleNameReferenceExpression) {
                referencedNames.add(node.identifier)
            }
        }
    }

    override fun afterCheckEachProject(context: com.android.tools.lint.detector.api.Context) {
        if (!secondPass && declaredNames.isNotEmpty()) {
            secondPass = true
            context.driver.requestRepeat(this, Scope.JAVA_FILE_SCOPE)
            return
        }

        declaredNames.clear()
        referencedNames.clear()
        referencedQualifiedNames.clear()
        secondPass = false
    }

    private fun isPublicNonOverride(node: UMethod): Boolean {
        if (node.isConstructor) return false

        val psi = node.javaPsi
        val isOverride = psi.hasAnnotation("java.lang.Override") || node.findSuperMethods().isNotEmpty()
        val isPrivateOrProtected = psi.hasModifierProperty("private") || psi.hasModifierProperty("protected")
        val containingClass = node.containingClass

        return !isOverride && !isPrivateOrProtected && containingClass != null && !containingClass.isInterface
    }

    private fun isExcluded(node: UMethod): Boolean {
        val name = node.name
        if (name.startsWith("get") || name.startsWith("set") || name.startsWith("is")) return true
        return node.javaPsi.annotations.any { it.qualifiedName in EXCLUDED_ANNOTATIONS }
    }

    companion object {
        private val EXCLUDED_ANNOTATIONS = setOf(
            "org.junit.Test",
            "org.junit.Before",
            "org.junit.After",
            "org.junit.BeforeClass",
            "org.junit.AfterClass",
            "androidx.compose.runtime.Composable",
            "androidx.compose.ui.tooling.preview.Preview",
            "dagger.Provides",
            "dagger.Binds",
        )

        val ISSUE = Issue.create(
            id = "UnusedPublicFunction",
            briefDescription = "Unused public function",
            explanation = "This public function does not appear to be called anywhere in the module. " +
                "Consider removing it or making it private/internal.",
            category = Category.PERFORMANCE,
            priority = 4,
            severity = Severity.WARNING,
            implementation = Implementation(
                UnusedPublicFunctionDetector::class.java,
                EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES),
            ),
        )
    }
}
