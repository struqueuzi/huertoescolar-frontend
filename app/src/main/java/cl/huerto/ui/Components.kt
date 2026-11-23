package cl.huerto.ui
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import cl.huerto.ui.theme.statusColor

@Composable fun Page(title: String, body: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.background))),contentAlignment=Alignment.TopCenter) {
        Column(Modifier.widthIn(max=720.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text(title,style=MaterialTheme.typography.headlineMedium)
            body()
            Spacer(Modifier.height(20.dp))
        }
    }
}
@Composable fun Line(title: String, detail: String="", action: (() -> Unit)?=null) {
    val content: @Composable ColumnScope.() -> Unit = {
        Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically) {
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
            Text(title,style=MaterialTheme.typography.titleMedium)
            if(detail.isNotBlank()) Text(detail,color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodyMedium)
        }
        if(action!=null) Text("›",style=MaterialTheme.typography.headlineMedium,color=MaterialTheme.colorScheme.primary)
        }
    }
    if(action == null) OutlinedCard(Modifier.fillMaxWidth(),content=content)
    else OutlinedCard(onClick=action,modifier=Modifier.fillMaxWidth(),content=content)
}
@Composable fun Status(value: String) { Crossfade(value,label="Estado de tarea") { Surface(shape=RoundedCornerShape(50),color=statusColor(it).copy(alpha=.14f)) { Text(it,Modifier.padding(horizontal=14.dp,vertical=6.dp),color=statusColor(it),style=MaterialTheme.typography.labelLarge) } } }
@Composable fun Field(label: String,value: String,error: String?,secret: Boolean=false,change: (String)->Unit) {
    OutlinedTextField(value,change,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp),colors=OutlinedTextFieldDefaults.colors(unfocusedContainerColor=MaterialTheme.colorScheme.surface,focusedContainerColor=MaterialTheme.colorScheme.surface),label={Text(label)},isError=error!=null,
        visualTransformation=if(secret) PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon={if(error!=null) Text("⚠",color=MaterialTheme.colorScheme.error)},
        supportingText={AnimatedVisibility(error!=null) { Text(error.orEmpty()) }})
}
@Composable fun Choice(label: String,value: String,options: List<Pair<String,String>>,error: String?=null,change: (String)->Unit) {
    var open by remember { mutableStateOf(false) }
    Column {
        Box {
            OutlinedButton(onClick={open=true},modifier=Modifier.fillMaxWidth()) { Text("$label · "+(options.find { it.first==value }?.second ?: "Seleccionar")) }
            DropdownMenu(open,{open=false}) { options.forEach { (id,name) -> DropdownMenuItem(text={Text(name)},onClick={change(id);open=false}) } }
        }
        AnimatedVisibility(error!=null) { Text("⚠ "+error.orEmpty(),color=MaterialTheme.colorScheme.error) }
    }
}
fun List<String>.options() = map { it to it }
@Composable fun Empty(text: String="No hay registros para mostrar.") { Text(text,style=MaterialTheme.typography.bodyMedium) }
