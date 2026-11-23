package de.velwebsolutions.mildcrowd.app

import android.content.Context
import android.os.Build
import android.provider.Settings
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import de.velwebsolutions.mildcrowd.app.shell.AppHost
import de.velwebsolutions.mildcrowd.chat.core.LookPhotos
import de.velwebsolutions.mildcrowd.chat.core.LookStore
import de.velwebsolutions.mildcrowd.core.auth.Session
import de.velwebsolutions.mildcrowd.core.di.AppScope
import de.velwebsolutions.mildcrowd.core.di.AppVersion
import de.velwebsolutions.mildcrowd.core.di.DeviceName
import de.velwebsolutions.mildcrowd.core.network.ApiConfig
import de.velwebsolutions.mildcrowd.core.network.RealtimeConfig
import de.velwebsolutions.mildcrowd.core.screentime.ScreenTime
import de.velwebsolutions.mildcrowd.design.navigation.DeepLink
import de.velwebsolutions.mildcrowd.design.navigation.Navigator
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Server, user agent, version and device name of this build (BuildConfig per build type: debug, staging, release); the navigator. */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun apiConfig(): ApiConfig = ApiConfig(
        baseUrl = BuildConfig.MILDCROWD_BASE_URL,
        userAgent = "MildcrowdApp/${BuildConfig.VERSION_NAME} (Android; Android ${Build.VERSION.RELEASE})",
        gateKey = BuildConfig.MILDCROWD_GATE_KEY.ifEmpty { null }
    )

    @Provides
    @Singleton
    fun realtimeConfig(): RealtimeConfig = RealtimeConfig(BuildConfig.REVERB_HOST_OVERRIDE.ifEmpty { null })

    /** "Pixel 9" or the name the member gave the phone, shown in Privatsphäre › Geräte. */
    @Provides
    @Singleton
    fun deviceName(@ApplicationContext context: Context): DeviceName = DeviceName(
        Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)?.takeIf { it.isNotBlank() }
            ?: "${Build.MANUFACTURER.replaceFirstChar(Char::uppercase)} ${Build.MODEL}"
    )

    /** The shared product version („Mildcrowd 0.24.0“); installed and never updated since when both times are equal. */
    @Provides
    @Singleton
    fun appVersion(@ApplicationContext context: Context): AppVersion {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        return AppVersion(BuildConfig.VERSION_NAME, freshInstall = info.firstInstallTime == info.lastUpdateTime)
    }

    /** The app's one navigator; after a sign-out the next member starts on the roots of the tabs ([AppHost.resetAfterSignOut]). */
    @Provides
    @Singleton
    fun navigator(
        session: Session,
        @AppScope scope: CoroutineScope,
        @ApplicationContext context: Context,
        lookStore: LookStore,
        lookPhotos: LookPhotos,
        screenTime: ScreenTime
    ): Navigator = Navigator().also { navigator ->
        // Links open screens only for this build's server (hints, push, posts).
        DeepLink.ownHost = java.net.URI(BuildConfig.MILDCROWD_BASE_URL).host
        val clear = { AppHost.clearAfterSignOut(context, lookStore, lookPhotos, scope, screenTime) }
        scope.launch { AppHost.resetAfterSignOut(session.state, navigator, clear) }
    }
}
