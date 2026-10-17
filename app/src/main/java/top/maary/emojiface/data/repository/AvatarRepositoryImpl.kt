package top.maary.emojiface.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.provider.OpenableColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import top.maary.emojiface.datastore.PreferenceRepository
import top.maary.emojiface.domain.model.Avatar
import top.maary.emojiface.domain.repository.AvatarRepository
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

/**
 * Concrete implementation of [AvatarRepository] handling internal storage persistence,
 * 1:1 square minimal-loss cropping, downscaled decoding, and DataStore synchronization.
 */
@Singleton
class AvatarRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val preferenceRepository: PreferenceRepository
) : AvatarRepository {

    companion object {
        const val MAX_AVATARS_COUNT = 50
        const val MAX_FILE_SIZE_BYTES = 15 * 1024 * 1024L // 15MB
        const val MAX_IMPORT_DIMENSION = 1536
        const val THUMBNAIL_SIZE = 160
        private const val AVATARS_DIR_NAME = "avatars"
    }

    private val avatarsDir: File
        get() = File(context.filesDir, AVATARS_DIR_NAME).apply {
            if (!exists()) {
                mkdirs()
            }
        }

    override val avatarsFlow: Flow<List<Avatar>> = preferenceRepository.avatarsMetadataFlow

    override suspend fun getAvatars(): List<Avatar> = preferenceRepository.avatarsMetadataFlow.first()

    override suspend fun getAvatar(id: String): Avatar? = getAvatars().find { it.id == id }

    override suspend fun getAvatarFile(id: String): File? = withContext(Dispatchers.IO) {
        val avatar = getAvatar(id) ?: return@withContext null
        val file = avatar.resolveFile(context.filesDir)
        if (file.exists()) file else null
    }

    override suspend fun getTotalStorageUsed(): Long = withContext(Dispatchers.IO) {
        if (!avatarsDir.exists() || !avatarsDir.isDirectory) return@withContext 0L
        avatarsDir.listFiles()?.filter { it.isFile }?.sumOf { it.length() } ?: 0L
    }

    override suspend fun importAvatar(uri: Uri, customDisplayName: String?): Result<Avatar> =
        withContext(Dispatchers.IO) {
            runCatching {
                // 1. Check max avatar count
                val currentAvatars = getAvatars()
                if (currentAvatars.size >= MAX_AVATARS_COUNT) {
                    throw IllegalStateException("Maximum avatar limit of $MAX_AVATARS_COUNT reached.")
                }

                // 2. Validate file size if known from content resolver
                val fileSize = runCatching {
                    context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize }
                }.getOrNull() ?: -1L
                if (fileSize > MAX_FILE_SIZE_BYTES) {
                    throw IllegalArgumentException("File size exceeds maximum allowed limit of 15MB.")
                }

                // 3. Open single source using modern ImageDecoder (minSdk=30)
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                val fullBitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.isMutableRequired = true
                    val maxDim = max(info.size.width, info.size.height)
                    if (maxDim > MAX_IMPORT_DIMENSION) {
                        var sampleSize = 1
                        while (maxDim / (sampleSize * 2) >= MAX_IMPORT_DIMENSION) {
                            sampleSize *= 2
                        }
                        decoder.setTargetSampleSize(sampleSize)
                    }
                }

                // 4. Compute 1:1 minimal-loss square crop
                val crop = Avatar.calculateSquareCrop(fullBitmap.width, fullBitmap.height)
                var croppedBitmap = Bitmap.createBitmap(
                    fullBitmap,
                    crop.x,
                    crop.y,
                    crop.size,
                    crop.size
                )
                if (croppedBitmap != fullBitmap) {
                    fullBitmap.recycle()
                }

                // 5. Ensure max size <= 1536px
                if (croppedBitmap.width > MAX_IMPORT_DIMENSION || croppedBitmap.height > MAX_IMPORT_DIMENSION) {
                    val scaled = Bitmap.createScaledBitmap(
                        croppedBitmap,
                        MAX_IMPORT_DIMENSION,
                        MAX_IMPORT_DIMENSION,
                        true
                    )
                    if (scaled != croppedBitmap) {
                        croppedBitmap.recycle()
                        croppedBitmap = scaled
                    }
                }

                // 6. Ensure config is ARGB_8888 to preserve transparency
                if (croppedBitmap.config != Bitmap.Config.ARGB_8888) {
                    val argbBitmap = croppedBitmap.copy(Bitmap.Config.ARGB_8888, true)
                    croppedBitmap.recycle()
                    croppedBitmap = argbBitmap
                }

                val finalWidth = croppedBitmap.width
                val finalHeight = croppedBitmap.height

                // 7. Generate IDs and file targets
                val id = UUID.randomUUID().toString()
                val timestamp = System.currentTimeMillis()
                val fileName = "avatar_${timestamp}_${id}.png"
                val relativePath = "$AVATARS_DIR_NAME/$fileName"
                val tmpFile = File(avatarsDir, "$fileName.tmp")
                val finalFile = File(avatarsDir, fileName)

                try {
                    // 8. Write to temporary file with lossless PNG compression
                    FileOutputStream(tmpFile).use { outStream ->
                        croppedBitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream)
                        outStream.flush()
                    }

                    // 9. Atomic rename to final file
                    val renamed = tmpFile.renameTo(finalFile)
                    if (!renamed) {
                        tmpFile.copyTo(finalFile, overwrite = true)
                        tmpFile.delete()
                    }
                } catch (e: Exception) {
                    if (tmpFile.exists()) tmpFile.delete()
                    if (finalFile.exists()) finalFile.delete()
                    throw e
                } finally {
                    croppedBitmap.recycle()
                }

                // 9. Determine display name
                val resolvedDisplayName = customDisplayName?.takeIf { it.isNotBlank() }
                    ?: queryDisplayName(uri)
                    ?: "Avatar ${currentAvatars.size + 1}"

                // 10. Construct metadata
                val avatar = Avatar(
                    id = id,
                    fileName = fileName,
                    relativePath = relativePath,
                    displayName = resolvedDisplayName,
                    dateAdded = timestamp,
                    width = finalWidth,
                    height = finalHeight,
                    mimeType = "image/png",
                    sizeBytes = finalFile.length()
                )

                // 11. Update DataStore metadata
                val updatedAvatars = currentAvatars + avatar
                preferenceRepository.updateAvatarsMetadata(updatedAvatars)

                // 12. If no avatar was selected previously, set this imported avatar as default
                val currentSelected = preferenceRepository.selectedDefaultAvatarIdFlow.first()
                if (currentSelected == PreferenceRepository.DEFAULT_AVATAR_ID ||
                    currentAvatars.none { it.id == currentSelected }
                ) {
                    preferenceRepository.setSelectedDefaultAvatarId(avatar.id)
                }

                avatar
            }
        }

    override suspend fun deleteAvatar(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val currentAvatars = getAvatars()
            val avatar = currentAvatars.find { it.id == id }
                ?: return@runCatching

            // 1. Delete physical file
            val file = avatar.resolveFile(context.filesDir)
            if (file.exists()) {
                file.delete()
            }

            // 2. Update DataStore metadata
            val remainingAvatars = currentAvatars.filterNot { it.id == id }
            preferenceRepository.updateAvatarsMetadata(remainingAvatars)

            // 3. Fallback selected avatar if needed
            val currentSelected = preferenceRepository.selectedDefaultAvatarIdFlow.first()
            if (currentSelected == id) {
                val nextSelectedId = remainingAvatars.firstOrNull()?.id ?: PreferenceRepository.DEFAULT_AVATAR_ID
                preferenceRepository.setSelectedDefaultAvatarId(nextSelectedId)
            }
        }
    }

    override suspend fun loadAvatarBitmap(id: String, targetSize: Int?): Result<Bitmap> =
        withContext(Dispatchers.IO) {
            runCatching {
                val file = getAvatarFile(id)
                    ?: throw FileNotFoundException("Avatar file for id $id not found on disk")

                if (targetSize != null && targetSize > 0) {
                    val boundsOptions = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    BitmapFactory.decodeFile(file.absolutePath, boundsOptions)

                    val maxDim = max(boundsOptions.outWidth, boundsOptions.outHeight)
                    var sampleSize = 1
                    while (maxDim / (sampleSize * 2) >= targetSize) {
                        sampleSize *= 2
                    }

                    val decodeOptions = BitmapFactory.Options().apply {
                        inSampleSize = sampleSize
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    }
                    val decoded = BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
                        ?: throw IOException("Failed to decode avatar bitmap for id: $id")

                    if (decoded.width > targetSize || decoded.height > targetSize) {
                        val scaled = Bitmap.createScaledBitmap(decoded, targetSize, targetSize, true)
                        if (scaled != decoded) {
                            decoded.recycle()
                        }
                        scaled
                    } else {
                        decoded
                    }
                } else {
                    val decodeOptions = BitmapFactory.Options().apply {
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    }
                    BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
                        ?: throw IOException("Failed to decode avatar bitmap for id: $id")
                }
            }
        }

    override suspend fun loadThumbnail(id: String, size: Int): Result<Bitmap> {
        return loadAvatarBitmap(id, size)
    }

    override suspend fun reconcileStorage(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val diskFiles = avatarsDir.listFiles()?.filter { it.isFile } ?: emptyList()
            val currentAvatars = getAvatars()
            val validFileNames = currentAvatars.map { it.fileName }.toSet()

            // 1. Delete orphan files and leftover temporary files (.tmp)
            diskFiles.forEach { file ->
                if (file.name.endsWith(".tmp") || file.name !in validFileNames) {
                    file.delete()
                }
            }

            // 2. Prune metadata for missing files
            val remainingDiskNames = (avatarsDir.listFiles()?.filter { it.isFile } ?: emptyList())
                .map { it.name }
                .toSet()
            val reconciledList = currentAvatars.filter { it.fileName in remainingDiskNames }

            if (reconciledList.size != currentAvatars.size) {
                preferenceRepository.updateAvatarsMetadata(reconciledList)

                val currentSelected = preferenceRepository.selectedDefaultAvatarIdFlow.first()
                if (currentSelected != PreferenceRepository.DEFAULT_AVATAR_ID &&
                    reconciledList.none { it.id == currentSelected }
                ) {
                    val nextSelectedId = reconciledList.firstOrNull()?.id ?: PreferenceRepository.DEFAULT_AVATAR_ID
                    preferenceRepository.setSelectedDefaultAvatarId(nextSelectedId)
                }
            }
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        val nameFromQuery = runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1 && !cursor.isNull(index)) {
                        cursor.getString(index)
                    } else null
                } else null
            }
        }.getOrNull()

        val rawName = nameFromQuery ?: uri.lastPathSegment
        return rawName?.substringAfterLast('/')?.substringBeforeLast('.')?.takeIf { it.isNotBlank() }
    }
}
