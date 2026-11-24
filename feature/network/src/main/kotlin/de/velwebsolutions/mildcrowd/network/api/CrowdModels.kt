package de.velwebsolutions.mildcrowd.network.api

import de.velwebsolutions.mildcrowd.core.network.MildcrowdInstant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

/** The form choices for founding and editing a crowd. */
@Serializable
data class CrowdOptions(
    val visibilities: List<Choice> = emptyList(),
    val colors: List<Choice> = emptyList(),
    /** The Bundesländer for „Region“ (none from a server without the Karte: the field is not shown). */
    val regions: List<Choice> = emptyList()
)

/** "Crowds": own crowds and all others. */
@Serializable
data class CrowdDirectory(
    val mine: List<CrowdSummary> = emptyList(),
    val others: List<CrowdSummary> = emptyList(),
    val canCreate: Boolean = false,
    val options: CrowdOptions = CrowdOptions()
)

/** A crowd page without its Posts. */
@Serializable
data class CrowdDetail(
    val crowd: CrowdSummary,
    val url: String,
    val houseRules: List<String> = emptyList(),
    val hosts: List<Member> = emptyList(),
    val isMember: Boolean = false,
    val isHost: Boolean = false,
    val canFollow: Boolean = false,
    val canPost: Boolean = false,
    /** „Mitglieder dürfen Treffen anlegen“. */
    val membersCreateMeetups: Boolean = false,
    /** Willkommenspaten (null for non-members). */
    val welcome: CrowdWelcome? = null
)

/** The member list of a crowd, for its members. */
@Serializable
data class CrowdMembers(
    val members: List<Entry> = emptyList(),
    val hiddenMembers: Int = 0,
    /** The viewer's own list audience when it limits them: "contacts" or "nobody". */
    val ownListAudience: String? = null
) {
    @Serializable
    data class Entry(
        val id: Int,
        val username: String,
        val displayName: String,
        val initials: String,
        val avatarColor: String = "sun",
        val photoUrl: String? = null,
        val canWrite: Boolean = false
    ) {
        val member: Member get() = Member(id, username, displayName, initials, avatarColor, photoUrl)
    }
}

/** The edit form of a crowd for hosts. */
@Serializable
data class CrowdEditForm(
    val crowd: CrowdSummary,
    val houseRules: String = "",
    val members: List<MemberRole> = emptyList(),
    val options: CrowdOptions = CrowdOptions(),
    /** How many other members chose not to appear to this host (`hidden_members`, CrowdMemberList::hiddenFromHost). */
    val hiddenMembers: Int = 0
) {
    /** The note under the role list while members are hidden (crowd-form.blade.php); null when none is. */
    val hiddenMembersNote: String?
        get() = when (hiddenMembers) {
            0 -> null

            1 -> "Ein weiteres Mitglied zeigt sich hier nur Freund:innen und Menschen, denen es folgt, oder niemandem. $ROLE_ONLY_VISIBLE"

            else ->
                "$hiddenMembers weitere Mitglieder zeigen sich hier nur Freund:innen und Menschen, denen sie folgen, oder niemandem. " +
                    ROLE_ONLY_VISIBLE
        }

    @Serializable
    data class MemberRole(val id: Int, val username: String, val displayName: String, val role: String, val roleLabel: String)
}

/** The end of [CrowdEditForm.hiddenMembersNote]. */
private const val ROLE_ONLY_VISIBLE = "Eine Rolle kannst du nur Mitgliedern geben, die du hier siehst."

/** The input of the crowd form. */
@Serializable
data class CrowdInput(
    val name: String = "",
    val description: String = "",
    val houseRules: String = "",
    val visibility: String = "public",
    val color: String = "spring",
    /** The Bundesland code (`BY`), null for none. */
    val region: String? = null,
    /** A city or area (at most 60), only with a region. */
    val city: String = ""
) {
    /** The multipart fields when founding (with the optional banner); region and city only with a region. */
    val fields: List<Pair<String, String>>
        get() = listOf(
            "name" to name,
            "description" to description,
            "house_rules" to houseRules,
            "visibility" to visibility,
            "color" to color
        ) + region?.let { listOf("region" to it, "city" to city) }.orEmpty()

    /** What saving an edited crowd sends: `region` also as null, since leaving it out would keep the old one. */
    internal val update: CrowdUpdate
        get() = CrowdUpdate(
            name,
            description,
            houseRules,
            visibility,
            color,
            region?.let(::JsonPrimitive) ?: JsonNull,
            region?.let {
                city
            }.orEmpty()
        )
}

/** The body of `PUT crowds/{slug}`. */
@Serializable
internal data class CrowdUpdate(
    val name: String,
    val description: String,
    val houseRules: String,
    val visibility: String,
    val color: String,
    val region: JsonElement,
    val city: String
)

/** One tile of the Karte der Crowds (`GET network/regions`): the Bundesland, its visible crowds and its grid cell. */
@Serializable
data class MapRegion(val code: String, val name: String, val crowdsCount: Int = 0, val tile: Tile) {
    @Serializable
    data class Tile(val x: Int, val y: Int)

    /** „Bayern: 3 Crowds“. */
    val accessibleName: String get() = "$name: ${if (crowdsCount == 1) "1 Crowd" else "$crowdsCount Crowds"}"
}

/** The confirmation page before closing a crowd. */
@Serializable
data class CrowdClosing(val name: String, val membersCount: Int = 0, val postsCount: Int = 0)

/** Host statistics of a crowd. */
@Serializable
data class CrowdStatistics(val name: String, val totals: Totals, val weekly: List<Week> = emptyList()) {
    @Serializable
    data class Totals(
        val members: Int = 0,
        val hosts: Int = 0,
        val posts: Int = 0,
        @SerialName("posts_30_days") val posts30Days: Int = 0,
        @SerialName("active_authors_30_days") val activeAuthors30Days: Int = 0,
        val removed: Int = 0
    )

    @Serializable
    data class Week(val week: MildcrowdInstant, val joined: Int = 0, val posts: Int = 0)
}

/** The slug of a founded or saved crowd. */
@Serializable
data class CrowdSlug(val slug: String)

/** After changing a role: whether the acting member is still a host (the last change may have been their own). */
@Serializable
data class RoleChange(val stillHost: Boolean = true)
