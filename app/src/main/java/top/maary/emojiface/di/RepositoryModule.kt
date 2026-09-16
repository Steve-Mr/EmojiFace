package top.maary.emojiface.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import top.maary.emojiface.data.repository.AvatarRepositoryImpl
import top.maary.emojiface.domain.repository.AvatarRepository
import javax.inject.Singleton

/**
 * Hilt module binding repository implementations to their domain interfaces.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAvatarRepository(impl: AvatarRepositoryImpl): AvatarRepository
}
