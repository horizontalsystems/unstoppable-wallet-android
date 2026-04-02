#!/usr/bin/env kotlin

import java.io.File
import java.util.Base64
import kotlin.experimental.xor

/**
 * Usage:
 * 1) Add API keys to local.properties:
 *      api.key_name=RELEASE_OR_COMMON_KEY
 *      api.debug.key_name=DEBUG_KEY
 * 2) Run:
 *      kotlin ./tools/encode_secrets.kts
 */

val p = "pcash-public-password"
val z = File(".")
val x = File(z, "local.properties")
val y = File(z, "app/build.gradle")
val w = File(z, "./core/network/src/commonMain/kotlin/cash/p/terminal/network/data/EncodedSecrets.kt")

val b = grabId(y)
val c = twist(b)
val a = readSecrets(x)

if (a.isEmpty()) {
    println("⚠️  No secrets!")
    kotlin.system.exitProcess(0)
}

val d = a.mapValues { (_, values) -> values.map { scramble(it, p, c) } }

writeOut(d, w)

println("✅ Done: ${w.absolutePath}")
d.forEach { (k, v) ->
    println("  » $k: ${v.joinToString { it.take(16) + "..." }}")
}

fun grabId(f: File): String {
    val l = f.readText().lines()
    val t = l.firstOrNull { it.contains("applicationId") }
        ?: error("☠️ App ID missing")
    return t.split("\"")[1]
}

fun twist(s: String): String {
    val r = s.reversed()
    return r.drop(1).dropLast(1)
}

fun scramble(secret: String, key: String, salt: String): String {
    val comboKey = (key + salt).toByteArray()
    val input = secret.toByteArray()
    val encrypted = ByteArray(input.size) { i ->
        input[i] xor comboKey[i % comboKey.size]
    }
    return Base64.getEncoder().encodeToString(encrypted)
}

fun readSecrets(f: File): Map<String, List<String>> {
    if (!f.exists()) {
        println("✘ No local.properties found")
        return emptyMap()
    }

    val raw = f.readLines()
        .filter { it.startsWith("api.") && "=" in it }
        .map { it.split("=", limit = 2).let { (k, v) -> k.trim() to v.trim() } }
        .toMap()

    val keys = raw.keys
        .filter { !it.startsWith("api.debug.") }
        .map { it.removePrefix("api.") }

    return keys.associateWith { k ->
        val releaseValue = raw["api.$k"]
        val debugValue = raw["api.debug.$k"]
        val allValues = listOfNotNull(debugValue, releaseValue)
        allValues
    }.filterValues { it.isNotEmpty() }
}

fun writeOut(data: Map<String, List<String>>, target: File) {
    target.parentFile?.mkdirs()

    val result = buildString {
        appendLine("package cash.p.terminal.network.data")
        appendLine()
        appendLine("import org.koin.core.component.KoinComponent")
        appendLine("import org.koin.core.component.get")
        appendLine()
        appendLine("// generated file — do not edit manually")
        appendLine("object EncodedSecrets : KoinComponent {")
        appendLine("    private val decoder by lazy { get<Decoder>() }")
        appendLine()

        data.forEach { (k, values) ->
            val name = if (k.matches(Regex("^[a-zA-Z_][a-zA-Z0-9_]*$"))) k.uppercase() else "`$k`"
            appendLine("    val $name = decoder.decode(listOf(")
            values.forEachIndexed { i, v ->
                val comma = if (i != values.lastIndex) "," else ""
                appendLine("        \"$v\"$comma")
            }
            appendLine("    ))")
        }

        appendLine("}")
    }

    target.writeText(result)
}
