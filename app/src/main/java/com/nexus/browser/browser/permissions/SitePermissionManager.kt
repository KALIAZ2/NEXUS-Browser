package com.nexus.browser.browser.permissions

import android.net.Uri
import com.nexus.browser.core.database.SitePermissionDao
import com.nexus.browser.core.database.SitePermissionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SitePermissionManager @Inject constructor(
    private val dao: SitePermissionDao
) : GeckoSession.PermissionDelegate {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun observePermissions(): Flow<List<SitePermissionEntity>> = dao.observeAll()

    suspend fun setPermission(origin: String, permissionType: String, granted: Boolean) {
        val cleanOrigin = normalizeOrigin(origin)
        val existing = dao.findPermission(cleanOrigin, permissionType)
        dao.upsert(
            SitePermissionEntity(
                id = existing?.id ?: 0L,
                origin = cleanOrigin,
                permissionType = permissionType,
                granted = granted,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun togglePermission(id: Long, granted: Boolean) {
        dao.updateGranted(id, granted)
    }

    suspend fun deletePermission(id: Long) {
        dao.deleteById(id)
    }

    suspend fun clearAllPermissions() {
        dao.clearAll()
    }

    override fun onContentPermissionRequest(
        session: GeckoSession,
        perm: GeckoSession.PermissionDelegate.ContentPermission
    ): GeckoResult<Int>? {
        val result = GeckoResult<Int>()
        val origin = runCatching { Uri.parse(perm.uri).host ?: perm.uri }.getOrDefault(perm.uri)
        val permName = mapPermissionType(perm.permission)

        scope.launch {
            val existing = dao.findPermission(origin, permName)
            if (existing != null) {
                result.complete(
                    if (existing.granted) {
                        GeckoSession.PermissionDelegate.ContentPermission.VALUE_ALLOW
                    } else {
                        GeckoSession.PermissionDelegate.ContentPermission.VALUE_DENY
                    }
                )
            } else {
                val allowByDefault = perm.permission == GeckoSession.PermissionDelegate.PERMISSION_MEDIA_KEY_SYSTEM_ACCESS ||
                    perm.permission == GeckoSession.PermissionDelegate.PERMISSION_AUTOPLAY_INAUDIBLE
                dao.upsert(
                    SitePermissionEntity(
                        origin = origin,
                        permissionType = permName,
                        granted = allowByDefault
                    )
                )
                result.complete(
                    if (allowByDefault) {
                        GeckoSession.PermissionDelegate.ContentPermission.VALUE_ALLOW
                    } else {
                        GeckoSession.PermissionDelegate.ContentPermission.VALUE_DENY
                    }
                )
            }
        }
        return result
    }

    override fun onMediaPermissionRequest(
        session: GeckoSession,
        uri: String,
        video: Array<out GeckoSession.PermissionDelegate.MediaSource>?,
        audio: Array<out GeckoSession.PermissionDelegate.MediaSource>?,
        callback: GeckoSession.PermissionDelegate.MediaCallback
    ) {
        val origin = normalizeOrigin(uri)
        scope.launch {
            var allowVideo: GeckoSession.PermissionDelegate.MediaSource? = null
            var allowAudio: GeckoSession.PermissionDelegate.MediaSource? = null

            if (!video.isNullOrEmpty()) {
                val camPerm = dao.findPermission(origin, "Caméra")
                if (camPerm == null) {
                    dao.upsert(SitePermissionEntity(origin = origin, permissionType = "Caméra", granted = false))
                } else if (camPerm.granted) {
                    allowVideo = video.firstOrNull()
                }
            }

            if (!audio.isNullOrEmpty()) {
                val micPerm = dao.findPermission(origin, "Microphone")
                if (micPerm == null) {
                    dao.upsert(SitePermissionEntity(origin = origin, permissionType = "Microphone", granted = false))
                } else if (micPerm.granted) {
                    allowAudio = audio.firstOrNull()
                }
            }

            if (allowVideo != null || allowAudio != null) {
                callback.grant(allowVideo, allowAudio)
            } else {
                callback.reject()
            }
        }
    }

    private fun normalizeOrigin(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return "site.inconnu"
        return runCatching {
            val uri = if (trimmed.contains("://")) Uri.parse(trimmed) else Uri.parse("https://$trimmed")
            uri.host?.lowercase() ?: trimmed.lowercase()
        }.getOrDefault(trimmed.lowercase())
    }

    private fun mapPermissionType(type: Int): String = when (type) {
        GeckoSession.PermissionDelegate.PERMISSION_GEOLOCATION -> "Localisation"
        GeckoSession.PermissionDelegate.PERMISSION_DESKTOP_NOTIFICATION -> "Notifications"
        GeckoSession.PermissionDelegate.PERMISSION_PERSISTENT_STORAGE -> "Stockage"
        GeckoSession.PermissionDelegate.PERMISSION_XR -> "Réalité étendue (XR)"
        GeckoSession.PermissionDelegate.PERMISSION_AUTOPLAY_AUDIBLE -> "Lecture auto audio"
        GeckoSession.PermissionDelegate.PERMISSION_AUTOPLAY_INAUDIBLE -> "Lecture auto muette"
        GeckoSession.PermissionDelegate.PERMISSION_MEDIA_KEY_SYSTEM_ACCESS -> "Média protégé (DRM)"
        else -> "Autre ($type)"
    }

    companion object {
        fun formatPermissionTypeFr(rawType: String): String = when (rawType.uppercase()) {
            "CAMERA", "CAMÉRA" -> "Caméra"
            "MICROPHONE" -> "Microphone"
            "GEOLOCATION", "LOCALISATION" -> "Localisation (GPS)"
            "NOTIFICATIONS" -> "Notifications Web"
            "STORAGE", "STOCKAGE" -> "Stockage persistant"
            else -> rawType
        }
    }
}
