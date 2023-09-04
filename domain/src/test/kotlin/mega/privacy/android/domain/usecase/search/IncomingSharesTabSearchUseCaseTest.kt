package mega.privacy.android.domain.usecase.search

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.repository.SearchRepository
import mega.privacy.android.domain.usecase.AddNodeType
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class IncomingSharesTabSearchUseCaseTest {
    private lateinit var underTest: IncomingSharesTabSearchUseCase
    private val addNodeType: AddNodeType = mock()
    private val searchRepository: SearchRepository = mock()
    private val getCloudSortOrder: GetCloudSortOrder = mock()

    @Before
    fun setUp() {
        underTest = IncomingSharesTabSearchUseCase(
            addNodeType = addNodeType,
            searchRepository = searchRepository,
            getCloudSortOrder = getCloudSortOrder
        )
    }

    @Test
    fun `test that when invoke is called it returns list of typed node`() = runTest {
        val query = "Some thing"
        val nodeHandles = listOf(1L, 2L)
        val nodeName = listOf("abc", "xyz")
        val folderNodes = nodeHandles.mapIndexed { index, nodeId ->
            mock<FolderNode> {
                on { id }.thenReturn(NodeId(nodeId))
                on { name }.thenReturn(nodeName[index])
            }
        }
        val typedFolderNodes = nodeHandles.mapIndexed { index, nodeId ->
            mock<TypedFolderNode> {
                on { id }.thenReturn(NodeId(nodeId))
                on { name }.thenReturn(nodeName[index])
            }
        }
        folderNodes.forEachIndexed { index, node ->
            whenever(addNodeType(node)).thenReturn(typedFolderNodes[index])
        }
        whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_DEFAULT_DESC)
        whenever(
            searchRepository.searchInShares(
                query = query,
                order = getCloudSortOrder()
            )
        ).thenReturn(folderNodes)
        val actual = underTest(query)
        Truth.assertThat(actual).isEqualTo(typedFolderNodes)
    }
}