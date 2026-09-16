package top.maary.emojiface.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import top.maary.emojiface.datastore.PreferenceRepository
import top.maary.emojiface.domain.model.Avatar
import top.maary.emojiface.domain.repository.AvatarRepository
import top.maary.emojiface.util.ExifUtils
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

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

                // 3. Decode raw image bounds
                val boundsOptions = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, boundsOptions)
                } ?: throw IOException("Unable to open input stream for URI: $uri")

                val rawWidth = boundsOptions.outWidth
                val rawHeight = boundsOptions.outHeight
                if (rawWidth <= 0 || rawHeight <= 0) {
                    throw IllegalArgumentException("Invalid image dimensions (${rawWidth}x$rawHeight)")
                }

                // 4. Calculate 1:1 square crop coordinates
                val crop = Avatar.calculateSquareCrop(rawWidth, rawHeight)

                // 5. Decode cropped 1:1 bitmap with safe memory bounds & transparency preservation
                val finalBitmap = decodeCroppedBitmap(uri, crop)
                    ?: throw IOException("Failed to decode cropped avatar bitmap from URI: $uri")

                val finalWidth = finalBitmap.width
                val finalHeight = finalBitmap.height

                // 6. Generate IDs and file targets
                val id = UUID.randomUUID().toString()
                val timestamp = System.currentTimeMillis()
                val fileName = "avatar_${timestamp}_${id}.png"
                val relativePath = "$AVATARS_DIR_NAME/$fileName"
                val tmpFile = File(avatarsDir, "$fileName.tmp")
                val finalFile = File(avatarsDir, fileName)

                try {
                    // 7. Write to temporary file with lossless PNG compression
                    FileOutputStream(tmpFile).use { outStream ->
                        finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream)
                        outStream.flush()
                    }

                    // 8. Atomic rename to final file
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
                    finalBitmap.recycle()
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

    /**
     * Decodes a 1:1 square centered crop safely with bounded memory usage.
     */
    private fun decodeCroppedBitmap(uri: Uri, crop: Avatar.CropCoordinates): Bitmap? {
        val s = crop.size
        var sampleSize = 1
        while (s / (sampleSize * 2) >= MAX_IMPORT_DIMENSION) {
            sampleSize *= 2
        }

        // Attempt 1: Region Decoder
        val regionResult = runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val decoder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    BitmapRegionDecoder.newInstance(stream)
                } else {
                    @Suppress("DEPRECATION")
                    BitmapRegionDecoder.newInstance(stream, false)
                }
                val regionOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                val cropRect = Rect(crop.x, crop.y, crop.x + crop.size, crop.y + crop.size)
                val decoded = decoder?.decodeRegion(cropRect, regionOptions)
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                    @Suppress("DEPRECATION")
                    decoder?.recycle()
                }
                decoded
            }
        }.getOrNull()

        var bitmap = regionResult

        // Attempt 2: Fallback to downsampled full decode + center crop
        if (bitmap == null) {
            val fullOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val fullBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, fullOptions)
            } ?: return null

            val rotatedFull = ExifUtils.handleImageOrientation(context, uri, fullBitmap)
            val fullCrop = Avatar.calculateSquareCrop(rotatedFull.width, rotatedFull.height)
            bitmap = Bitmap.createBitmap(
                rotatedFull,
                fullCrop.x,
                fullCrop.y,
                fullCrop.size,
                fullCrop.size
            )
            if (bitmap != rotatedFull) {
                rotatedFull.recycle()
            }
        } else {
            // Apply EXIF rotation if needed on region-decoded image
            bitmap = ExifUtils.handleImageOrientation(context, uri, bitmap)
        }

        // Downscale to MAX_IMPORT_DIMENSION if still oversized
        if (bitmap.width > MAX_IMPORT_DIMENSION || bitmap.height > MAX_IMPORT_DIMENSION) {
            val scaled = Bitmap.createScaledBitmap(
                bitmap,
                MAX_IMPORT_DIMENSION,
                MAX_IMPORT_DIMENSION,
                true
            )
            if (scaled != bitmap) {
                bitmap.recycle()
                bitmap = scaled
            }
        }

        // Ensure ARGB_8888 config to preserve transparency
        if (bitmap.config != Bitmap.Config.ARGB_8888) {
            val argbBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            bitmap.recycle()
            bitmap = argbBitmap
        }

        return bitmap
    }

    private fun queryDisplayName(uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        val nameWithExt = cursor.getString(index)
                        nameWithExt?.substringBeforeLast('.')
                    } else null
                } else null
            }
        }.getOrNull()
    }
}
