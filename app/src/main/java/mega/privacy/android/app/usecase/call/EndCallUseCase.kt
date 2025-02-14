package mega.privacy.android.app.usecase.call

import io.reactivex.rxjava3.core.Completable
import mega.privacy.android.app.listeners.OptionalMegaChatRequestListenerInterface
import mega.privacy.android.app.usecase.exception.toMegaException
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaChatError
import nz.mega.sdk.MegaChatRequest
import nz.mega.sdk.MegaError
import timber.log.Timber
import javax.inject.Inject

/**
 * Use case to end call
 *
 * @property megaChatApi    MegaChatApi required to call the SDK
 */
class EndCallUseCase @Inject constructor(
    private val megaChatApi: MegaChatApiAndroid,
) {

    /**
     * Method to end a call for all
     *
     * @param chatId Chat ID
     */
    @Deprecated("Use [EndCallUseCase] domain use case instead")
    fun endCallForAllWithChatId(
        chatId: Long,
    ): Completable =
        Completable.create { emitter ->
            megaChatApi.getChatCall(chatId)?.let { call ->
                megaChatApi.endChatCall(call.callId,
                    OptionalMegaChatRequestListenerInterface(
                        onRequestFinish = { _: MegaChatRequest, error: MegaChatError ->
                            when {
                                emitter.isDisposed -> return@OptionalMegaChatRequestListenerInterface
                                error.errorCode == MegaError.API_OK -> emitter.onComplete()
                                else -> emitter.onError(error.toMegaException())
                            }
                        })
                )
            }

            emitter.onComplete()
        }

    /**
     * Method to hang a call
     *
     * @param callId Chat ID
     */
    fun hangCall(
        callId: Long,
    ): Completable =
        Completable.create { emitter ->
            megaChatApi.hangChatCall(callId,
                OptionalMegaChatRequestListenerInterface(
                    onRequestFinish = { _: MegaChatRequest, error: MegaChatError ->
                        when {
                            emitter.isDisposed -> return@OptionalMegaChatRequestListenerInterface
                            error.errorCode == MegaError.API_OK -> {
                                Timber.d("Call successfully hung up")
                                emitter.onComplete()
                            }
                            else -> emitter.onError(error.toMegaException())
                        }
                    })
            )

            emitter.onComplete()
        }
}