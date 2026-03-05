package cash.p.terminal.core

import io.horizontalsystems.core.CoreApp
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class WriteBackupToTempFileTest {

    @Before
    fun setUp() {
        val tmpDir = File(System.getProperty("java.io.tmpdir"))
        CoreApp.instance = mockk(relaxed = true) {
            every { cacheDir } returns tmpDir
        }
    }

    @Test
    fun writeBackupToTempFile_validBytes_createsFileWithExactContent() {
        val data = byteArrayOf(1, 2, 3, 4, 5, 0x7F, -1)
        val path = writeBackupToTempFile(data)
        try {
            val file = File(path)
            assertTrue(file.exists())
            assertArrayEquals(data, file.readBytes())
        } finally {
            File(path).delete()
        }
    }

    @Test
    fun writeBackupToTempFile_emptyBytes_createsEmptyFile() {
        val path = writeBackupToTempFile(byteArrayOf())
        try {
            val file = File(path)
            assertTrue(file.exists())
            assertEquals(0, file.length())
        } finally {
            File(path).delete()
        }
    }

    @Test
    fun writeBackupToTempFile_returnsAbsolutePath() {
        val path = writeBackupToTempFile(byteArrayOf(42))
        try {
            assertTrue(File(path).isAbsolute)
            assertTrue(File(path).exists())
        } finally {
            File(path).delete()
        }
    }
}
