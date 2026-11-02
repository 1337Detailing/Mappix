package io.github.mappix.data.repository

import io.github.mappix.data.local.ProfileDao
import io.github.mappix.data.local.ProfileEntity
import io.github.mappix.domain.model.Mapping
import io.github.mappix.domain.model.Profile
import io.github.mappix.domain.model.ProfileExport
import io.github.mappix.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject

class RoomProfileRepository @Inject constructor(private val dao: ProfileDao, private val json: Json) : ProfileRepository {
    override fun observeAll(): Flow<List<Profile>> = dao.observeAll().map { it.mapNotNull(::decodeSafely) }
    override fun observe(id: String): Flow<Profile?> = dao.observe(id).map { it?.let(::decodeSafely) }
    override suspend fun get(id: String): Profile? = dao.get(id)?.let(::decodeSafely)

    override suspend fun save(profile: Profile) {
        require(profile.name.isNotBlank() && profile.name.length <= 80) { "Invalid profile name" }
        require(profile.mappings.size <= 250) { "Too many mappings" }
        dao.upsert(ProfileEntity(
            profile.id, profile.name.trim(), profile.packageName?.trim()?.takeIf(String::isNotEmpty),
            json.encodeToString(ListSerializer(Mapping.serializer()), profile.mappings), System.currentTimeMillis()
        ))
    }

    override suspend fun delete(id: String) = dao.delete(id)

    override suspend fun duplicate(id: String): Profile? {
        val base = get(id) ?: return null
        val duplicate = base.copy(id = UUID.randomUUID().toString(), name = "${base.name} (copy)", packageName = null)
        save(duplicate)
        return duplicate
    }

    override fun exportJson(profile: Profile): String = json.encodeToString(ProfileExport.serializer(), ProfileExport(profile = profile))

    override suspend fun importJson(jsonText: String): Profile {
        require(jsonText.length < 512_000) { "File too large" }
        val envelope = json.decodeFromString(ProfileExport.serializer(), jsonText)
        require(envelope.schemaVersion == 1) { "Unsupported schema version" }
        require(envelope.profile.name.isNotBlank()) { "Missing profile name" }
        require(envelope.profile.mappings.size <= 250) { "Too many mappings" }
        require(envelope.profile.name.length <= 80) { "Profile name too long" }
        require(envelope.profile.mappings.all { mapping ->
            mapping.deadzone.isFinite() && mapping.deadzone in 0f..0.95f &&
            mapping.sensitivity.isFinite() && mapping.sensitivity in 0.1f..3f &&
            when (val action = mapping.output) {
                is io.github.mappix.domain.model.OutputAction.Tap ->
                    action.x.isFinite() && action.y.isFinite() &&
                    action.x in 0f..1f && action.y in 0f..1f && action.durationMs in 1L..1000L
                else -> true
            }
        }) { "Invalid mappings" }
        val imported = envelope.profile.copy(
            id = UUID.randomUUID().toString(),
            mappings = envelope.profile.mappings.map { it.copy(id = UUID.randomUUID().toString()) }
        )
        save(imported)
        return imported
    }

    private fun decodeSafely(entity: ProfileEntity): Profile? = runCatching {
        Profile(entity.id, entity.name, entity.packageName,
            json.decodeFromString(ListSerializer(Mapping.serializer()), entity.mappingsJson), entity.updatedAt)
    }.getOrNull()
}
