package mega.privacy.android.domain.entity.chat.messages

/**
 * Invalid message
 *
 * @property type [InvalidMessageType]
 */
data class InvalidMessage(
    override val msgId: Long,
    override val time: Long,
    override val isMine: Boolean,
    override val userHandle: Long,
    val type: InvalidMessageType,
) : TypedMessage
