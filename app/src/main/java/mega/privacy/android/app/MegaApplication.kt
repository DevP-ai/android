package mega.privacy.android.app

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.StrictMode
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.multidex.MultiDexApplication
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.SvgDecoder
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.hilt.android.HiltAndroidApp
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.components.ChatManagement
import mega.privacy.android.app.components.PushNotificationSettingManagement
import mega.privacy.android.app.fcm.CreateNotificationChannelsUseCase
import mega.privacy.android.app.fetcher.MegaThumbnailFetcher
import mega.privacy.android.app.fetcher.MegaThumbnailKeyer
import mega.privacy.android.app.fragments.settingsFragments.cookie.usecase.rxjava.GetCookieSettingsUseCaseRx
import mega.privacy.android.app.globalmanagement.ActivityLifecycleHandler
import mega.privacy.android.app.globalmanagement.CallChangesObserver
import mega.privacy.android.app.globalmanagement.MegaChatNotificationHandler
import mega.privacy.android.app.globalmanagement.MegaChatRequestHandler
import mega.privacy.android.app.globalmanagement.MyAccountInfo
import mega.privacy.android.app.globalmanagement.TransfersManagement
import mega.privacy.android.app.listeners.GlobalChatListener
import mega.privacy.android.app.meeting.CallService
import mega.privacy.android.app.meeting.CallSoundType
import mega.privacy.android.app.meeting.CallSoundsController
import mega.privacy.android.app.meeting.gateway.RTCAudioManagerGateway
import mega.privacy.android.app.meeting.listeners.MeetingListener
import mega.privacy.android.app.monitoring.CrashReporter
import mega.privacy.android.app.objects.PasscodeManagement
import mega.privacy.android.app.presentation.theme.ThemeModeState
import mega.privacy.android.app.receivers.GlobalNetworkStateHandler
import mega.privacy.android.app.usecase.call.GetCallSoundsUseCase
import mega.privacy.android.app.utils.CacheFolderManager.clearPublicCache
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.greeter.Greeter
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.data.qualifier.MegaApiFolder
import mega.privacy.android.domain.entity.settings.cookie.CookieType
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.apiserver.UpdateApiServerUseCase
import mega.privacy.android.domain.usecase.monitoring.EnablePerformanceReporterUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.GetPublicNodeThumbnailUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.GetThumbnailUseCase
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaChatCall
import nz.mega.sdk.MegaHandleList
import org.webrtc.ContextUtils
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Provider

/**
 * Mega application
 *
 * @property megaApi
 * @property megaApiFolder
 * @property megaChatApi
 * @property dbH
 * @property getCookieSettingsUseCase
 * @property myAccountInfo
 * @property passcodeManagement
 * @property crashReporter
 * @property enablePerformanceReporterUseCase
 * @property getCallSoundsUseCase
 * @property themeModeState
 * @property transfersManagement
 * @property activityLifecycleHandler
 * @property megaChatNotificationHandler
 * @property pushNotificationSettingManagement
 * @property chatManagement
 * @property chatRequestHandler
 * @property rtcAudioManagerGateway
 * @property callChangesObserver
 * @property globalChatListener
 * @property localIpAddress
 * @property isEsid
 * @property globalNetworkStateHandler
 */
