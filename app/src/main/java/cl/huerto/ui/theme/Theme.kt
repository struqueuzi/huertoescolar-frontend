package cl.huerto.ui.theme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import cl.huerto.domain.*
private val ink = Color(0xFF14142C)
private val plum = Color(0xFF29243F)
private val lime = Color(0xFFCDFA76)
val success = Color(0xFFAAF3A9)
val warning = Color(0xFFFFCE83)
val information = Color(0xFFBAB5FF)
val danger = Color(0xFFFFA19B)
private val palette = darkColorScheme(
    primary=lime, onPrimary=ink, secondary=information, background=ink,
    surface=plum, surfaceVariant=Color(0xFF37324E), onSurface=Color(0xFFFBF6ED),
    onBackground=Color(0xFFFBF6ED), onSurfaceVariant=Color(0xFFD2CBDD),
    secondaryContainer=Color(0xFF474061), onSecondaryContainer=Color.White,
    outline=Color(0xFF8B819E), outlineVariant=Color(0xFF4A435D), error=danger)
private val type = Typography().let { defaults -> defaults.copy(
    headlineMedium=defaults.headlineMedium.copy(fontFamily=androidx.compose.ui.text.font.FontFamily.Serif),
    titleMedium=defaults.titleMedium.copy(fontWeight=androidx.compose.ui.text.font.FontWeight.Bold)) }
@Composable fun GardenTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme=palette,typography=type,content=content)
fun statusColor(text: String) = when(text) {
    APPROVED,"Cosechado","Cerrado","Atendido","Disponible","En crecimiento" -> success
    REJECTED,"Con problemas","Atrasada","Fuera de uso" -> danger
    PENDING,SUBMITTED,"Informado","Listo para cosechar","En mantenimiento" -> warning
    else -> information
}
