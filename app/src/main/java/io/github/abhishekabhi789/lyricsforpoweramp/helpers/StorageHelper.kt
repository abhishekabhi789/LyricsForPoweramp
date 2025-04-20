package io.github.abhishekabhi789.lyricsforpoweramp.helpers

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.StringRes
import androidx.documentfile.provider.DocumentFile
import io.github.abhishekabhi789.lyricsforpoweramp.R
import io.github.abhishekabhi789.lyricsforpoweramp.model.LyricsType
import io.github.abhishekabhi789.lyricsforpoweramp.utils.AppPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException

object StorageHelper {
    private const val TAG = "StorageHelper"
    private const val SYNCED_LYRICS_MIMETYPE = "text/lrc"
    private const val PLAIN_LYRICS_MIMETYPE = "text/plain"
    private const val EXTENSION_LRC = ".lrc"
    private const val EXTENSION_TXT = ".txt"

    suspend fun writeLyricsFile(
        context: Context,
        filePath: String,
        lyricsContent: String,
        lyricsType: LyricsType,
    ): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "writeLyricsFile: track file path $filePath")
        if (filePath.isBlank()) return@withContext Result.INVALID_FILEPATH
        val trackParentDir = filePath.substringBeforeLast(File.separator)
        if (lyricsContent.isBlank()) {
            Log.e(TAG, "writeLyricsFile: aborting lyrics write due to null lyrics")
            return@withContext Result.INVALID_LYRICS
        }
        val (mimeType, extension) = if (lyricsType == LyricsType.SYNCED)
            SYNCED_LYRICS_MIMETYPE to EXTENSION_LRC else PLAIN_LYRICS_MIMETYPE to EXTENSION_TXT

        val fileName =
            filePath.substringAfterLast(File.separator).substringBeforeLast(".") + extension

        val savedUris = AppPreference.getSavedUris(context)
        val processedPath = processPath(trackParentDir, savedUris)
        if (processedPath == null) {
            Log.e(TAG, "writeLyricsFile: required folder not found in saved path")
            return@withContext Result.NO_PERMISSION
        }
        val (folderUri, extraPath) = processedPath
        var parentFolder = DocumentFile.fromTreeUri(context, folderUri)
            ?: return@withContext Result.INVALID_FILEPATH

        if (extraPath.isNotEmpty()) {
            //navigate to the child folder where the track is located
            for (segment in extraPath) {
                val childFolder = parentFolder.findFile(segment)
                    ?: run {
                        Log.e(TAG, "writeLyricsFile: folder not found '$segment' from $extraPath")
                        Log.i(TAG, "writeLyricsFile: filePath $filePath, uri $folderUri")
                        return@withContext Result.INVALID_FILEPATH
                    }
                parentFolder = childFolder
            }
        }

        if (!parentFolder.isDirectory) {
            Log.e(TAG, "writeLyricsFile: Failed to create or access child folder")
            return@withContext Result.INVALID_FILEPATH
        }

        return@withContext try {
            parentFolder.findFile(fileName)?.let {
                //to overwrite the existing lyrics file
                if (!it.delete()) {
                    Log.w(TAG, "writeLyricsFile: Failed to delete existing file $fileName")
                }
            }
            val file = parentFolder.createFile(mimeType, fileName)
            if (file == null) {
                Log.e(TAG, "writeLyricsFile: Failed to create file $fileName")
                return@withContext Result.INVALID_FILEPATH
            }

            context.contentResolver.openOutputStream(file.uri)?.use { outputStream ->
                outputStream.write(lyricsContent.toByteArray())
            }
            Result.SUCCESS
        } catch (e: Throwable) {
            Log.e(TAG, "writeTextFile: failed to write lyrics for $filePath", e)
            when (e) {
                is FileNotFoundException -> Result.INVALID_FILEPATH
                is SecurityException -> Result.NO_PERMISSION
                else -> Result.UNKNOWN_ERROR
            }
        }
    }

    /** Processes the track file path and the permitted URIs to identify the working directory,
     * determining any extra path segments.
     * @param parentPath parent path(folder) of the music track.
     * @param savedPaths list of uris saved by the user.
     * @return Pair of Uri and list of sub folder names to that uri path*/
    private fun processPath(parentPath: String, savedPaths: List<Uri>): Pair<Uri, List<String>>? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            //From Android Q poweramp gives path like `primary/Music/Folder/File.mp3`
            savedPaths.find { uri -> parentPath.startsWith(uri.getCleanedPath()) }?.let { uri ->
                //find the extra child depth
                val extraPath = parentPath.removePrefix(uri.getCleanedPath())
                    .trimStart(File.separatorChar).takeIf { it.isNotBlank() }
                    ?.split(File.separatorChar) ?: emptyList()
                uri to extraPath
            }
        } else {
            // Poweramp gives absolute path for devices below Android Q
            val prefix: String
            val relativePath: String
            when {
                //internal storage
                parentPath.startsWith("/storage/emulated/0/") -> {
                    prefix = "primary"
                    relativePath = parentPath.substringAfter("/storage/emulated/0/")
                }
                //sdcard or similar storage
                parentPath.startsWith("/storage/") -> {
                    val storageName =
                        parentPath.substringAfter("/storage/").substringBefore(File.separator)
                    prefix = storageName
                    relativePath =
                        parentPath.substringAfter(storageName).trimStart(File.separatorChar)
                }

                else -> return null
            }
            //restructuring absolute path to compare with savedPath(uri path)
            val restructuredPath = "$prefix${File.separator}$relativePath"
            savedPaths.find { uri -> restructuredPath.startsWith(uri.getCleanedPath()) }
                ?.let { uri ->
                    uri.path?.replace(File.pathSeparatorChar, File.separatorChar)
                        ?.trimEnd(File.separatorChar)?.substringAfterLast(File.separator, "")
                        ?.let { lastSegment ->
                            val extraPath = relativePath.substringAfter(lastSegment)
                                .trimEnd(File.separatorChar).takeIf { it.isNotBlank() }
                                ?.split(File.separatorChar) ?: emptyList()
                            uri to extraPath
                        }
                }
        }
    }


    enum class Result(@StringRes val message: Int) {
        /** Successfully wrote the lyrics file. */
        SUCCESS(R.string.lyrics_write_success),

        /** Missing permission to access the selected folder. */
        NO_PERMISSION(R.string.lyrics_write_no_permission),

        /** Invalid file path. */
        INVALID_FILEPATH(R.string.lyrics_write_invalid_path),

        /** Invalid lyrics content. */
        INVALID_LYRICS(R.string.lyrics_write_invalid_lyrics),

        /** An unknown error occurred while writing the file. */
        UNKNOWN_ERROR(R.string.lyrics_write_unknown_error)
    }
}

/** Returns a cleaner relative path for comparing with a Poweramp URI.
 * @return A path formatted like `primary/Music/AnySubFolder`.
 * Converts the Uri path to match the structure used by Poweramp on Android 29+ devices.
 */
fun Uri.getCleanedPath(): String {
    return path?.removePrefix("/tree/")?.replace(":", File.separator)
        ?.trimEnd(File.separatorChar) ?: ""
}

/**check whether the uri has persisted write permission*/
fun Uri.hasAccess(context: Context): Boolean {
    return context.contentResolver.persistedUriPermissions
        .any { it.uri == this && it.isWritePermission }
}
