package io.github.mappix.di

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.mappix.data.local.MappixDatabase
import io.github.mappix.data.local.ProfileDao
import io.github.mappix.data.repository.RoomProfileRepository
import io.github.mappix.domain.repository.ProfileRepository
import io.github.mappix.domain.repository.ControllerRepository
import io.github.mappix.input.AndroidControllerRepository
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun database(@ApplicationContext context: Context): MappixDatabase =
        Room.databaseBuilder(context, MappixDatabase::class.java, "mappix.db").build()

    @Provides fun dao(db: MappixDatabase): ProfileDao = db.profiles()

    @Provides @Singleton
    fun json(): Json = Json { ignoreUnknownKeys = true; prettyPrint = true; classDiscriminator = "type" }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton abstract fun profiles(repository: RoomProfileRepository): ProfileRepository
    @Binds @Singleton abstract fun controllers(repository: AndroidControllerRepository): ControllerRepository
}
