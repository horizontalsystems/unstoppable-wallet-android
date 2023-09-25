package io.horizontalsystems.bankwallet.core.tor.torutils

import android.os.Build
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipFile


object NativeLoader {

    private const val TAG = "TorNativeLoader"

    private fun loadFromZip(appSourceDir: File, libName: String, destLocalFile: File, arch: String): Boolean {

        var zipFile: ZipFile? = null
        var stream: InputStream? = null

        try {

            zipFile = ZipFile(appSourceDir)
            var entry = zipFile.getEntry("lib/$arch/$libName.so")

            if (entry == null) {
                entry = zipFile.getEntry("jni/$arch/$libName.so")

                if (entry == null)
                    throw Exception("Unable to find file in apk:lib/$arch/$libName")
            }
            //how we wrap this in another stream because the native .so is zipped itself
            stream = zipFile.getInputStream(entry)
            val out: OutputStream = FileOutputStream(destLocalFile)
            val buf = ByteArray(4096)
            var len: Int

            while (stream.read(buf).also { len = it } > 0) {
                Thread.yield()
                out.write(buf, 0, len)
            }

            out.close()
            destLocalFile.setReadable(true, false)
            destLocalFile.setExecutable(true, false)
            destLocalFile.setWritable(true)

            return true
        } catch (e: Exception) {
            Log.e(TAG, "error", e)

        } finally {

            if (stream != null) {
                try {
                    stream.close()
                } catch (e: Exception) {
                    Log.e(TAG, "error", e)
                }
            }

            if (zipFile != null) {
                try {
                    zipFile.close()
                } catch (e: Exception) {
                    Log.e(TAG, "error", e)
                }
            }
        }
        return false
    }

    fun loadNativeBinary(
        appNativeDir: File, appSourceDir: File,
        libName: String, destLocalFile: File?
    ): File? {

        try {

            val fileNativeBin = File(appNativeDir.path, "$libName.so")
            if (fileNativeBin.exists()) {
                if (fileNativeBin.canExecute())
                    return fileNativeBin
                else {
                    FileUtils.setExecutable(
                        fileNativeBin
                    )

                    if (fileNativeBin.canExecute())
                        return fileNativeBin
                }
            }
            var folder = Build.CPU_ABI
            val javaArch = System.getProperty("os.arch")

            if (javaArch != null && javaArch.contains("686")) {
                folder = "x86"
            }

            destLocalFile?.let {
                if (loadFromZip(appSourceDir, libName, destLocalFile, folder)) {
                    return destLocalFile
                }
            }

        } catch (e: Throwable) {
            Log.e(TAG, e.message, e)
        }

        return null
    }

}
