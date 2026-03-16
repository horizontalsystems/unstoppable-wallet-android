package cash.p.terminal.modules.address

import cash.p.terminal.core.utils.AddressUriParser
import cash.p.terminal.core.utils.AddressUriResult
import cash.p.terminal.entities.Address
import cash.p.terminal.ui_compose.entities.DataState
import io.horizontalsystems.core.entities.BlockchainType
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
class FullAddressParserUseCaseTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(dispatcher)

    private val addressUriParser = mockk<AddressUriParser>()
    private val addressParserChain = mockk<AddressParserChain>()
    private val handler = mockk<IAddressHandler>()

    private val validAddress = "0xA94c267404479efEb91A0b504B883227fA6C23d6"
    private val parsedAddress = Address(validAddress, blockchainType = BlockchainType.Ethereum)

    private lateinit var useCase: FullAddressParserUseCase

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)

        every { addressUriParser.parse(any()) } returns AddressUriResult.NoUri
        every { addressParserChain.supportedHandler(validAddress) } returns handler
        every { handler.parseAddress(validAddress) } returns parsedAddress

        useCase = FullAddressParserUseCase(
            blockchainType = BlockchainType.Ethereum,
            addressUriParser = addressUriParser,
            addressParserChain = addressParserChain,
            coroutineScope = testScope,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun awaitCondition(message: String, condition: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(1)

        while (!condition()) {
            if (System.nanoTime() >= deadline) {
                throw AssertionError(message)
            }

            Thread.sleep(10)
        }
    }

    // =========================================================================
    // Core bug: parseText must update valueFlow immediately, not from background
    // =========================================================================

    @Test
    fun parseText_updatesValueFlowImmediately() = testScope.runTest {
        useCase.parseText(validAddress)

        assertEquals(validAddress, useCase.valueFlow.value)
    }

    @Test
    fun parseText_preservesExactUserInput_noTrimming() = testScope.runTest {
        val addressWithSpaces = "  $validAddress  "

        useCase.parseText(addressWithSpaces)

        assertEquals(addressWithSpaces, useCase.valueFlow.value)
    }

    @Test
    fun parseText_spacesAroundAddress_validatesOnTrimmed() = testScope.runTest {
        val addressWithSpaces = "  $validAddress  "

        useCase.parseText(addressWithSpaces)
        advanceUntilIdle()
        awaitCondition("Expected success for address with spaces") {
            useCase.inputState.value is DataState.Success
        }

        // UI shows exact user input (with spaces)
        assertEquals(addressWithSpaces, useCase.valueFlow.value)
        // Validation succeeds on trimmed text, _address has clean value
        assertEquals(parsedAddress, useCase.address.value)
    }

    @Test
    fun parseText_spacesAroundAddress_editInMiddle_preservesText() = testScope.runTest {
        val addressWithSpaces = "  $validAddress  "
        useCase.parseText(addressWithSpaces)
        advanceUntilIdle()
        awaitCondition("Expected success for address with spaces") {
            useCase.inputState.value is DataState.Success
        }

        // User edits in the middle — deletes char at position 20
        val edited = addressWithSpaces.removeRange(20, 21)
        every { addressParserChain.supportedHandler(edited.trim()) } returns null

        useCase.parseText(edited)
        advanceUntilIdle()
        awaitCondition("Expected error after editing") {
            useCase.inputState.value is DataState.Error
        }

        // valueFlow reflects exact user edit — spaces preserved
        assertEquals(edited, useCase.valueFlow.value)
    }

    @Test
    fun parseText_rapidEdits_lastValueWins() = testScope.runTest {
        val address1 = "${validAddress}A"
        val address2 = "${validAddress}AB"
        val address3 = "${validAddress}ABC"

        every { addressParserChain.supportedHandler(any()) } returns null

        useCase.parseText(address1)
        useCase.parseText(address2)
        useCase.parseText(address3)
        advanceUntilIdle()

        assertEquals(address3, useCase.valueFlow.value)
    }

    @Test
    fun parseText_validationDoesNotOverwriteValueFlow() = testScope.runTest {
        useCase.parseText(validAddress)
        advanceUntilIdle()

        assertEquals(validAddress, useCase.valueFlow.value)
    }

    @Test
    fun parseText_middleEdit_valueFlowMatchesUserInput() = testScope.runTest {
        val firstParseStarted = CountDownLatch(1)
        val allowFirstParseToFinish = CountDownLatch(1)
        val editedAddress = validAddress.removeRange(39, 40)

        every { handler.parseAddress(validAddress) } answers {
            firstParseStarted.countDown()
            assertTrue(
                "First validation did not unblock in time",
                allowFirstParseToFinish.await(1, TimeUnit.SECONDS)
            )
            parsedAddress
        }
        every { addressParserChain.supportedHandler(editedAddress) } returns null

        useCase.parseText(validAddress)
        assertTrue(
            "First validation did not start in time",
            firstParseStarted.await(1, TimeUnit.SECONDS)
        )

        useCase.parseText(editedAddress)
        advanceUntilIdle()

        awaitCondition("Edited address should produce error state") {
            useCase.inputState.value is DataState.Error
        }
        assertNull(useCase.address.value)
        assertEquals(editedAddress, useCase.valueFlow.value)

        allowFirstParseToFinish.countDown()
        awaitCondition("Canceled first validation should not restore old success state") {
            useCase.inputState.value is DataState.Error
        }

        assertEquals(editedAddress, useCase.valueFlow.value)
    }

    // =========================================================================
    // parseAddress (programmatic) — initial value, QR scan
    // =========================================================================

    @Test
    fun parseAddress_updatesValueFlow() = testScope.runTest {
        useCase.parseAddress(validAddress)
        advanceUntilIdle()

        assertEquals(validAddress, useCase.valueFlow.value)
    }

    @Test
    fun parseAddress_validAddress_setsSuccessState() = testScope.runTest {
        useCase.parseAddress(validAddress)
        advanceUntilIdle()

        awaitCondition("Expected success state for valid address") {
            useCase.inputState.value is DataState.Success
        }

        val state = useCase.inputState.value
        assertTrue("Expected DataState.Success, got $state", state is DataState.Success)
        assertEquals(parsedAddress, (state as DataState.Success).data)
    }

    // =========================================================================
    // Blank input
    // =========================================================================

    @Test
    fun parseText_blank_clearsState() = testScope.runTest {
        useCase.parseText(validAddress)
        advanceUntilIdle()

        useCase.parseText("")

        assertEquals("", useCase.valueFlow.value)
        assertNull(useCase.inputState.value)
        assertNull(useCase.address.value)
    }

    @Test
    fun parseAddress_blank_clearsState() = testScope.runTest {
        useCase.parseAddress(validAddress)
        advanceUntilIdle()

        useCase.parseAddress("")

        assertEquals("", useCase.valueFlow.value)
        assertNull(useCase.inputState.value)
        assertNull(useCase.address.value)
    }

    // =========================================================================
    // Validation state
    // =========================================================================

    @Test
    fun parseText_validAddress_setsSuccessState() = testScope.runTest {
        useCase.parseText(validAddress)
        advanceUntilIdle()

        awaitCondition("Expected success state for valid address") {
            useCase.inputState.value is DataState.Success
        }

        val state = useCase.inputState.value
        assertTrue("Expected DataState.Success, got $state", state is DataState.Success)
    }

    @Test
    fun parseText_unsupportedAddress_setsErrorState() = testScope.runTest {
        val unsupported = "not_a_valid_address"
        every { addressParserChain.supportedHandler(unsupported) } returns null

        useCase.parseText(unsupported)
        advanceUntilIdle()

        awaitCondition("Expected error state for unsupported address") {
            useCase.inputState.value is DataState.Error
        }

        val state = useCase.inputState.value
        assertTrue("Expected DataState.Error, got $state", state is DataState.Error)
    }

    // =========================================================================
    // Job cancellation
    // =========================================================================

    @Test
    fun parseText_cancelsStaleJob_onNewCall() = testScope.runTest {
        every { addressParserChain.supportedHandler("first_address") } returns null

        useCase.parseText("first_address")
        useCase.parseText(validAddress)
        advanceUntilIdle()

        awaitCondition("Expected success state for latest valid address") {
            useCase.inputState.value is DataState.Success
        }

        assertEquals(validAddress, useCase.valueFlow.value)
        // Final state should be for the last call (validAddress), not the first
        val state = useCase.inputState.value
        assertTrue("Expected DataState.Success, got $state", state is DataState.Success)
    }

    // =========================================================================
    // onAddressError
    // =========================================================================

    @Test
    fun onAddressError_setsErrorState() {
        val error = Throwable("test error")

        useCase.onAddressError(error)

        val state = useCase.inputState.value
        assertTrue("Expected DataState.Error, got $state", state is DataState.Error)
    }

    @Test
    fun onAddressError_null_doesNotChangeState() = testScope.runTest {
        useCase.parseText(validAddress)
        advanceUntilIdle()
        val stateBefore = useCase.inputState.value

        useCase.onAddressError(null)

        assertEquals(stateBefore, useCase.inputState.value)
    }
}
