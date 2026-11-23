package cl.huerto.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import cl.huerto.R
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import cl.huerto.data.*
import cl.huerto.domain.*
import java.time.*
import java.time.format.DateTimeFormatter

fun stamp(time: Long): String = Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
private fun Garden.person(id: Long) = usuario.find { it.id==id }?.nombre ?: "Docente"
private fun Garden.crop(id: Long) = cultivo.find { it.id==id }?.nombre.orEmpty()
private fun Garden.place(id: Long) = sector.find { it.id==id }?.nombre.orEmpty()
fun editRoute(form: Form,id: Long=0) = "form/${form.name}/$id"
@Composable fun Home(user: Usuario,g: Garden,go: (String)->Unit) {
    val admin=user.rol==ADMIN
    Page(if(admin) "Cuaderno del huerto" else "Mi jornada") {
        val pendientes = g.tarea.count { it.estado == SUBMITTED }
        val atrasadas = g.tarea.count { overdue(it.estado,it.fecha) }
        Surface(shape=RoundedCornerShape(24.dp), color=MaterialTheme.colorScheme.primary) {
            Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                    Image(painterResource(R.drawable.brote),null,Modifier.size(44.dp))
                    Text(user.grupo+" · "+user.nombre,style=MaterialTheme.typography.titleMedium)
                }
                Text(if(admin) "$pendientes entregas por revisar" else "$atrasadas tareas atrasadas",
                    style=MaterialTheme.typography.headlineMedium)
                FilledTonalButton(onClick={go(if(admin) "revisiones" else "tareas")}) {
                    Text(if(admin) "Revisar entregas" else "Mis tareas")
                }
            }
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            for ((ruta,valor,titulo) in listOf(Triple("tareas",g.tarea.size,"Tareas"),Triple("sectores",g.sector.size,"Sectores"))) {
                ElevatedCard(onClick={go(ruta)},modifier=Modifier.weight(1f)) {
                    Column(Modifier.padding(16.dp)) {
                        Text(valor.toString(),style=MaterialTheme.typography.headlineMedium)
                        Text(titulo,style=MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
        Text("Seguimiento",style=MaterialTheme.typography.titleLarge)
        Line("Problemas del huerto","${g.reporteproblema.size} reportes") { go("reportes") }
        Line("Avisos","Riegos, cosechas y mantenimiento") { go("avisos") }
        Line("Historial","Actividades, evidencias y revisiones") { go("historial") }
        Text("Planificación del taller",style=MaterialTheme.typography.titleLarge)
        Line("Tiempo en San Bernardo","Pronóstico de Open-Meteo") { go("tiempo") }
        if(admin) {
            Line("Inventario compartido","Insumos del taller") { go("insumos") }
            Line("Jornadas del taller","Agenda compartida") { go("jornadas") }
        }
        if(admin) Line("Sensores simulados","Lecturas de prueba del taller") { go("sensores") }
    }
}
@Composable fun Sectors(g: Garden,admin: Boolean,go: (String)->Unit) = Page("Sectores") {
    if(admin) Button(onClick={go(editRoute(Form.SECTOR))}) { Text("Crear sector") }
    if(g.sector.isEmpty()) Empty()
    g.sector.forEach { Line(it.nombre,it.ubicacion+" · "+it.estado) { go("sector/"+it.id) } }
}
@Composable fun SectorDetail(id: Long,g: Garden,admin: Boolean,go: (String)->Unit,remove: ()->Unit) {
    val s=g.sector.find { it.id==id } ?: return Empty()
    var confirm by rememberSaveable { mutableStateOf(false) }
    Page(s.nombre) {
        Status(s.estado); Text(s.ubicacion+" · "+s.descripcion)
        if(admin) {
            Text("Responsable: "+g.person(s.responsable))
            Text("Actualizado: "+stamp(s.actualizado))
            OutlinedButton(onClick={go(editRoute(Form.SECTOR,id))}) { Text("Editar sector") }
            Button(onClick={go(editRoute(Form.CULTIVO)+"?sector="+id)}) { Text("Registrar cultivo") }
            TextButton(onClick={confirm=true}) { Text("Eliminar sector vacío") }
        }
        Text("Cultivos",style=MaterialTheme.typography.titleLarge)
        val crops=g.cultivo.filter { it.sector==id }
        if(crops.isEmpty()) Empty("Este sector todavía no tiene cultivos.")
        crops.forEach { Line(it.nombre,it.estado) { go("cultivo/"+it.id) } }
    }
    if(confirm) AlertDialog(onDismissRequest={confirm=false},title={Text("¿Eliminar este sector?")},text={Text("Solo se puede eliminar un sector sin registros asociados.")},confirmButton={TextButton(onClick={confirm=false;remove()}) { Text("Eliminar") }},dismissButton={TextButton(onClick={confirm=false}) { Text("Cancelar") }})
}
@Composable fun CropDetail(id: Long,g: Garden,admin: Boolean,go: (String)->Unit) {
    val c=g.cultivo.find { it.id==id } ?: return Empty()
    Page(c.nombre) {
        Status(c.estado); Text(g.place(c.sector)); Text(c.descripcion)
        Line("Calendario","Siembra: ${c.siembra}\nTrasplante: ${c.trasplante.ifBlank { "Sin registrar" }}\nCosecha estimada: ${c.cosecha}\nRiego cada ${c.riego} días")
        if(c.observaciones.isNotBlank()) Text(c.observaciones)
        Preview(c.foto)
        if(admin) Button(onClick={go(editRoute(Form.CULTIVO,id))}) { Text("Editar cultivo") }
    }
}
@Composable fun Tasks(g: Garden,admin: Boolean,review: Boolean,go: (String)->Unit) {
    var state by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf("") }
    var owner by rememberSaveable { mutableStateOf("") }
    Page(if(review) "Entregas por revisar" else "Tareas") {
        if(!review) {
            if(admin) Button(onClick={go(editRoute(Form.TAREA))}) { Text("Crear y asignar tarea") }
            Choice("Estado",state,listOf("" to "Todos")+taskStates.options()){state=it}
            Choice("Fecha",date,listOf("" to "Todas")+g.tarea.map { it.fecha }.distinct().sorted().options()){date=it}
            if(admin) Choice("Responsable",owner,listOf("" to "Todos")+g.usuario.filter { it.rol==STUDENT }.map { it.id.toString() to it.nombre }){owner=it}
        }
        val rows=g.tarea.filter { (!review || it.estado==SUBMITTED) && (state.isEmpty() || it.estado==state) && (date.isEmpty() || it.fecha==date) && (owner.isEmpty() || it.responsable.toString()==owner) }.sortedBy { it.fecha }
        if(rows.isEmpty()) Empty("No hay tareas que coincidan.")
        rows.forEach { t -> Line(t.tipo+" · "+g.crop(t.cultivo),t.fecha+" · "+g.person(t.responsable)+"\n"+t.estado+if(overdue(t.estado,t.fecha)) " · Atrasada" else "") { go("tarea/"+t.id) } }
    }
}
@Composable fun TaskDetail(id: Long,g: Garden,admin: Boolean,go: (String)->Unit,start: ()->Unit) {
    val t=g.tarea.find { it.id==id } ?: return Empty()
    Page(t.tipo+" · "+g.crop(t.cultivo)) {
        Status(t.estado)
        Text(g.place(g.cultivo.find { it.id==t.cultivo }?.sector ?: 0))
        Line("Instrucciones",t.descripcion)
        Text("Responsable: "+g.person(t.responsable)+" · "+t.fecha)
        if(admin && t.estado==PENDING) Button(onClick={go(editRoute(Form.TAREA,id))}) { Text("Editar o reasignar") }
        if(!admin && t.estado in listOf(PENDING,REJECTED)) Button(onClick=start) { Text(if(t.estado==REJECTED) "Corregir actividad" else "Iniciar tarea") }
        if(!admin && t.estado==WORKING) Button(onClick={go(editRoute(Form.ENTREGA,id))}) { Text("Adjuntar y entregar") }
        if(admin && t.estado==SUBMITTED) Button(onClick={go(editRoute(Form.REVISION,id))}) { Text("Revisar entrega") }
        g.evidencia.filter { it.tarea==id }.sortedByDescending { it.id }.forEach { e ->
            Line("Entrega · "+stamp(e.fecha),e.comentario); Preview(e.foto)
            g.validacion.filter { it.evidencia==e.id }.forEach { v -> Line(v.resultado,g.person(v.autor)+" · "+stamp(v.fecha)+"\n"+v.comentario) }
        }
    }
}
@Composable fun Reports(g: Garden,go: (String)->Unit) = Page("Problemas reportados") {
    Button(onClick={go(editRoute(Form.REPORTE))}) { Text("Reportar problema") }
    if(g.reporteproblema.isEmpty()) Empty("Aún no hay problemas reportados.")
    g.reporteproblema.sortedByDescending { it.fecha }.forEach { Line(it.tipo,it.estado+" · "+g.place(it.sector)) { go("reporte/"+it.id) } }
}
@Composable fun ReportDetail(id: Long,g: Garden,admin: Boolean,go: (String)->Unit) {
    val r=g.reporteproblema.find { it.id==id } ?: return Empty()
    Page(r.tipo) {
        Status(r.estado); Text(stamp(r.fecha)+" · "+g.person(r.autor))
        Text(g.place(r.sector)+" "+(r.cultivo?.let {g.crop(it)} ?: "")); Text(r.descripcion); Preview(r.foto)
        if(admin) Button(onClick={go(editRoute(Form.SEGUIMIENTO,id))}) { Text("Actualizar atención") }
        g.seguimiento.filter { it.reporte==id }.sortedByDescending { it.id }.forEach { Line(it.estado+" · "+stamp(it.fecha),it.comentario) }
    }
}
@Composable fun Alerts(g: Garden,go: (String)->Unit) = Page("Avisos del huerto") {
    val today=LocalDate.now()
    val active=g.tarea.filter { it.estado in listOf(PENDING,WORKING,REJECTED) }.sortedBy { it.fecha }
    if(active.isEmpty() && g.cultivo.isEmpty()) Empty()
    g.tarea.filter { it.estado==SUBMITTED }.forEach { Line("Entrega pendiente de validación",it.tipo) { go("tarea/"+it.id) } }
    g.reporteproblema.filter { it.estado in listOf("Informado","En revisión") }.forEach { Line("Problema pendiente",it.tipo) { go("reporte/"+it.id) } }
    g.lectura.filter { it.estado!="Normal" }.forEach { Line("Lectura simulada fuera de rango",g.place(it.sector)+" · "+it.estado) { go("sensores") } }
    active.forEach { t -> Line(if(overdue(t.estado,t.fecha)) "Atrasada · "+t.tipo else "Programada · "+t.tipo,t.fecha+" · "+g.crop(t.cultivo)) { go("tarea/"+t.id) } }
    g.cultivo.filter { it.estado !in listOf("Finalizado","Cosechado") }.forEach { c ->
        val sow=LocalDate.parse(c.siembra)
        val days=java.time.temporal.ChronoUnit.DAYS.between(sow,today).coerceAtLeast(0)
        val next=sow.plusDays(((days+c.riego-1)/c.riego)*c.riego)
        Line("Riego planificado · "+c.nombre,next.toString()+" · cada "+c.riego+" días") { go("cultivo/"+c.id) }
        Line("Cosecha estimada · "+c.nombre,c.cosecha) { go("cultivo/"+c.id) }
    }
}
private data class Entry(val time: Long,val title: String,val detail: String,val route: String)
@Composable fun History(g: Garden,go: (String)->Unit) = Page("Historial de actividades") {
    val entries=g.evento.map { Entry(it.fecha,it.estado,"Tarea #"+it.tarea+" · "+g.person(it.autor),"tarea/"+it.tarea) } +
        g.reporteproblema.map { Entry(it.fecha,it.tipo,it.descripcion,"reporte/"+it.id) } +
        g.seguimiento.map { Entry(it.fecha,it.estado,it.comentario,"reporte/"+it.reporte) }
    if(entries.isEmpty()) Empty()
    entries.sortedByDescending { it.time }.forEach { Line(it.title,stamp(it.time)+" · "+it.detail) { go(it.route) } }
}
@Composable fun Sensors(g: Garden) = Page("Sensores simulados") {
    Text("Simulación académica · datos de prueba")
    if(g.lectura.isEmpty()) Empty()
    g.lectura.forEach { Line(g.place(it.sector)+" · "+it.estado,"Humedad del suelo: ${it.suelo}%\nTemperatura ambiental: ${it.temperatura} °C\nHumedad ambiental: ${it.aire}%\n"+stamp(it.fecha)) }
}
