package com.thekeeperofpie.artistalleydatabase.anilist.character

import android.util.Log
import androidx.annotation.Discouraged
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.anilist.fragment.AniListCharacter
import com.squareup.moshi.JsonClass
import com.thekeeperofpie.artistalleydatabase.android_utils.AppJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

@JsonClass(generateAdapter = true)
@Entity("character_entries")
data class CharacterEntry(
    @PrimaryKey
    val id: String,
    @Embedded(prefix = "name_")
    val name: Name? = null,
    @Embedded(prefix = "image_")
    val image: Image? = null,
    val mediaIds: List<String>? = null,
    @Discouraged("Prefer #voiceActors(AppJson)")
    val voiceActors: Map<String, String>? = null
) {

    companion object {
        private const val TAG = "CharacterEntry"
    }

    constructor(
        character: AniListCharacter,
        appJson: AppJson
    ) : this(
        id = character.id.toString(),
        name = character.name?.aniListCharacterName?.run {
            Name(
                first = first?.trim(),
                middle = middle?.trim(),
                last = last?.trim(),
                full = full?.trim(),
                native = native?.trim(),
                alternative = alternative?.filterNotNull()?.map(String::trim),
            )
        },
        image = Image(
            large = character.image?.large,
            medium = character.image?.medium,
        ),
        mediaIds = character.media?.nodes?.mapNotNull { it?.aniListMedia?.id?.toString() },
        voiceActors = CharacterUtils.parseVoiceActors(character)
            .mapValues { (_, value) -> appJson.json.encodeToString<List<VoiceActor>>(value) },
    )

    @Ignore
    @Transient
    @kotlinx.serialization.Transient
    private lateinit var _voiceActors: Map<String, List<VoiceActor>>

    fun voiceActors(appJson: AppJson): Map<String, List<VoiceActor>> {
        if (!::_voiceActors.isInitialized) {
            _voiceActors = voiceActors?.mapNotNull { (mediaId, value) ->
                if (value.startsWith("[")) {
                    try {
                        return@mapNotNull mediaId to
                                appJson.json.decodeFromString<List<VoiceActor>>(value)
                    } catch (e: Exception) {
                        Log.e(TAG, "Fail to parse VoiceActor: $value")
                    }
                }
                null
            }?.associate { it }
                .orEmpty()
        }

        return _voiceActors
    }

    @Serializable
    data class Name(
        val first: String? = null,
        val middle: String? = null,
        val last: String? = null,
        val full: String? = null,
        val native: String? = null,
        val alternative: List<String>? = null,
    )

    @Serializable
    data class Image(
        val large: String? = null,
        val medium: String? = null,
    )

    @Serializable
    data class VoiceActor(
        val id: String,
        val language: String?,
        val name: Name,
        val image: Image?,
    )
}