@HiltAndroidApp
class MegaApplication : MultiDexApplication(), DefaultLifecycleObserver,
    ImageLoaderFactory {
    @MegaApi
    @Inject
    lateinit var megaApi: MegaApiAndroid

    @MegaApiFolder
    @Inject
    lateinit var megaApiFolder: MegaApiAndroid

    @Inject
    @get:JvmName("megaChatApi")
    lateinit var megaChatApi: MegaChatApiAndroid

    @Inject
    lateinit var dbH: LegacyDatabaseHandler

    @Inject
    lateinit var getCookieSettingsUseCase: GetCookieSettingsUseCaseRx

    @Inject
    lateinit var myAccountInfo: MyAccountInfo

    @Inject
    lateinit var passcodeManagement: PasscodeManagement

    @Inject
    lateinit var crashReporter: CrashReporter

    @Inject
    lateinit var enablePerformanceReporterUseCase: EnablePerformanceReporterUseCase

    @Inject
    lateinit var getCallSoundsUseCase: GetCallSoundsUseCase


    @ApplicationScope
    @Inject
    lateinit var applicationScope: CoroutineScope

    @Inject
    internal lateinit var createNotificationChannelsUseCase: CreateNotificationChannelsUseCase

    @Inject
    lateinit var themeModeState: ThemeModeState

    @Inject
    lateinit var transfersManagement: TransfersManagement

    @Inject
    lateinit var activityLifecycleHandler: ActivityLifecycleHandler

    @Inject
    lateinit var megaChatNotificationHandler: MegaChatNotificationHandler

    @Inject
    @get:JvmName("pushNotificationSettingManagement")
    lateinit var pushNotificationSettingManagement: PushNotificationSettingManagement

    @Inject
    @get:JvmName("chatManagement")
    lateinit var chatManagement: ChatManagement

    @Inject
    lateinit var chatRequestHandler: MegaChatRequestHandler

    @Inject
    lateinit var rtcAudioManagerGateway: RTCAudioManagerGateway

    @Inject
    lateinit var callChangesObserver: CallChangesObserver

    @Inject
    lateinit var globalChatListener: GlobalChatListener

    @Inject
    lateinit var globalNetworkStateHandler: GlobalNetworkStateHandler

    @Inject
    lateinit var greeter: Provider<Greeter>

    @Inject
    lateinit var getThumbnailUseCase: dagger.Lazy<GetThumbnailUseCase>

    @Inject
    lateinit var getPublicNodeThumbnailUseCase: dagger.Lazy<GetPublicNodeThumbnailUseCase>

    @Inject
    lateinit var updateApiServerUseCase: UpdateApiServerUseCase

    var localIpAddress: String? = ""

    var isEsid = false

    private val meetingListener = MeetingListener()
    private val soundsController = CallSoundsController()

    private fun handleUncaughtException(throwable: Throwable?) {
        Timber.e(throwable, "UNCAUGHT EXCEPTION")
        crashReporter.report(throwable ?: return)
    }

    /**
     * On create
     *
     */
    override fun onCreate() {
        instance = this
        super<MultiDexApplication>.onCreate()
        enableStrictMode()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        themeModeState.initialise()
        callChangesObserver.init()
        LiveEventBus.config().enableLogger(false)

        // Setup handler and RxJava for uncaught exceptions.
        if (!BuildConfig.DEBUG) {
            Thread.setDefaultUncaughtExceptionHandler { _: Thread?, e: Throwable? ->
                handleUncaughtException(e)
            }
        } else {
            Firebase.crashlytics.setCrashlyticsCollectionEnabled(false)
        }
        RxJavaPlugins.setErrorHandler { throwable: Throwable? -> handleUncaughtException(throwable) }

        registerActivityLifecycleCallbacks(activityLifecycleHandler)
        isVerifySMSShowed = false

        setupMegaChatApi()

        //Logout check resumed pending transfers
        transfersManagement.apply {
            checkResumedPendingTransfers()
        }

        applicationScope.launch {
            runCatching { updateApiServerUseCase() }
            dbH.resetExtendedAccountDetailsTimestamp()
        }

        val useHttpsOnly = java.lang.Boolean.parseBoolean(dbH.useHttpsOnly)
        Timber.d("Value of useHttpsOnly: %s", useHttpsOnly)
        megaApi.useHttpsOnly(useHttpsOnly)
        myAccountInfo.resetDefaults()

        // clear the cache files stored in the external cache folder.
        clearPublicCache()
        ContextUtils.initialize(applicationContext)

        if (BuildConfig.ACTIVATE_GREETER) greeter.get().initialize()

        applicationScope.launch { createNotificationChannelsUseCase() }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                if (SDK_INT >= Build.VERSION_CODES.P) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
                add(SvgDecoder.Factory())
                add(
                    MegaThumbnailFetcher.Factory(
                        getThumbnailUseCase = getThumbnailUseCase,
                        getPublicNodeThumbnailUseCase = getPublicNodeThumbnailUseCase
                    )
                )
                add(MegaThumbnailKeyer)
            }
            .build()
    }

    /**
     * On start
     *
     */
    override fun onStart(owner: LifecycleOwner) {
        val backgroundStatus = megaChatApi.backgroundStatus
        Timber.d("Application start with backgroundStatus: %s", backgroundStatus)
        if (backgroundStatus != -1 && backgroundStatus != 0) {
            megaChatApi.setBackgroundStatus(false)
        }
    }

    /**
     * On stop
     *
     */
    override fun onStop(owner: LifecycleOwner) {
        val backgroundStatus = megaChatApi.backgroundStatus
        Timber.d("Application stop with backgroundStatus: %s", backgroundStatus)
        if (backgroundStatus != -1 && backgroundStatus != 1) {
            megaChatApi.setBackgroundStatus(true)
        }
    }

    private fun enableStrictMode() {
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyDeathOnNetwork()
                    .penaltyLog()
                    .build()
            )

            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )

            if (SDK_INT >= Build.VERSION_CODES.S) {
                StrictMode.setVmPolicy(
                    StrictMode.VmPolicy.Builder() // Other StrictMode checks that you've previously added.
                        .detectUnsafeIntentLaunch()
                        .penaltyLog()
                        .build()
                )
            }
        }
    }

    /**
     * Disable mega chat api
     *
     */
    fun disableMegaChatApi() {
        try {
            megaChatApi.apply {
                removeChatRequestListener(chatRequestHandler)
                removeChatNotificationListener(megaChatNotificationHandler)
                removeChatListener(globalChatListener)
                removeChatCallListener(meetingListener)
            }
            registeredChatListeners = false
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    private fun setupMegaChatApi() {
        if (!registeredChatListeners) {
            Timber.d("Add listeners of megaChatApi")
            megaChatApi.apply {
                addChatRequestListener(chatRequestHandler)
                addChatNotificationListener(megaChatNotificationHandler)
                addChatListener(globalChatListener)
                addChatCallListener(meetingListener)
            }
            registeredChatListeners = true
            checkCallSounds()
        }
    }

    /**
     * Check the changes of the meeting to play the right sound
     */
    private fun checkCallSounds() {
        getCallSoundsUseCase.get()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { next: CallSoundType ->
                soundsController.playSound(next)
            }
    }

    /**
     * Check current enabled cookies and set the corresponding flags to true/false
     */
    fun checkEnabledCookies() {
        getCookieSettingsUseCase.get()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { cookies: Set<CookieType> ->
                    val analyticsCookiesEnabled = cookies.contains(CookieType.ANALYTICS)
                    crashReporter.setEnabled(analyticsCookiesEnabled)
                    enablePerformanceReporterUseCase(analyticsCookiesEnabled)
                },
                { throwable: Throwable -> Timber.e(throwable) }
            )
    }

    /**
     * Get mega chat api
     *
     */
    fun getMegaChatApi(): MegaChatApiAndroid {
        setupMegaChatApi()
        return megaChatApi
    }

    /**
     * Send signal presence activity
     *
     */
    fun sendSignalPresenceActivity() {
        Timber.d("sendSignalPresenceActivity")
        megaChatApi.run { signalPresenceActivity() }
    }

    /**
     * Method for showing an incoming group or one-to-one call notification.
     *
     * @param incomingCall The incoming call
     */
    fun showOneCallNotification(incomingCall: MegaChatCall) =
        callChangesObserver.showOneCallNotification(incomingCall)

    /**
     * Check one call
     *
     * @param incomingCallChatId
     */
    fun checkOneCall(incomingCallChatId: Long) =
        callChangesObserver.checkOneCall(incomingCallChatId)

    /**
     * Check several call
     *
     * @param listAllCalls
     * @param callStatus
     * @param isRinging
     * @param incomingCallChatId
     */
    fun checkSeveralCall(
        listAllCalls: MegaHandleList?,
        callStatus: Int,
        isRinging: Boolean,
        incomingCallChatId: Long,
    ) {
        listAllCalls?.let {
            callChangesObserver.checkSeveralCall(
                it,
                callStatus,
                isRinging,
                incomingCallChatId
            )
        }
    }

    /**
     * Create or update audio manager
     *
     * @param isSpeakerOn
     * @param type
     */
    fun createOrUpdateAudioManager(isSpeakerOn: Boolean, type: Int) {
        Timber.d("Create or update audio manager, type is %s", type)
        chatManagement.registerScreenReceiver()
        rtcAudioManagerGateway.createOrUpdateAudioManager(isSpeakerOn, type)
    }

    /**
     * Remove the incoming call AppRTCAudioManager.
     */
    fun removeRTCAudioManagerRingIn() = rtcAudioManagerGateway.removeRTCAudioManagerRingIn()

    /**
     * Activate the proximity sensor.
     */
    fun startProximitySensor() = rtcAudioManagerGateway.startProximitySensor { isNear: Boolean ->
        chatManagement.controlProximitySensor(isNear)
    }

    /**
     * Open call service
     *
     * @param chatId
     */
    fun openCallService(chatId: Long) {
        if (chatId != MegaChatApiJava.MEGACHAT_INVALID_HANDLE) {
            Timber.d("Start call Service. Chat iD = $chatId")
            Intent(this, CallService::class.java).run {
                putExtra(Constants.CHAT_ID, chatId)
                startForegroundService(this)
            }
        }
    }

    /**
     * Reset my account info
     *
     */
    fun resetMyAccountInfo() = myAccountInfo.resetDefaults()

    /**
     * Current activity
     */
    val currentActivity: Activity?
        get() = activityLifecycleHandler.getCurrentActivity()

    companion object {
        /**
         * App Key
         */
        const val APP_KEY = "6tioyn8ka5l6hty"

        /**
         * Is logging out
         */
        @JvmStatic
        var isLoggingOut = false

        /**
         * Is is heart beat alive
         */
        @JvmStatic
        @Volatile
        var isIsHeartBeatAlive = false
            private set

        /**
         * Is show info chat messages
         */
        @JvmStatic
        var isShowInfoChatMessages = false

        /**
         * Open chat id
         */
        @JvmStatic
        var openChatId: Long = -1

        /**
         * Is closed chat
         */
        @JvmStatic
        var isClosedChat = true

        /**
         * Is show rich link warning
         */
        @JvmStatic
        var isShowRichLinkWarning = false

        /**
         * Counter not now rich link warning
         */
        @JvmStatic
        var counterNotNowRichLinkWarning = -1

        /**
         * Is enabled rich links
         */
        var isEnabledRichLinks = false

        /**
         * Is enabled geo location
         *
         * @deprecated
         */
        @JvmStatic
        @Deprecated(
            "Global usage is deprecated",
            ReplaceWith("IsGeolocationEnabledUseCase")
        )
        var isEnabledGeoLocation = false

        /**
         * Url confirmation link
         */
        @JvmStatic
        @Volatile
        var urlConfirmationLink: String? = null

        private var registeredChatListeners = false

        /**
         * Is verify s m s showed
         */
        var isVerifySMSShowed = false
            private set

        /**
         * Is blocked due to weak account
         */
        @JvmStatic
        var isBlockedDueToWeakAccount = false

        /**
         * Is web open due to email verification
         */
        var isWebOpenDueToEmailVerification = false
            private set

        /**
         * Is waiting for call
         */
        @JvmStatic
        var isWaitingForCall = false

        /**
         * User waiting for call
         */
        @JvmStatic
        var userWaitingForCall = MegaChatApiJava.MEGACHAT_INVALID_HANDLE
        private lateinit var instance: MegaApplication

        /**
         * Get instance
         */
        @JvmStatic
        fun getInstance(): MegaApplication = instance

        /**
         * Sms verify showed
         *
         * @param isShowed
         */
        @JvmStatic
        fun smsVerifyShowed(isShowed: Boolean) {
            isVerifySMSShowed = isShowed
        }

        /**
         * Set is web open due to email verification
         *
         * @param isWebOpenDueToEmailVerification
         */
        @JvmStatic
        fun setIsWebOpenDueToEmailVerification(isWebOpenDueToEmailVerification: Boolean) {
            this.isWebOpenDueToEmailVerification = isWebOpenDueToEmailVerification
        }

        /**
         * Set heart beat alive
         *
         * @param heartBeatAlive
         */
        @JvmStatic
        fun setHeartBeatAlive(heartBeatAlive: Boolean) {
            isIsHeartBeatAlive = heartBeatAlive
        }

        /**
         * Get push notification setting management
         */
        @JvmStatic
        fun getPushNotificationSettingManagement(): PushNotificationSettingManagement =
            instance.pushNotificationSettingManagement

        /**
         * Get chat management
         */
        @JvmStatic
        fun getChatManagement(): ChatManagement = instance.chatManagement
    }
}
