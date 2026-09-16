package top.maary.emojiface.domain.repository

import android.graphics.Bitmap
import android.net.Uri
import kotlinx.coroutines.flow.Flow
import top.maary.emojiface.domain.model.Avatar
import java.io.File

/**
 * Repository interface for managing user-imported avatar assets and their storage.
 */
interface AvatarRepository {

    /**
     * Hot Flow emitting current list of registered avatars from persistence.
     */
    val avatarsFlow: Flow<List<Avatar>>

    /**
     * Retrieves current snapshot of all registered avatars.
     */
    suspend fun getAvatars(): List<Avatar>

    /**
     * Finds an avatar by its unique identifier, or null if not found.
     */
    suspend fun getAvatar(id: String): Avatar?

    /**
     * Imports an avatar image from [uri], applies 1:1 minimal-loss square center cropping,
     * stores it in the private app storage as a lossless PNG, and registers its metadata.
     *
     * @param uri URI pointing to the image source.
     * @param customDisplayName Optional human-readable display name.
     * @return Result containing the imported [Avatar], or error details upon failure.
     */
    suspend fun importAvatar(uri: Uri, customDisplayName: String? = null): Result<Avatar>

    /**
     * Deletes an avatar asset from disk and unregisters its metadata from DataStore.
     * If the deleted avatar was currently selected as default, falls back to the next available avatar.
     */
    suspend fun deleteAvatar(id: String): Result<Unit>

    /**
     * Returns the physical File handle on disk for the given avatar [id], or null if missing.
     */
    suspend fun getAvatarFile(id: String): File?

    /**
     * Loads the avatar bitmap with safe bounded decoding and memory management.
     *
     * @param id The avatar identifier.
     * @param targetSize Optional maximum bounding dimension (width/height). If provided, applies downsampling.
     */
    suspend fun loadAvatarBitmap(id: String, targetSize: Int? = null): Result<Bitmap>

    /**
     * Loads a thumbnail bitmap for the specified avatar, downscaled to [size] (defaults to 160px).
     */
    suspend fun loadThumbnail(id: String, size: Int = 160): Result<Bitmap>

    /**
     * Returns the total storage used by all stored avatar files on disk, in bytes.
     */
    suspend fun getTotalStorageUsed(): Long

    /**
     * Reconciles disk files against metadata: removes orphaned files on disk and prunes
     * missing records from metadata, maintaining persistence consistency.
     */
    suspend fun reconcileStorage(): Result<Unit>
}
