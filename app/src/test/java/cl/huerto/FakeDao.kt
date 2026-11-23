package cl.huerto
import cl.huerto.data.*
import kotlinx.coroutines.flow.*

// Double for unit tests. Room integration tests separately exercise real SQL constraints.
class FakeDao : HuertoDao {
    val usuarioRows=MutableStateFlow<List<Usuario>>(emptyList())
    override fun observeUsuario():Flow<List<Usuario>> = usuarioRows
    override suspend fun getUsuario(id:Long)=usuarioRows.value.find { it.id==id }
    override suspend fun insert(row:Usuario):Long {
        val id=(usuarioRows.value.maxOfOrNull { it.id } ?: 0L)+1
        usuarioRows.value=usuarioRows.value+row.copy(id=id)
        return id
    }
    val sectorRows=MutableStateFlow<List<Sector>>(emptyList())
    override fun observeSector():Flow<List<Sector>> = sectorRows
    override suspend fun getSector(id:Long)=sectorRows.value.find { it.id==id }
    override suspend fun insert(row:Sector):Long {
        val id=(sectorRows.value.maxOfOrNull { it.id } ?: 0L)+1
        sectorRows.value=sectorRows.value+row.copy(id=id)
        return id
    }
    override suspend fun update(row:Sector) { sectorRows.value=sectorRows.value.map { if(it.id==row.id) row else it } }
    val cultivoRows=MutableStateFlow<List<Cultivo>>(emptyList())
    override fun observeCultivo():Flow<List<Cultivo>> = cultivoRows
    override suspend fun getCultivo(id:Long)=cultivoRows.value.find { it.id==id }
    override suspend fun insert(row:Cultivo):Long {
        val id=(cultivoRows.value.maxOfOrNull { it.id } ?: 0L)+1
        cultivoRows.value=cultivoRows.value+row.copy(id=id)
        return id
    }
    override suspend fun update(row:Cultivo) { cultivoRows.value=cultivoRows.value.map { if(it.id==row.id) row else it } }
    val tareaRows=MutableStateFlow<List<Tarea>>(emptyList())
    override fun observeTarea():Flow<List<Tarea>> = tareaRows
    override suspend fun getTarea(id:Long)=tareaRows.value.find { it.id==id }
    override suspend fun insert(row:Tarea):Long {
        val id=(tareaRows.value.maxOfOrNull { it.id } ?: 0L)+1
        tareaRows.value=tareaRows.value+row.copy(id=id)
        return id
    }
    override suspend fun update(row:Tarea) { tareaRows.value=tareaRows.value.map { if(it.id==row.id) row else it } }
    val evidenciaRows=MutableStateFlow<List<Evidencia>>(emptyList())
    override fun observeEvidencia():Flow<List<Evidencia>> = evidenciaRows
    override suspend fun getEvidencia(id:Long)=evidenciaRows.value.find { it.id==id }
    override suspend fun insert(row:Evidencia):Long {
        val id=(evidenciaRows.value.maxOfOrNull { it.id } ?: 0L)+1
        evidenciaRows.value=evidenciaRows.value+row.copy(id=id)
        return id
    }
    val validacionRows=MutableStateFlow<List<Validacion>>(emptyList())
    override fun observeValidacion():Flow<List<Validacion>> = validacionRows
    override suspend fun getValidacion(id:Long)=validacionRows.value.find { it.id==id }
    override suspend fun insert(row:Validacion):Long {
        val id=(validacionRows.value.maxOfOrNull { it.id } ?: 0L)+1
        validacionRows.value=validacionRows.value+row.copy(id=id)
        return id
    }
    val reporteProblemaRows=MutableStateFlow<List<ReporteProblema>>(emptyList())
    override fun observeReporteProblema():Flow<List<ReporteProblema>> = reporteProblemaRows
    override suspend fun getReporteProblema(id:Long)=reporteProblemaRows.value.find { it.id==id }
    override suspend fun insert(row:ReporteProblema):Long {
        val id=(reporteProblemaRows.value.maxOfOrNull { it.id } ?: 0L)+1
        reporteProblemaRows.value=reporteProblemaRows.value+row.copy(id=id)
        return id
    }
    override suspend fun update(row:ReporteProblema) { reporteProblemaRows.value=reporteProblemaRows.value.map { if(it.id==row.id) row else it } }
    val seguimientoRows=MutableStateFlow<List<Seguimiento>>(emptyList())
    override fun observeSeguimiento():Flow<List<Seguimiento>> = seguimientoRows
    override suspend fun getSeguimiento(id:Long)=seguimientoRows.value.find { it.id==id }
    override suspend fun insert(row:Seguimiento):Long {
        val id=(seguimientoRows.value.maxOfOrNull { it.id } ?: 0L)+1
        seguimientoRows.value=seguimientoRows.value+row.copy(id=id)
        return id
    }
    val eventoRows=MutableStateFlow<List<Evento>>(emptyList())
    override fun observeEvento():Flow<List<Evento>> = eventoRows
    override suspend fun getEvento(id:Long)=eventoRows.value.find { it.id==id }
    override suspend fun insert(row:Evento):Long {
        val id=(eventoRows.value.maxOfOrNull { it.id } ?: 0L)+1
        eventoRows.value=eventoRows.value+row.copy(id=id)
        return id
    }
    val lecturaRows=MutableStateFlow<List<Lectura>>(emptyList())
    override fun observeLectura():Flow<List<Lectura>> = lecturaRows
    override suspend fun getLectura(id:Long)=lecturaRows.value.find { it.id==id }
    override suspend fun insert(row:Lectura):Long {
        val id=(lecturaRows.value.maxOfOrNull { it.id } ?: 0L)+1
        lecturaRows.value=lecturaRows.value+row.copy(id=id)
        return id
    }
    override suspend fun login(name:String)=usuarioRows.value.find { it.nombre==name && it.activo }
    override suspend fun latest(id:Long)=evidenciaRows.value.lastOrNull { it.tarea==id }
    override suspend fun hasSector(user:Long,sector:Long)=tareaRows.value.any { it.responsable==user && getCultivo(it.cultivo)?.sector==sector }
    override suspend fun removeEmpty(id:Long):Int {
        if(getSector(id)==null || cultivoRows.value.any { it.sector==id } || lecturaRows.value.any { it.sector==id } || reporteProblemaRows.value.any { it.sector==id }) return 0
        sectorRows.value=sectorRows.value.filter { it.id!=id }
        return 1
    }
}
