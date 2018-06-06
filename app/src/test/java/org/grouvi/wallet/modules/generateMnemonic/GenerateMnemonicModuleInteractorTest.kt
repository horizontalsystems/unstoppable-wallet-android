package org.grouvi.wallet.modules.generateMnemonic

import com.nhaarman.mockito_kotlin.whenever
import org.bitcoinj.wallet.DeterministicSeed
import org.grouvi.wallet.modules.confirmMnemonic.ConfirmMnemonicModule
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class GenerateMnemonicModuleInteractorTest {

    private var interactor = GenerateMnemonicModuleInteractor()
    private var seedGenerator = mock(GenerateMnemonicModule.ISeedGenerator::class.java)
    private var delegate = mock(GenerateMnemonicModule.IInteractorDelegate::class.java)
    private var walletDataProvider = mock(ConfirmMnemonicModule.IWalletDataProvider::class.java)

    @Before
    fun before() {
        interactor.seedGenerator = seedGenerator
        interactor.delegate = delegate
        interactor.walletDataProvider = walletDataProvider
    }


    @Test
    fun generateSeed() {
        val deterministicSeed = mock(DeterministicSeed::class.java)
        val words = listOf("one", "two", "etc")

        whenever(seedGenerator.generateSeed("")).thenReturn(deterministicSeed)
        whenever(deterministicSeed.mnemonicCode).thenReturn(words)

        interactor.generateMnemonic()

        verify(delegate).didGenerateMnemonic(words)
        verify(walletDataProvider).mnemonicWords = words
    }


//    @Test
//    fun createWallet_createMasterKey() {
//        val deterministicSeed = mock(DeterministicSeed::class.java)
//
//        whenever(seedGenerator.generateSeed("")).thenReturn(deterministicSeed)
//
//        interactor.generateMnemonic()
//
//        verify(hdKeyFactory).createKeyFromSeed(deterministicSeed)
//    }
//
//    @Test
//    fun createWallet_createBIP44RootKey() {
//        val deterministicSeed = mock(DeterministicSeed::class.java)
//        val masterKey = mock(DeterministicKey::class.java)
//
//        whenever(seedGenerator.generateSeed("")).thenReturn(deterministicSeed)
//        whenever(hdKeyFactory.createKeyFromSeed(deterministicSeed)).thenReturn(masterKey)
//
//        interactor.generateMnemonic()
//
//        verify(hdKeyFactory).deriveChildKeyHardened(masterKey, 44)
//    }
}