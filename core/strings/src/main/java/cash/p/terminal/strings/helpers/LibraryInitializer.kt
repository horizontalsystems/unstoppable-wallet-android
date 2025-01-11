package cash.p.terminal.strings.helpers

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri

class LibraryInitializer : ContentProvider() {
    companion object {
        private lateinit var applicationContext: Context

        fun getApplicationContext(): Context {
            if (!Companion::applicationContext.isInitialized) {
                throw IllegalStateException("Context not initialized")
            }
            return applicationContext
        }
    }

    override fun onCreate(): Boolean {
        applicationContext = context?.applicationContext
            ?: throw IllegalStateException("Context cannot be null")
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0
}