package bs.wahgwaan.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import bs.wahgwaan.model.EventCategory

// Bahamian flag palette: aquamarine, gold, black.
val Aquamarine = Color(0xFF00778B)
val AquamarineLight = Color(0xFF4FA8B8)
val AquaDeep = Color(0xFF00505E)
val Gold = Color(0xFFFFC72C)
val GoldDark = Color(0xFFB8860B)
val Sand = Color(0xFFFAF6EE)
val Charcoal = Color(0xFF121417)

private val LightColors = lightColorScheme(
    primary = Aquamarine,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB7E7EF),
    onPrimaryContainer = AquaDeep,
    secondary = GoldDark,
    secondaryContainer = Color(0xFFFFE7A3),
    onSecondaryContainer = Color(0xFF4A3A00),
    background = Sand,
    surface = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = AquamarineLight,
    onPrimary = Color(0xFF00363F),
    primaryContainer = AquaDeep,
    onPrimaryContainer = Color(0xFFB7E7EF),
    secondary = Gold,
    onSecondary = Color(0xFF3D2F00),
    background = Charcoal,
    surface = Color(0xFF1B1E22),
)

@Composable
fun WahGwaanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}

/** Stable per-category accent used by cards, chips and detail headers. */
val EventCategory.accent: Color
    get() = when (this) {
        EventCategory.JUNKANOO_CULTURAL -> Color(0xFFE65100)
        EventCategory.REGATTA_MARITIME -> Color(0xFF0277BD)
        EventCategory.FARMERS_CRAFT_MARKET -> Color(0xFF558B2F)
        EventCategory.CLUB_PROMOTION -> Color(0xFF7B1FA2)
        EventCategory.FESTIVAL -> Color(0xFFAD1457)
        EventCategory.CONCERT_LIVE_MUSIC -> Color(0xFF6A1B9A)
        EventCategory.NIGHTLIFE_PARTY -> Color(0xFF283593)
        EventCategory.BEACH_PARTY -> Color(0xFF00838F)
        EventCategory.COMEDY -> Color(0xFFF9A825)
        EventCategory.PAGEANT -> Color(0xFFC2185B)
        EventCategory.FOOD_DRINK -> Color(0xFF2E7D32)
        EventCategory.SPORTS_FITNESS -> Color(0xFF00695C)
        EventCategory.ARTS_THEATRE -> Color(0xFF4527A0)
        EventCategory.BUSINESS_NETWORKING -> Color(0xFF455A64)
        EventCategory.FAITH_COMMUNITY -> Color(0xFF795548)
        EventCategory.GENERAL, EventCategory.UNKNOWN -> Color(0xFF546E7A)
    }
