package de.velwebsolutions.mildcrowd.membership

import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.test.core.app.ApplicationProvider
import de.velwebsolutions.mildcrowd.design.navigation.LocalNavigator
import de.velwebsolutions.mildcrowd.design.navigation.MainTab
import de.velwebsolutions.mildcrowd.design.navigation.MembershipRoute
import de.velwebsolutions.mildcrowd.design.navigation.Navigator
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdTheme
import de.velwebsolutions.mildcrowd.membership.billing.PlayDelivery
import de.velwebsolutions.mildcrowd.membership.data.MembershipEvents
import de.velwebsolutions.mildcrowd.membership.data.MembershipFiles
import de.velwebsolutions.mildcrowd.membership.ui.CancelViewModel
import de.velwebsolutions.mildcrowd.membership.ui.CompanionLinkViewModel
import de.velwebsolutions.mildcrowd.membership.ui.InvoiceViewModel
import de.velwebsolutions.mildcrowd.membership.ui.OverviewViewModel
import de.velwebsolutions.mildcrowd.membership.ui.PerksViewModel
import de.velwebsolutions.mildcrowd.membership.ui.PlayCheckoutViewModel
import de.velwebsolutions.mildcrowd.membership.ui.PortalViewModel
import de.velwebsolutions.mildcrowd.membership.ui.TransparencyViewModel
import de.velwebsolutions.mildcrowd.membership.ui.WithdrawViewModel
import kotlin.reflect.KClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * An activity that answers Hilt's lookup for the view-model factory without a Hilt component: the view models are
 * already in the store, so the factory Hilt builds from it is never asked to create one.
 */
class HiltlessActivity : androidx.activity.ComponentActivity(), dagger.hilt.internal.GeneratedComponentManager<Any> {
    override fun generatedComponent(): Any {
        val entryPoint = Class.forName("dagger.hilt.android.internal.lifecycle.HiltViewModelFactory\$ActivityCreatorEntryPoint")
        return java.lang.reflect.Proxy.newProxyInstance(
            entryPoint.classLoader,
            arrayOf(entryPoint, dagger.hilt.internal.GeneratedComponent::class.java)
        ) {
                _,
                method,
                _
            ->
            if (method.name == "getViewModelKeys") emptyMap<Class<*>, Boolean>() else null
        }
    }
}

/**
 * The module's entry point picks the screen for each route. Hilt is not running in unit tests, so the view models the
 * screens ask Hilt for are put into the view-model store beforehand, under the keys the screens use.
 */
@RunWith(RobolectricTestRunner::class)
class RouteScreensTest {
    @get:Rule
    val compose = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val repository = FakeMembershipRepository()
    private val files = MembershipFiles(context.cacheDir)
    private val events = MembershipEvents()
    private val gateway = FakeBillingGateway()

    private val store = ViewModelStore()
    private val owner = object : ViewModelStoreOwner {
        override val viewModelStore = store
    }

    private val factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T = when (modelClass) {
            OverviewViewModel::class -> OverviewViewModel(repository, events)
            PortalViewModel::class -> PortalViewModel(repository, files)
            InvoiceViewModel::class -> InvoiceViewModel(7, repository, files)
            CancelViewModel::class -> CancelViewModel(repository, files, events, Fixtures.session())
            WithdrawViewModel::class -> WithdrawViewModel(repository, files, events, Fixtures.session())
            PerksViewModel::class -> PerksViewModel(repository, events, Fixtures.session())
            CompanionLinkViewModel::class -> CompanionLinkViewModel("a".repeat(40), repository, events, Fixtures.session())
            TransparencyViewModel::class -> TransparencyViewModel(repository)
            else -> PlayCheckoutViewModel(repository, gateway, PlayDelivery(repository), events, Fixtures.session())
        } as T
    }

    private val activity = org.robolectric.Robolectric.buildActivity(HiltlessActivity::class.java).create().get()

    private fun prepare() {
        val provider = ViewModelProvider.create(store, factory)
        provider[OverviewViewModel::class]
        provider[PortalViewModel::class]
        provider["invoice-7", InvoiceViewModel::class]
        provider[CancelViewModel::class]
        provider[WithdrawViewModel::class]
        provider[PlayCheckoutViewModel::class]
        provider[PerksViewModel::class]
        provider[CompanionLinkViewModel::class]
        provider[TransparencyViewModel::class]
    }

    @Test
    fun `every membership route shows its screen`() {
        prepare()
        var route: MembershipRoute by mutableStateOf(MembershipRoute.Overview)
        compose.setContent {
            MildcrowdTheme {
                CompositionLocalProvider(
                    LocalNavigator provides Navigator().apply { selectedTab = MainTab.Profile },
                    LocalViewModelStoreOwner provides owner,
                    androidx.compose.ui.platform.LocalContext provides activity
                ) { MembershipScreen(route) }
            }
        }
        compose.onNodeWithTag("membership.statusCard").assertExists()
        route = MembershipRoute.Portal
        compose.onNodeWithText("Bezahlte Beiträge").assertExists()
        route = MembershipRoute.Invoice(7)
        compose.onNodeWithTag("invoice.view").assertExists()
        route = MembershipRoute.Cancel
        compose.onNodeWithText("Bestätigung deiner Kündigung gemäß § 312k BGB.").assertExists()
        route = MembershipRoute.Withdraw
        compose.onNodeWithText("Gesetzliches Widerrufsrecht innerhalb von 14 Tagen (§ 355 BGB).").assertExists()
        route = MembershipRoute.Checkout
        compose.onNodeWithText("Bleib in der Mildcrowd.").assertExists()
        route = MembershipRoute.Perks
        compose.onNodeWithText("In Mäzen enthalten").assertExists()
        route = MembershipRoute.CompanionLink("a".repeat(40))
        compose.onNodeWithText("Mia Mäzen nimmt dich mit nach Mildcrowd").assertExists()
        route = MembershipRoute.Transparency
        compose.onNodeWithText("Mildcrowd heute").assertExists()
    }
}
