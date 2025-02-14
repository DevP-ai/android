package mega.privacy.android.app.fragments.settingsFragments.cookie.usecase

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Use Case to get cookie settings from SDK
 */
class GetCookieSettingsUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {

    /**
     * Invoke
     *
     * @return Flow of cookie settings
     */
    suspend operator fun invoke() = accountRepository.getCookieSettings()
}
