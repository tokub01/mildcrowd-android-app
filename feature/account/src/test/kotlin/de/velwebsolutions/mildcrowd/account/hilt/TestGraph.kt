package de.velwebsolutions.mildcrowd.account.hilt

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import dagger.Module
import dagger.Provides
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import de.velwebsolutions.mildcrowd.core.auth.AuthApi
import de.velwebsolutions.mildcrowd.core.auth.InMemoryTokenStore
import de.velwebsolutions.mildcrowd.core.auth.RemoteAuthApi
import de.velwebsolutions.mildcrowd.core.auth.Session
import de.velwebsolutions.mildcrowd.core.auth.SessionState
import de.velwebsolutions.mildcrowd.core.di.AppScope
import de.velwebsolutions.mildcrowd.core.di.AppVersion
import de.velwebsolutions.mildcrowd.core.di.CoreModule
import de.velwebsolutions.mildcrowd.core.di.DeviceName
import de.velwebsolutions.mildcrowd.core.network.ApiClient
import de.velwebsolutions.mildcrowd.core.network.CommonApi
import de.velwebsolutions.mildcrowd.core.network.RemoteCommonApi
import de.velwebsolutions.mildcrowd.core.realtime.NoRealtime
import de.velwebsolutions.mildcrowd.core.realtime.RealtimeService
import de.velwebsolutions.mildcrowd.core.screentime.PreferencesScreenTimeStore
import de.velwebsolutions.mildcrowd.core.screentime.ScreenTime
import de.velwebsolutions.mildcrowd.testing.StubTransport
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** The app's graph for the module's Hilt tests: the stubbed API instead of a server, no realtime. */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [CoreModule::class])
object TestCoreModule {
    @Provides
    @Singleton
    fun transport(): StubTransport = StubTransport()

    @Provides
    @Singleton
    @AppScope
    fun appScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

    @Provides
    @Singleton
    fun session(@AppScope scope: CoroutineScope, auth: dagger.Lazy<AuthApi>): Session =
        Session(InMemoryTokenStore("token"), "Test", scope) { auth.get() }

    @Provides
    @Singleton
    fun apiClient(transport: StubTransport): ApiClient = transport.client

    @Provides
    @Singleton
    fun authApi(client: ApiClient): AuthApi = RemoteAuthApi(client)

    @Provides
    @Singleton
    fun commonApi(client: ApiClient): CommonApi = RemoteCommonApi(client)

    @Provides
    @Singleton
    fun realtime(): RealtimeService = NoRealtime()

    @Provides
    fun deviceName(): DeviceName = DeviceName("Pixel 9")

    @Provides
    fun appVersion(): AppVersion = AppVersion("0.24.0", freshInstall = false)

    @Provides
    @Singleton
    fun screenTime(@ApplicationContext context: Context, session: Session): ScreenTime =
        ScreenTime(PreferencesScreenTimeStore(context), userId = { (session.state.value as? SessionState.SignedIn)?.me?.id })
}

/**
 * An activity of the test graph, so `hiltViewModel()` works in the screens. It shows [content] again when it is
 * recreated, so saved state can be restored like in the app.
 */
@AndroidEntryPoint
class HiltTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { content() }
    }

    companion object {
        /** What the activity shows. */
        var content: @Composable () -> Unit = {}
    }
}
