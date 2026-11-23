package cl.huerto.remote

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.huerto.ui.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.Credentials
import retrofit2.HttpException

data class Panel(val filas: List<Registro> = emptyList(), val ocupado: Boolean=false,val aviso: String="")
class TallerViewModel(
    val modulo: Modulo,
    private val api: TallerApi = retrofit("http://127.0.0.1:${modulo.puerto}/api/").create(TallerApi::class.java)
): ViewModel() {
    private val _panel=MutableStateFlow(Panel())
    val panel=_panel.asStateFlow()
    fun ejecutar(clave: String, accion: suspend (TallerApi,String)->Unit = { _,_-> }) {
        if(_panel.value.ocupado) return
        _panel.value=_panel.value.copy(ocupado=true,aviso="")
        viewModelScope.launch {
            var escrito=false
            try {
                require(clave.isNotBlank()) { "Ingresa la clave del servicio." }
                val auth=Credentials.basic("docente",clave)
                accion(api,auth)
                escrito=true
                val rows=api.listar(auth,modulo.ruta)
                _panel.value=Panel(rows,aviso="Datos actualizados")
            } catch(e: kotlinx.coroutines.CancellationException) { throw e } catch(e: Exception) {
                val msg=when(e) {
                    is HttpException -> if(e.code()==401) "Clave del servicio incorrecta" else "Operación rechazada (HTTP ${e.code()})"
                    is IllegalArgumentException -> e.message.orEmpty()
                    else -> "Sin conexión al servicio del taller"
                }
                _panel.value=_panel.value.copy(ocupado=false,aviso=msg+if(escrito) ". Vuelve a consultar antes de repetir la operación." else "")
            }
        }
    }
}
@Composable fun TallerScreen(vm: TallerViewModel) {
    val modulo=vm.modulo
    val panel by vm.panel.collectAsStateWithLifecycle()
    var clave by remember { mutableStateOf("") }
    var id by rememberSaveable { mutableStateOf<Long?>(null) }
    var nombre by rememberSaveable { mutableStateOf("") }
    var valor by rememberSaveable { mutableStateOf("") }
    var borrar by remember { mutableStateOf<Registro?>(null) }
    Page(if(modulo==Modulo.INSUMOS) "Inventario compartido" else "Jornadas del taller") {
        Field("Clave del servicio docente",clave,null,true){clave=it}
        Button(enabled=!panel.ocupado,onClick={vm.ejecutar(clave)}){Text("Consultar servidor")}
        if(panel.ocupado) LinearProgressIndicator()
        if(panel.aviso.isNotBlank()) Text(panel.aviso)
        panel.filas.forEach { fila ->
            Line(fila.nombre,fila.fecha ?: "Cantidad: ${fila.cantidad}") { id=fila.id;nombre=fila.nombre;valor=fila.fecha ?: fila.cantidad.toString() }
            TextButton(enabled=!panel.ocupado,onClick={borrar=fila}) { Text("Eliminar "+fila.nombre) }
        }
        Text(if(id==null) "Nuevo registro" else "Editar registro",style=MaterialTheme.typography.titleLarge)
        Field("Nombre",nombre,null){nombre=it}
        Field(if(modulo==Modulo.INSUMOS) "Cantidad" else "Fecha AAAA-MM-DD",valor,null){valor=it}
        Button(enabled=!panel.ocupado,onClick={
            val actual=id
            vm.ejecutar(clave){api,auth ->
                val dato=registro(modulo,nombre,valor)
                if(actual==null) api.crear(auth,modulo.ruta,dato) else api.editar(auth,modulo.ruta,actual,dato)
                id=null;nombre="";valor=""
            }
        }) { Text("Guardar en servidor") }
        if(id!=null) TextButton(onClick={id=null;nombre="";valor=""}){Text("Cancelar edición")}
    }
    borrar?.let { fila -> AlertDialog(onDismissRequest={borrar=null},title={Text("¿Eliminar "+fila.nombre+"?")},confirmButton={
        TextButton(onClick={borrar=null;vm.ejecutar(clave){api,auth->api.borrar(auth,modulo.ruta,requireNotNull(fila.id)); if(id==fila.id){id=null;nombre="";valor=""}}}){Text("Eliminar")}
    },dismissButton={TextButton(onClick={borrar=null}){Text("Cancelar")}}) }
}
@Composable fun WeatherScreen() {
    var intento by remember { mutableIntStateOf(0) }
    var lectura by remember { mutableStateOf<LecturaActual?>(null) }
    var aviso by remember { mutableStateOf("") }
    LaunchedEffect(intento) {
        aviso="Consultando Open-Meteo…"
        try { lectura=retrofit("https://api.open-meteo.com/").create(ClimaApi::class.java).consultar().current; aviso="" }
        catch(e:java.io.IOException){aviso="Sin conexión. Puedes volver a consultar."}
        catch(e:HttpException){aviso="Open-Meteo respondió HTTP ${e.code()}."}
    }
    Page("Tiempo en San Bernardo") {
        Text("Fuente: Open-Meteo · referencia meteorológica para planificar el taller")
        if(aviso.isNotBlank()) Text(aviso)
        lectura?.let { Line("Temperatura: ${it.temperature_2m} °C","Precipitación: ${it.precipitation} mm · ${it.time}") }
        OutlinedButton(onClick={intento++}){Text("Actualizar tiempo")}
    }
}
