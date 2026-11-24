package de.velwebsolutions.mildcrowd.design.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import de.velwebsolutions.mildcrowd.core.format.GermanTime
import de.velwebsolutions.mildcrowd.core.format.RelativeTime
import de.velwebsolutions.mildcrowd.core.network.ApiClient
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdColors
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdType
import java.time.Instant

/** The API client of the running app, to resolve image URLs the API returns (null in previews). */
val LocalApiClient = staticCompositionLocalOf<ApiClient?> { null }

/** The colours members and crowds choose (`AvatarColor` on the web: sun, spring, night, dune). */
object AvatarTint {
    /** The circle behind the initials (`AvatarColor::classes`), where dune is the darker dune line. */
    fun background(name: String): Color = when (name) {
        "sun" -> MildcrowdColors.Sun
        "spring" -> MildcrowdColors.Spring
        "night" -> MildcrowdColors.Night
        else -> MildcrowdColors.DuneLine
    }

    fun foreground(name: String): Color = when (name) {
        "sun" -> MildcrowdColors.SunInk
        "spring" -> MildcrowdColors.SpringInk
        "night" -> MildcrowdColors.OnNight
        else -> MildcrowdColors.Night
    }

    /** „Automatisch“ in the colour choice of initials (`avatar_color` "auto"): the server resolves it per member. */
    const val AUTOMATIC = "auto"

    /** The palette in the server's order: „Automatisch“ is `KEYS[id % 4]`. */
    val KEYS = listOf("sun", "spring", "night", "dune")

    /** The colour „Automatisch“ resolves to for member [memberId], like the server. */
    fun automatic(memberId: Int): String = KEYS[Math.floorMod(memberId, KEYS.size)]

    /** The colour a choice shows for member [memberId]: „Automatisch“ shows the colour it resolves to. */
    fun shown(choice: String, memberId: Int): String = if (choice == AUTOMATIC) automatic(memberId) else choice

    /** A crowd colour surface (`bg-{color}`), where dune is the light sand tone. */
    fun surface(name: String): Color = if (name == "dune") MildcrowdColors.Dune else background(name)
}

/**
 * A round avatar like `x-network.avatar`: the photo where the server sends one, otherwise the initials in the colour the
 * member chose. Decorative – the name is always written next to it.
 */
@Composable
fun AvatarView(initials: String, color: String, modifier: Modifier = Modifier, photoUrl: String? = null, size: Dp = 44.dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(if (photoUrl == null) AvatarTint.background(color) else MildcrowdColors.Dune)
            .clearAndSetSemantics {},
        contentAlignment = Alignment.Center
    ) {
        val fontSize = with(LocalDensity.current) { (size / 3).toSp() }
        Text(initials, style = MildcrowdType.initials(fontSize), color = AvatarTint.foreground(color))
        if (photoUrl != null) {
            AuthorizedImage(
                photoUrl,
                contentDescription = null,
                modifier = Modifier.size(size).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }
    }
}

/**
 * Loads an image with the member's token (Post images, banners, profile photos with restricted audience); accepts
 * absolute or root-relative URLs. The app's image loader shares the API client's OkHttp, so the headers are the same.
 * Its disk cache keeps what it loads, whatever `Cache-Control` says: [cacheOnDisk] false keeps an image the server sends
 * with `no-store` (the photos of a reported chat message) off the device's storage, only in memory.
 */
@Composable
fun AuthorizedImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    cacheOnDisk: Boolean = true
) {
    val client = LocalApiClient.current
    val absolute = client?.absoluteUrl(url)?.toString() ?: url
    SubcomposeAsyncImage(
        model = imageModel(LocalContext.current, absolute, cacheOnDisk),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        error = {
            Box(Modifier.background(MildcrowdColors.Dune), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Image, contentDescription = null, tint = MildcrowdColors.Muted)
            }
        }
    )
}

/** What [AuthorizedImage] asks the image loader for: the URL itself, or a request that never reads or writes the disk cache. */
internal fun imageModel(context: Context, url: String, cacheOnDisk: Boolean): Any =
    if (cacheOnDisk) url else ImageRequest.Builder(context).data(url).diskCachePolicy(CachePolicy.DISABLED).build()

/** "vor 5 Min." / "vor 3 Tagen" / "12. Okt." – relative time in German like the web's timestamps ([RelativeTime]). */
@Composable
fun RelativeTimeText(
    instant: Instant,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MildcrowdType.Caption,
    color: Color = MildcrowdColors.Muted
) {
    Text(
        RelativeTime.format(instant),
        style = style,
        color = color,
        modifier = modifier.semantics { contentDescription = GermanTime.format(instant, "d. MMMM yyyy, HH:mm") }
    )
}
