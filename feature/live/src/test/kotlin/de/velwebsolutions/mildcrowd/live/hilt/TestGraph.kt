package de.velwebsolutions.mildcrowd.live.hilt

import androidx.activity.ComponentActivity
import dagger.Module
import dagger.Provides
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import de.velwebsolutions.mildcrowd.core.auth.AuthApi
import de.velwebsolutions.mildcrowd.core.auth.InMemoryTokenStore
import de.velwebsolutions.mildcrowd.core.auth.RemoteAuthApi
import de.velwebsolutions.mildcrowd.core.auth.Session
import de.velwebsolutions.mildcrowd.core.di.AppScope
import de.velwebsolutions.mildcrowd.core.di.CoreModule
import de.velwebsolutions.mildcrowd.core.network.ApiClient
import de.velwebsolutions.mildcrowd.core.network.CommonApi
import de.velwebsolutions.mildcrowd.core.network.RemoteCommonApi
import de.velwebsolutions.mildcrowd.core.realtime.NoRealtime
import de.velwebsolutions.mildcrowd.core.realtime.RealtimeService
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
}

/** An activity of the test graph, so `hiltViewModel()` works in the screens. */
@AndroidEntryPoint
class HiltTestActivity : ComponentActivity()
