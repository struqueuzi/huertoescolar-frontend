package cl.huerto.ui.theme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import cl.huerto.domain.*
private val bosque = Color(0xFF24563E)
private val tierra = Color(0xFF77502F)
private val tinta = Color(0xFF192820)
val success = Color(0xFF21613D)
val warning = Color(0xFF785100)
val information = Color(0xFF245780)
val danger = Color(0xFFA32B29)
private val palette = lightColorScheme(primary=bosque,secondary=tierra,background=Color(0xFFF8F8EE),surface=Color(0xFFFCFCF4),onSurface=tinta,error=danger)
private val type = Typography()
@Composable fun GardenTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme=palette,typography=type,content=content)
fun statusColor(text: String) = when(text) {
    APPROVED,"Cosechado","Cerrado","Atendido","Disponible","En crecimiento" -> success
    REJECTED,"Con problemas","Atrasada","Fuera de uso" -> danger
    PENDING,SUBMITTED,"Informado","Listo para cosechar","En mantenimiento" -> warning
    else -> information
}
