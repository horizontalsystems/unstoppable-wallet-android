package io.horizontalsystems.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import com.lemonappdev.konsist.api.declaration.KoImportDeclaration
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.Test

/**
 * The rules of AGENTS.md that can be read off the source code. A rule here
 * must hold for the whole project; a known exception is named in the rule
 * itself, never left failing.
 */
class ArchitectureTest {

    // Session worktrees under .claude are full copies of the project.
    private val project = Konsist.scopeFromProject()
        .slice { !it.modulePath.startsWith("/.claude/") }

    // Every source set that ships, the flavor and build-type ones included.
    private val production = project
        .slice { !TEST_SOURCE_SET.containsMatchIn(it.modulePath) }

    @Test
    fun `designkit imports no other module of the project`() {
        project
            .slice { it.modulePath.startsWith("/designkit/") }
            .imports
            .assertFalse {
                it.isIn("io.horizontalsystems") && !it.isIn("io.horizontalsystems.designkit")
            }
    }

    @Test
    fun `walletkit and its chain modules do not import the app`() {
        project
            .slice { it.modulePath.startsWith("/walletkit") }
            .imports
            .assertFalse { it.isIn("io.horizontalsystems.bankwallet") }
    }

    @Test
    fun `RxJava is not imported`() {
        project
            // Installs the RxJava error handler that the blockchain kits rely on.
            .slice { !it.modulePath.endsWith("/walletkit/core/App.kt") }
            .imports
            .assertFalse { it.isIn("io.reactivex") }
    }

    @Test
    fun `GlobalScope is not used`() {
        production.files.assertFalse { GLOBAL_SCOPE in it.code }
    }

    @Test
    fun `classes extending ViewModelUiState are named ViewModel`() {
        production
            .classes()
            .filter { declaration ->
                // A parent's name is written as in the source: with its type
                // arguments, and with its package when fully qualified
                declaration.parents().any {
                    it.name.substringBefore('<').substringAfterLast('.') == "ViewModelUiState"
                }
            }
            .assertTrue { it.name.endsWith("ViewModel") }
    }

    @Test
    fun `test tags are declared in TestTags, not written inline`() {
        production.files.assertFalse { INLINE_TEST_TAG.containsMatchIn(it.code) }
    }

    private companion object {
        // src/test, src/androidTest and their variants, such as src/testDebug
        val TEST_SOURCE_SET = Regex("""/src/(test|androidTest)[A-Za-z]*/""")
        val GLOBAL_SCOPE = Regex("""\bGlobalScope\b""")
        val INLINE_TEST_TAG = Regex("""\.testTag\s*\(\s*(tag\s*=\s*)?"""")

        /** The path from the project root, with `/` on every host. */
        val KoFileDeclaration.modulePath: String
            get() = projectPath.replace('\\', '/')

        /**
         * The file's text without comments and without the text of string
         * literals, either of which may name what a rule bans.
         */
        val KoFileDeclaration.code: String
            get() = text.withoutCommentsAndLiteralText()

        /** A wildcard import has the package itself as its name, with no `.*`. */
        fun KoImportDeclaration.isIn(packageName: String) =
            name == packageName || name.startsWith("$packageName.")
    }
}
