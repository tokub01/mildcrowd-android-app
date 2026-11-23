package de.velwebsolutions.mildcrowd.network.api

import de.velwebsolutions.mildcrowd.core.network.MildcrowdInstant
import kotlinx.serialization.Serializable

/**
 * A meetup of a crowd (docs/mildcrowd-network.md, „Treffen“), for its members only: when and where, whether it was
 * cancelled or is over, the answers („Ich komme“, „Vielleicht“, „Nein“) with who gave them, and what the viewer may do.
 */
@Serializable
data class Meetup(
    val id: Int,
    val crowd: CrowdRef,
    val title: String,
    val startsAt: MildcrowdInstant,
    val place: String? = null,
    val link: String? = null,
    val description: String? = null,
    val cancelled: Boolean = false,
    val past: Boolean = false,
    val counts: Counts = Counts(),
    val myAnswer: String? = null,
    val people: List<Person> = emptyList(),
    val canAnswer: Boolean = false,
    val canEdit: Boolean = false
) {
    @Serializable
    data class CrowdRef(val slug: String, val name: String)

    @Serializable
    data class Counts(val yes: Int = 0, val maybe: Int = 0, val no: Int = 0)

    @Serializable
    data class Person(val answer: String, val member: Member)

    companion object {
        /** The answers in their order (`Meetup::ANSWERS`). */
        val ANSWERS = listOf("yes" to "Ich komme", "maybe" to "Vielleicht", "no" to "Nein")

        fun answerLabel(answer: String): String = ANSWERS.firstOrNull { it.first == answer }?.second ?: answer
    }
}

/** The meetups of a crowd: who may create one, the coming ones (soonest first) and the last five past ones. */
@Serializable
data class CrowdMeetups(
    val canCreate: Boolean = false,
    val membersCreateMeetups: Boolean = false,
    val upcoming: List<Meetup> = emptyList(),
    val past: List<Meetup> = emptyList()
)

/** What a new or changed meetup sends (all fields, empty ones as ""). */
@Serializable
data class MeetupInput(
    val title: String = "",
    val startsAt: String = "",
    val place: String = "",
    val link: String = "",
    val description: String = ""
)

/** The Willkommenspate of the viewer, whether they volunteer, and – for hosts – the volunteers. */
@Serializable
data class CrowdWelcome(val pate: Member? = null, val volunteer: Boolean = false, val volunteers: List<Member>? = null)

@Serializable
internal data class MeetupAnswerBody(val answer: String)

/** „Mitglieder dürfen Treffen anlegen“ as stored. */
@Serializable
data class MeetupSettings(val membersCreateMeetups: Boolean)

/** „Ich bin gern Willkommenspate oder -patin“ as stored. */
@Serializable
data class WelcomeVolunteer(val volunteer: Boolean)
