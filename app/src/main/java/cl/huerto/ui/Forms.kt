package cl.huerto.ui
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.huerto.domain.*
import java.time.LocalDate

private fun initial(form: Form,id: Long,g: Garden): Map<String,String> = when(form) {
    Form.SECTOR -> g.sector.find { it.id==id }?.let { mapOf("Nombre" to it.nombre,"Ubicación" to it.ubicacion,"Descripción" to it.descripcion,"Estado" to it.estado) } ?: mapOf("Estado" to sectorStates.first())
    Form.CULTIVO -> g.cultivo.find { it.id==id }?.let { mapOf("Nombre" to it.nombre,"Sector" to it.sector.toString(),"Descripción" to it.descripcion,"Estado" to it.estado,"Siembra" to it.siembra,"Trasplante" to it.trasplante,"Cosecha" to it.cosecha,"Riego cada (días)" to it.riego.toString(),"Observaciones" to it.observaciones,"Foto" to it.foto) } ?: mapOf("Estado" to cropStates.first(),"Siembra" to LocalDate.now().toString())
    Form.TAREA -> g.tarea.find { it.id==id }?.let { mapOf("Tipo" to it.tipo,"Cultivo" to it.cultivo.toString(),"Responsable" to it.responsable.toString(),"Fecha" to it.fecha,"Instrucciones" to it.descripcion) } ?: mapOf("Fecha" to LocalDate.now().toString())
    Form.REVISION -> mapOf("Resultado" to APPROVED)
    Form.SEGUIMIENTO -> mapOf("Estado" to (g.reporteproblema.find { it.id==id }?.estado ?: reportStates.first()))
    else -> emptyMap()
}
private val layouts = mapOf(
    Form.SECTOR to listOf("Nombre","Ubicación","Descripción","Estado"),
    Form.CULTIVO to listOf("Sector","Nombre","Descripción","Siembra","Trasplante","Cosecha","Riego cada (días)","Estado","Observaciones","Foto"),
    Form.TAREA to listOf("Cultivo","Responsable","Tipo","Instrucciones","Fecha"),
    Form.ENTREGA to listOf("Comentario","Foto"),
    Form.REVISION to listOf("Resultado","Comentario"),
    Form.REPORTE to listOf("Sector","Cultivo","Tipo","Descripción","Foto"),
    Form.SEGUIMIENTO to listOf("Estado","Comentario")
)
@Composable fun Editor(form: Form,id: Long,g: Garden,vm: HuertoViewModel,sector: Long,done: ()->Unit) {
    var fields by rememberSaveable(form,id) { mutableStateOf(HashMap(initial(form,id,g)).apply { if(form==Form.CULTIVO && id==0L && sector>0) put("Sector",sector.toString()) }) }
    val errors by vm.errors.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    LaunchedEffect(form,id) { vm.errors.value=emptyMap() }
    val title=when(form) {
        Form.SECTOR -> if(id==0L) "Crear sector" else "Editar sector"
        Form.CULTIVO -> if(id==0L) "Registrar cultivo" else "Editar cultivo"
        Form.TAREA -> if(id==0L) "Asignar tarea" else "Editar tarea"
        Form.ENTREGA -> "Entregar actividad"
        Form.REVISION -> "Revisar evidencia"
        Form.REPORTE -> "Reportar problema"
        Form.SEGUIMIENTO -> "Registrar seguimiento"
    }
    Page(title) {
        if(form==Form.REVISION) g.evidencia.filter { it.tarea==id }.maxByOrNull { it.id }?.let { Text(it.comentario); Preview(it.foto) }
        for(key in layouts.getValue(form)) {
            val options=when(key) {
                "Sector" -> g.sector.filter { form!=Form.CULTIVO || id==0L || it.id==g.cultivo.find { c->c.id==id }?.sector }.map { it.id.toString() to it.nombre }
                "Cultivo" -> (if(form==Form.REPORTE) listOf("" to "Solo sector") else emptyList()) + g.cultivo.filter { form!=Form.REPORTE || it.sector==fields.number("Sector") }.map { c -> c.id.toString() to (c.nombre+" · "+g.sector.find { it.id==c.sector }?.nombre.orEmpty()) }
                "Responsable" -> g.usuario.filter { it.activo && it.rol==STUDENT }.map { it.id.toString() to it.nombre }
                "Tipo" -> (if(form==Form.TAREA) types else problems).options()
                "Estado" -> (when(form){ Form.SECTOR->sectorStates; Form.CULTIVO->cropStates; else->reportStates }).options()
                "Resultado" -> listOf(APPROVED,REJECTED).options()
                else -> null
            }
            val change: (String)->Unit = { value ->
                fields=HashMap(fields).apply { put(key,value); if(key=="Sector" && form==Form.REPORTE) remove("Cultivo") }
                vm.errors.value=vm.errors.value-key
            }
            if(key=="Foto") PhotoField(fields.value(key),errors[key],change)
            else if(options!=null) Choice(key,fields.value(key),options,errors[key],change)
            else Field(if(key in listOf("Fecha","Siembra","Cosecha","Trasplante")) "$key (AAAA-MM-DD)" else key,fields[key].orEmpty(),errors[key],change=change)
        }
        Button(enabled=!busy,onClick={vm.save(form,id,fields,done)},modifier=Modifier.fillMaxWidth()) { Text(title) }
    }
}
