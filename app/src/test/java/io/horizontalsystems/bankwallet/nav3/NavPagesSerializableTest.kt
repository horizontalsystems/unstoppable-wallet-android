package io.horizontalsystems.bankwallet.nav3

import io.github.classgraph.ClassGraph
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializerOrNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The nav backstack is persisted in onSaveInstanceState, so every HSPage must
 * resolve a kotlinx serializer at runtime — a page that doesn't crashes the app
 * the moment it is backgrounded with that page on the stack. This scans all
 * modules on the app classpath and verifies the exact lookup NavKeySerializer
 * performs, which also catches serializable pages with unserializable
 * property types.
 */
class NavPagesSerializableTest {

    @OptIn(InternalSerializationApi::class)
    @Test
    fun allNavPagesHaveSerializers() {
        val problems = mutableListOf<String>()

        ClassGraph()
            .enableClassInfo()
            .acceptPackages("io.horizontalsystems")
            .scan()
            .use { scan ->
                val subclasses = scan.getSubclasses("io.horizontalsystems.walletkit.modules.nav3.HSPage")
                assertTrue("HSPage subclass scan found nothing — scan setup is broken", subclasses.isNotEmpty())

                for (info in subclasses) {
                    if (info.isAbstract) continue
                    val problem = try {
                        val serializer = info.loadClass().kotlin.serializerOrNull()
                        if (serializer == null) {
                            "missing @Serializable"
                        } else {
                            serializer.descriptor // force child serializer resolution
                            null
                        }
                    } catch (e: Throwable) {
                        e.message ?: e.javaClass.simpleName
                    }
                    if (problem != null) {
                        problems.add("${info.name}: $problem")
                    }
                }
            }

        assertTrue(
            "Nav pages that cannot be serialized (would crash on backgrounding):\n" +
                problems.joinToString("\n"),
            problems.isEmpty()
        )
    }
}
