package benrusza.gatekepper.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AppColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = Color.Black,         // Fondo de la app: negro
    surface = Color.Black,            // Fondo de superficies (cards, etc.): negro
    onPrimary = Color.White,          // Texto sobre color primario: blanco
    onSecondary = Color.White,        // Texto sobre color secundario: blanco
    onTertiary = Color.White,         // Texto sobre color terciario: blanco
    onBackground = Color.White,       // Texto sobre el fondo principal: blanco
    onSurface = Color.White           // Texto sobre superficies: blanco
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    // Aplicamos siempre nuestro esquema de color personalizado.
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = Typography,
        content = content
    )
}