package com.example.iapmanagement.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * Demo palette, aligned with the developer portal (portal-web/tailwind.config.js):
 *   primary navy #1E3A5F · accent teal #14B8A6 · slate neutrals · status green/amber/red.
 * A single professional, SaaS-style theme shared by every demo screen.
 */

// Brand
val BrandPrimary = Color(0xFF1E3A5F)
val BrandPrimaryDark = Color(0xFF172C48)
val AccentTeal = Color(0xFF14B8A6)
val AccentTealDark = Color(0xFF0F766E)
val SlateSecondary = Color(0xFF334155)

// Neutrals (light)
val Canvas = Color(0xFFF8FAFC)
val CardSurface = Color(0xFFFFFFFF)
val SurfaceMuted = Color(0xFFF1F5F9)
val BorderLight = Color(0xFFE2E8F0)
val OutlineLight = Color(0xFFCBD5E1)
val TextPrimary = Color(0xFF0F172A)
val TextSecondary = Color(0xFF64748B)

// Neutrals (dark)
val CanvasDark = Color(0xFF0F172A)
val CardSurfaceDark = Color(0xFF1E293B)
val SurfaceMutedDark = Color(0xFF334155)
val BorderDark = Color(0xFF334155)
val OutlineDark = Color(0xFF475569)
val TextPrimaryDark = Color(0xFFE2E8F0)
val TextSecondaryDark = Color(0xFF94A3B8)
val BrandPrimaryLight = Color(0xFF7FA6D0)
val AccentTealLight = Color(0xFF2DD4BF)

// Section accents — one distinct hue per demo area, for a more unique, modern home screen.
val AccentBlue = Color(0xFF2563EB)
val AccentPurple = Color(0xFF8B5CF6)

// Status (shared across light/dark; containers tuned per scheme in Theme.kt)
val StatusSuccess = Color(0xFF16A34A)
val StatusSuccessContainer = Color(0xFFDCFCE7)
val StatusWarning = Color(0xFFF59E0B)
val StatusError = Color(0xFFDC2626)
val StatusErrorContainer = Color(0xFFFEE2E2)
val StatusErrorDark = Color(0xFFF87171)
