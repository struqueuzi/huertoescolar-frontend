package cl.huerto.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import cl.huerto.data.*
import cl.huerto.domain.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

data class Garden(
    val usuario: List<Usuario> = emptyList(),
    val sector: List<Sector> = emptyList(),
    val cultivo: List<Cultivo> = emptyList(),
    val tarea: List<Tarea> = emptyList(),
    val evidencia: List<Evidencia> = emptyList(),
    val validacion: List<Validacion> = emptyList(),
    val reporteproblema: List<ReporteProblema> = emptyList(),
    val seguimiento: List<Seguimiento> = emptyList(),
    val evento: List<Evento> = emptyList(),
    val lectura: List<Lectura> = emptyList()
) {
    fun forUser(user: Usuario): Garden {
        if(user.rol == ADMIN) return this
        val tasks = tarea.filter { it.responsable == user.id }
        val crops = cultivo.filter { c -> tasks.any { it.cultivo == c.id } }
        val reports = reporteproblema.filter { it.autor == user.id }
        val sectors = sector.filter { s -> crops.any { it.sector == s.id } }
        val photos = evidencia.filter { e -> tasks.any { it.id == e.tarea } }
        return copy(usuario=listOf(user), sector=sectors, cultivo=crops, tarea=tasks, evidencia=photos,
            validacion=validacion.filter { v -> photos.any { it.id == v.evidencia } }, reporteproblema=reports,
            seguimiento=seguimiento.filter { s -> reports.any { it.id == s.reporte } },
            evento=evento.filter { e -> tasks.any { it.id == e.tarea } }, lectura=emptyList())
    }
}
data class ScreenState(val user: Usuario? = null, val garden: Garden = Garden(), val ready: Boolean = false)
class HuertoViewModel(app: Application) : AndroidViewModel(app) {
    private val database = Room.databaseBuilder(app,HuertoDb::class.java,"huerto.db").build()
    private val repo = Repository(database,File(app.filesDir,"fotos"))
    private val dao = repo.dao
    private val session = MutableStateFlow<Usuario?>(null)
    private val initialized = MutableStateFlow(false)
    val message = MutableStateFlow("")
    val errors = MutableStateFlow<Map<String,String>>(emptyMap())
    val busy = MutableStateFlow(false)
    private val inventory = combine(dao.observeUsuario(),dao.observeSector(),dao.observeCultivo(),dao.observeTarea(),dao.observeEvidencia()) { u,s,c,t,e -> Garden(u,s,c,t,e) }
    private val history = combine(dao.observeValidacion(),dao.observeReporteProblema(),dao.observeSeguimiento(),dao.observeEvento(),dao.observeLectura()) { v,r,s,e,l -> Garden(validacion=v,reporteproblema=r,seguimiento=s,evento=e,lectura=l) }
    val state = combine(session,inventory,history,initialized) { user,i,h,ready ->
        val garden = i.copy(validacion=h.validacion,reporteproblema=h.reporteproblema,seguimiento=h.seguimiento,evento=h.evento,lectura=h.lectura)
        ScreenState(user,user?.let { garden.forUser(it) } ?: Garden(),ready)
    }.stateIn(viewModelScope,SharingStarted.Eagerly,ScreenState())
    init { runAction { repo.seed(); initialized.value=true } }
    private fun runAction(work: suspend () -> Unit) {
        if(busy.value) return
        busy.value=true
        viewModelScope.launch {
            try { errors.value=emptyMap(); work() }
            catch(e: Invalid) { errors.value=e.errors; message.value=e.message.orEmpty() }
            catch(e: Exception) { message.value=e.localizedMessage ?: "No se pudo completar la operación" }
            finally { busy.value=false }
        }
    }
    fun login(name: String, key: String) = runAction {
        session.value=repo.login(name,key) ?: throw Invalid(mapOf("Cuenta" to "Cuenta o clave incorrecta."))
    }
    fun logout() { session.value=null; errors.value=emptyMap(); message.value="" }
    fun start(id: Long) = runAction { repo.start(requireNotNull(session.value).id,id); message.value="Tarea en desarrollo" }
    fun deleteSector(id: Long, done: () -> Unit) = runAction { repo.deleteSector(requireNotNull(session.value).id,id); done(); message.value="Sector eliminado" }
    fun save(form: Form,id: Long,fields: Fields,done: () -> Unit) = runAction {
        repo.save(requireNotNull(session.value).id,form,id,fields); done(); message.value="Registro guardado"
    }
}
