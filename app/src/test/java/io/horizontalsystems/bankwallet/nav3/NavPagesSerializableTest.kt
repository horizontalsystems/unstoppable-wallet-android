package io.horizontalsystems.bankwallet.nav3

import io.github.classgraph.ClassGraph
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.elementDescriptors
import kotlinx.serialization.serializerOrNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The nav backstack is persisted in onSaveInstanceState, so every HSPage must
 * resolve a kotlinx serializer at runtime — a page that doesn't crashes the app
 * the moment it is backgrounded with that page on the stack. This scans all
 * modules on the app classpath and verifies the exact lookup NavKeySerializer
 * performs, which also catches serializable pages with unserializable
 * property types, and flags properties whose serializer needs a
 * SerializersModule (e.g. a bare `HSPage` field), since the backstack is
 * saved without one.
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
                            unresolvableAtRuntime(serializer.descriptor)
                                ?.let { "property needs a SerializersModule: $it" }
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

    // An open polymorphic serializer resolves its descriptor fine but throws on encode, as no
    // SerializersModule is configured for the backstack. Sealed ones carry their subclasses.
    @OptIn(ExperimentalSerializationApi::class)
    private fun unresolvableAtRuntime(
        descriptor: SerialDescriptor,
        visited: MutableSet<String> = mutableSetOf(),
    ): String? {
        if (!visited.add(descriptor.serialName)) return null
        if (descriptor.kind == PolymorphicKind.OPEN) {
            return descriptor.serialName
        }
        return descriptor.elementDescriptors.firstNotNullOfOrNull { unresolvableAtRuntime(it, visited) }
    }
}
