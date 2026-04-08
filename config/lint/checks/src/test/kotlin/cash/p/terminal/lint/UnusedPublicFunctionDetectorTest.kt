package cash.p.terminal.lint

import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test

class UnusedPublicFunctionDetectorTest {

    @Test
    fun unusedPublicFunction_flagged() {
        lint()
            .files(
                kotlin(
                    """
                    package test

                    class MyClass {
                        fun unusedFunction() { }
                    }
                    """
                ).indented()
            )
            .issues(UnusedPublicFunctionDetector.ISSUE)
            .run()
            .expectWarningCount(1)
    }

    @Test
    fun usedPublicFunction_notFlagged() {
        lint()
            .files(
                kotlin(
                    """
                    package test

                    class MyClass {
                        fun doWork() { }
                    }

                    class Caller {
                        fun execute() {
                            MyClass().doWork()
                            execute()
                        }
                    }
                    """
                ).indented()
            )
            .issues(UnusedPublicFunctionDetector.ISSUE)
            .run()
            .expectClean()
    }

    @Test
    fun privateFunction_notFlagged() {
        lint()
            .files(
                kotlin(
                    """
                    package test

                    class MyClass {
                        private fun helper() { }
                    }
                    """
                ).indented()
            )
            .issues(UnusedPublicFunctionDetector.ISSUE)
            .run()
            .expectClean()
    }
}
