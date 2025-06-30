package cash.p.terminal.domain.usecase

import cash.p.terminal.core.tryOrNull
import io.horizontalsystems.core.CoreApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale

class GetReleaseNotesUseCase {

    private companion object {
        const val CHANGELOG_FOLDER = "common"
        const val CHANGELOG_PREFIX = "changelog_"
        const val CHANGELOG_SUFFIX = ".md"
        const val FALLBACK_LANGUAGE = "en"
    }

    suspend operator fun invoke(): String = withContext(Dispatchers.IO) {
        val currentLanguage = getCurrentLanguage()
        readChangelogFromAssets(currentLanguage)
    }

    private fun readChangelogFromAssets(language: String): String {
        val availableChangelogs = getAvailableChangelogFiles()

        val primaryFile = findChangelogFile(availableChangelogs, language)
        if (primaryFile != null) {
            return tryOrNull { readFileFromAssets("$CHANGELOG_FOLDER/$primaryFile") } ?: ""
        }

        if (language != FALLBACK_LANGUAGE) {
            val fallbackFile = findChangelogFile(availableChangelogs, FALLBACK_LANGUAGE)
            if (fallbackFile != null) {
                return tryOrNull { readFileFromAssets("$CHANGELOG_FOLDER/$fallbackFile") } ?: ""
            }
        }

        return if (availableChangelogs.isNotEmpty()) {
            tryOrNull { readFileFromAssets("$CHANGELOG_FOLDER/${availableChangelogs.first()}") } ?: ""
        } else {
            ""
        }
    }

    private fun getAvailableChangelogFiles(): List<String> {
        return try {
            val commonFiles = CoreApp.instance.assets.list(CHANGELOG_FOLDER) ?: emptyArray()

            commonFiles.filter { fileName ->
                val lowerFileName = fileName.lowercase()
                lowerFileName.startsWith(CHANGELOG_PREFIX) && lowerFileName.endsWith(CHANGELOG_SUFFIX)
            }.sortedBy { it.lowercase() }

        } catch (e: IOException) {
            emptyList()
        }
    }

    private fun findChangelogFile(availableFiles: List<String>, language: String): String? {
        val targetPattern = "${CHANGELOG_PREFIX}${language}${CHANGELOG_SUFFIX}"

        return availableFiles.find { fileName ->
            fileName.lowercase() == targetPattern
        }
    }

    private fun readFileFromAssets(fileName: String): String {
        return CoreApp.instance.assets.open(fileName).use { inputStream ->
            inputStream.bufferedReader().use { reader ->
                reader.readText()
            }
        }
    }

    private fun getCurrentLanguage(): String {
        return Locale.getDefault().language
    }
}