package io.github.mappix.domain.repository

import io.github.mappix.domain.model.ControllerEvent
import io.github.mappix.domain.model.ControllerInfo
import io.github.mappix.domain.model.Profile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface ProfileRepository {
    fun observeAll(): Flow<List<Profile>>
    fun observe(id: String): Flow<Profile?>
    suspend fun get(id: String): Profile?
    suspend fun save(profile: Profile)
    suspend fun delete(id: String)
    suspend fun duplicate(id: String): Profile?
    fun exportJson(profile: Profile): String
    suspend fun importJson(json: String): Profile
}

interface ControllerRepository {
    val controllers: StateFlow<List<ControllerInfo>>
    val events: SharedFlow<ControllerEvent>
    fun publish(event: ControllerEvent)
}
