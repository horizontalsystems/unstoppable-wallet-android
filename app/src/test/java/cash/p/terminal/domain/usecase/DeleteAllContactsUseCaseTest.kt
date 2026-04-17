package cash.p.terminal.domain.usecase

import cash.p.terminal.modules.contacts.ContactsRepository
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class DeleteAllContactsUseCaseTest {

    private val contactsRepository: ContactsRepository = mockk(relaxed = true)
    private val useCase = DeleteAllContactsUseCase(contactsRepository)

    @Test
    fun invoke_clearsContactsSynchronously() {
        useCase()

        verify(exactly = 1) {
            contactsRepository.clear(writeSynchronously = true)
        }
    }
}
