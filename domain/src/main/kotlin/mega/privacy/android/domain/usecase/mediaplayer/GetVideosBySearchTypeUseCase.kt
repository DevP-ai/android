package mega.privacy.android.domain.usecase.mediaplayer

import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.repository.MediaPlayerRepository
import mega.privacy.android.domain.usecase.AddNodeType
import javax.inject.Inject

/**
 * The use case for getting videos by search type api
 */
class GetVideosBySearchTypeUseCase @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository,
    private val addNodeType: AddNodeType,
) {

    /**
     * Getting videos by search type api
     *
     * @param handle parent handle
     * @param searchString containing a search string in their name
     * @param recursive: is search recursively,
     * @param order SortOrder
     * @return video nodes
     */
    suspend operator fun invoke(
        handle: Long,
        searchString: String = "*",
        recursive: Boolean,
        order: SortOrder,
    ) =
        mediaPlayerRepository.getVideosBySearchType(
            handle = handle,
            searchString = searchString,
            recursive = recursive,
            order = order
        )?.map { addNodeType(it) }
}