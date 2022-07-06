package io.horizontalsystems.bankwallet.widgets

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.dataStoreFile
import androidx.glance.state.GlanceStateDefinition
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import java.io.File
import java.io.InputStream
import java.io.OutputStream

object MarketWidgetStateDefinition : GlanceStateDefinition<MarketWidgetState> {

    private fun Context.marketWidgetDataStoreFile(name: String): File = dataStoreFile("$name.uw")

    override suspend fun getDataStore(context: Context, fileKey: String): DataStore<MarketWidgetState> {
        return DataStoreFactory.create(serializer = MarketWidgetStateSerializer) {
            context.marketWidgetDataStoreFile(fileKey)
        }
    }

    override fun getLocation(context: Context, fileKey: String): File {
        return context.marketWidgetDataStoreFile(fileKey)
    }

    object MarketWidgetStateSerializer : Serializer<MarketWidgetState> {
        private val gson by lazy {
            GsonBuilder()
                .setLenient()
                .registerTypeAdapter(MarketWidgetType::class.java, MarketWidgetTypeAdapter())
                .create()
        }

        override val defaultValue = MarketWidgetState(loading = true)

        override suspend fun readFrom(input: InputStream): MarketWidgetState = try {
            val jsonString = input.readBytes().decodeToString()
            gson.fromJson(jsonString, MarketWidgetState::class.java)
        } catch (exception: JsonSyntaxException) {
            throw CorruptionException("Could not read data: ${exception.message}")
        }

        override suspend fun writeTo(t: MarketWidgetState, output: OutputStream) {
            output.use {
                it.write(
                    gson.toJson(t).encodeToByteArray()
                )
            }
        }
    }
}
