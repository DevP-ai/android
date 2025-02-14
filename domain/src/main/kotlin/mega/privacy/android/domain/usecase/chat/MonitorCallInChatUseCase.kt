package mega.privacy.android.domain.usecase.chat

import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import mega.privacy.android.domain.entity.meeting.ChatCallStatus
import mega.privacy.android.domain.repository.CallRepository
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdatesUseCase
import javax.inject.Inject

/**
 * Monitor A Call In This Chat Use Case
 *
 */
class MonitorCallInChatUseCase @Inject constructor(
    private val monitorChatCallUpdatesUseCase: MonitorChatCallUpdatesUseCase,
    private val callRepository: CallRepository,
) {
    /**
     * Invoke
     *
     */
    operator fun invoke(chatId: Long) = monitorChatCallUpdatesUseCase()
        .filter { it.chatId == chatId }
        .map {
            if (it.status == ChatCallStatus.Destroyed || it.status == ChatCallStatus.Unknown) {
                null
            } else {
                callRepository.getChatCall(chatId)
            }
        }
        .onStart { emit(callRepository.getChatCall(chatId)) }
}