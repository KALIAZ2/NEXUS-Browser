package com.nexus.browser.core.designsystem

import androidx.compose.ui.graphics.Color

object NexusColors {
    // Direction Artistique Officielle NEXUS (Thème Sombre Bleu Nuit / Cyan / Violet)
    val DarkBackground = Color(0xFF050816)
    val DarkSurface = Color(0xFF0A1024)
    val DarkSurfaceElevated = Color(0xFF151E38)
    val DarkPrimary = Color(0xFF2979FF)
    val DarkSecondary = Color(0xFF00E5FF)
    val DarkTextPrimary = Color(0xFFF5F8FF)
    val DarkTextSecondary = Color(0xFF94A3B8)
    val DarkBorder = Color(0xFF1E2D52)
    val Error = Color(0xFFFF5964)

    // Dégradés de marque du ruban NEXUS N
    val BrandCyan = Color(0xFF00E5FF)
    val BrandBlue = Color(0xFF2563EB)
    val BrandViolet = Color(0xFF7C3AED)
    val BrandMagenta = Color(0xFFD946EF)

    // Thème Clair
    val LightBackground = Color(0xFFF5F7FC)
    val LightSurface = Color(0xFFFFFFFF)
    val LightSurfaceElevated = Color(0xFFEDF2FA)
    val LightPrimary = Color(0xFF2563EB)
    val LightSecondary = Color(0xFF0284C7)
    val LightTextPrimary = Color(0xFF0F172A)
    val LightTextSecondary = Color(0xFF64748B)
    val LightBorder = Color(0xFFD8E2F0)

    // Navigation Privée
    val PrivateAccent = Color(0xFFA855F7)
    val PrivateBackground = Color(0xFF0B061A)
    val PrivateSurface = Color(0xFF140C2E)

    // Couleurs sémantiques
    val Success = Color(0xFF10B981)
    val Warning = Color(0xFFF59E0B)

    fun parseHexColor(hex: String, fallback: Color = DarkPrimary): Color {
        return try {
            val cleaned = hex.trim().removePrefix("#")
            val longVal = cleaned.toLong(16)
            if (cleaned.length == 6) {
                Color(0xFF000000 or longVal)
            } else {
                Color(longVal)
            }
        } catch (_: Exception) {
            fallback
        }
    }
}
