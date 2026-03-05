package cash.p.terminal.modules.restorelocal

import cash.p.terminal.core.IAccountFactory
import cash.p.terminal.core.TestDispatcherProvider
import cash.p.terminal.modules.backuplocal.BackupLocalModule
import cash.p.terminal.modules.backuplocal.fullbackup.BackupProvider
import cash.p.terminal.modules.backuplocal.fullbackup.BackupViewItemFactory
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class RestoreLocalViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val dispatcherProvider = TestDispatcherProvider(dispatcher, CoroutineScope(dispatcher))
    private val accountFactory = mockk<IAccountFactory> {
        every { getNextAccountName() } returns "Wallet 1"
        every { getUniqueName(any()) } answers { firstArg() }
    }
    private val backupProvider = mockk<BackupProvider>(relaxed = true) {
        every { parseV3Backup(any()) } returns null
    }
    private val backupViewItemFactory = mockk<BackupViewItemFactory>(relaxed = true)

    private fun createTempFile(content: ByteArray): File {
        val file = File.createTempFile("test_backup", ".tmp")
        file.writeBytes(content)
        return file
    }

    private fun createViewModel(filePath: String?, fileName: String? = null) =
        RestoreLocalViewModel(
            backupFilePath = filePath,
            accountFactory = accountFactory,
            backupProvider = backupProvider,
            backupViewItemFactory = backupViewItemFactory,
            dispatcherProvider = dispatcherProvider,
            fileName = fileName
        )

    @Test
    fun init_binaryBackupFile_setsBackupV4Binary() = runTest(dispatcher) {
        val binaryData = ByteArray(10).apply {
            System.arraycopy(BackupLocalModule.BackupV4Binary.MAGIC, 0, this, 0, 4)
            this[4] = BackupLocalModule.BackupV4Binary.VERSION
        }
        val file = createTempFile(binaryData)

        val viewModel = createViewModel(file.absolutePath)
        advanceUntilIdle()

        assertNull(viewModel.uiState.parseError)
    }

    @Test
    fun init_jsonBackupFile_parsesAsJson() = runTest(dispatcher) {
        val json = """{"version":1}"""
        val file = createTempFile(json.toByteArray(Charsets.UTF_8))

        val viewModel = createViewModel(file.absolutePath)
        advanceUntilIdle()

        assertNull(viewModel.uiState.parseError)
    }

    @Test
    fun init_validFile_deletesFileAfterReading() = runTest(dispatcher) {
        val file = createTempFile("test".toByteArray())
        assertTrue(file.exists())

        createViewModel(file.absolutePath)
        advanceUntilIdle()

        assertTrue(!file.exists())
    }

    @Test
    fun init_missingFile_setsParseError() = runTest(dispatcher) {
        val viewModel = createViewModel("/tmp/nonexistent_backup_${System.nanoTime()}.tmp")
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.parseError)
    }

    @Test
    fun init_nullFilePath_noError() = runTest(dispatcher) {
        val viewModel = createViewModel(null)
        advanceUntilIdle()

        assertNull(viewModel.uiState.parseError)
    }

    @Test
    fun onImportClick_singleWalletJson_restoresAsSingleWallet() = runTest(dispatcher) {
        val json = requireNotNull(requireNotNull(javaClass.classLoader).getResource("backup/wallet_backup_v2_sample.json")).readText()
        val file = createTempFile(json.toByteArray(Charsets.UTF_8))

        val mockAccountType = mockk<cash.p.terminal.wallet.AccountType>()
        coEvery { backupProvider.accountType(any(), any()) } returns mockAccountType

        val viewModel = createViewModel(file.absolutePath)
        advanceUntilIdle()

        viewModel.onChangePassphrase("1")
        viewModel.onImportClick()
        advanceUntilIdle()

        // Should restore as single wallet, not as full backup
        assertNull("parseError should be null: ${viewModel.uiState.parseError}", viewModel.uiState.parseError)
        assertTrue("expected restored=true", viewModel.uiState.restored)
        assertFalse("expected showBackupItems=false", viewModel.uiState.showBackupItems)
        coVerify { backupProvider.restoreSingleWalletBackup(mockAccountType, any(), any()) }
    }

    @Test
    fun onImportClick_fullBackupJson_showsBackupItems() = runTest(dispatcher) {
        val json = requireNotNull(requireNotNull(javaClass.classLoader).getResource("backup/full_backup_v2_sample.json")).readText()
        val file = createTempFile(json.toByteArray(Charsets.UTF_8))

        val decryptedFullBackup = mockk<cash.p.terminal.modules.backuplocal.fullbackup.DecryptedFullBackup>()
        coEvery { backupProvider.decryptedFullBackup(any(), any()) } returns decryptedFullBackup
        coEvery { backupProvider.fullBackupItems(decryptedFullBackup) } returns mockk(relaxed = true)
        every { backupViewItemFactory.backupViewItems(any()) } returns Pair(emptyList(), emptyList())

        val viewModel = createViewModel(file.absolutePath)
        advanceUntilIdle()

        viewModel.onChangePassphrase("1")
        viewModel.onImportClick()
        advanceUntilIdle()

        // Should show backup items screen, not restore as single wallet
        assertNull("parseError should be null: ${viewModel.uiState.parseError}", viewModel.uiState.parseError)
        assertTrue("expected showBackupItems=true", viewModel.uiState.showBackupItems)
        assertFalse("expected restored=false", viewModel.uiState.restored)
        coVerify(exactly = 0) { backupProvider.restoreSingleWalletBackup(any(), any(), any()) }
    }
}
