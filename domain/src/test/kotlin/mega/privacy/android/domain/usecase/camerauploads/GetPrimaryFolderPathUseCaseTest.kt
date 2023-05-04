package mega.privacy.android.domain.usecase.camerauploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CameraUploadRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import java.util.stream.Stream

/**
 * Test class for [GetPrimaryFolderPathUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetPrimaryFolderPathUseCaseTest {

    private lateinit var underTest: GetPrimaryFolderPathUseCase

    private val cameraUploadRepository = mock<CameraUploadRepository>()

    @BeforeAll
    fun setUp() {
        underTest = GetPrimaryFolderPathUseCase(
            cameraUploadRepository = cameraUploadRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            cameraUploadRepository,
        )
    }

    @ParameterizedTest(name = "is in SD card: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that the primary folder path is retrieved`(isInSDCard: Boolean) = runTest {
        val testLocalPath = "test/local/path/"
        val testSDCardPath = "test/sd/card/path/"

        cameraUploadRepository.stub {
            onBlocking { isPrimaryFolderInSDCard() }.thenReturn(isInSDCard)
            onBlocking { getPrimaryFolderSDCardUriPath() }.thenReturn(testSDCardPath)
            onBlocking { getPrimaryFolderLocalPath() }.thenReturn(testLocalPath)
        }

        val expectedPath = underTest()
        if (isInSDCard) {
            assertThat(expectedPath).isEqualTo(testSDCardPath)
        } else {
            assertThat(expectedPath).isEqualTo(testLocalPath)
        }
    }

    @ParameterizedTest(name = "when the original path is {0}, the new path becomes {1}")
    @MethodSource("providePathParameters")
    fun `test that the separator is appended in the primary folder path`(
        originalPath: String,
        newPath: String,
    ) = runTest {
        cameraUploadRepository.stub {
            onBlocking { isPrimaryFolderInSDCard() }.thenReturn(false)
            onBlocking { getPrimaryFolderLocalPath() }.thenReturn(originalPath)
        }

        val expectedPath = underTest()
        assertThat(expectedPath).isEqualTo(newPath)
    }

    private fun providePathParameters() = Stream.of(
        Arguments.of("", ""),
        Arguments.of(" ", " "),
        Arguments.of("test/path", "test/path/"),
        Arguments.of("test/path/", "test/path/"),
        Arguments.of("test/path//", "test/path//")
    )
}