package cl.huerto.data

import androidx.room.withTransaction
import cl.huerto.domain.*
import java.io.File
import java.time.LocalDate

class Repository(val db: HuertoDb, private val photos: File) {
    val dao = db.dao()
    suspend fun login(name: String, password: String) = dao.login(name.trim())?.takeIf { it.claveHash == hash(password) }
    private suspend fun actor(id: Long) = requireNotNull(dao.getUsuario(id)?.takeIf { it.activo }) { "Sesión inválida" }
    private fun admin(user: Usuario) = require(user.rol == ADMIN) { "Acción reservada al docente" }
    private fun owner(user: Usuario, task: Tarea) = require(user.rol == STUDENT && task.responsable == user.id) { "Tarea no asignada a esta cuenta" }
    private fun verifyPhoto(path: String) {
        require(File(path).canonicalFile.parentFile == photos.canonicalFile && File(path).length() > 0) { "Fotografía no disponible" }
    }
    suspend fun start(userId: Long, id: Long) = db.withTransaction {
        val task = requireNotNull(dao.getTarea(id))
        owner(actor(userId),task)
        require(task.estado in listOf(PENDING, REJECTED)) { "La tarea no se puede iniciar" }
        dao.update(task.copy(estado = WORKING))
        dao.insert(Evento(tarea=id,autor=userId,estado=WORKING,fecha=System.currentTimeMillis()))
    }
    suspend fun deleteSector(userId: Long, id: Long) = db.withTransaction {
        admin(actor(userId))
        require(dao.removeEmpty(id) == 1) { "El sector tiene registros asociados" }
    }
    suspend fun save(userId: Long, form: Form, id: Long, f: Fields) = db.withTransaction {
        val user = actor(userId)
        val errors = Rules.validate(form,f)
        if(errors.isNotEmpty()) throw Invalid(errors)
        val now = System.currentTimeMillis()
        fun v(k: String) = f.value(k)
        fun n(k: String) = f.number(k)
        if(v("Foto").isNotEmpty()) verifyPhoto(v("Foto"))
        when(form) {
            Form.SECTOR -> {
                admin(user)
                val old = if(id == 0L) null else requireNotNull(dao.getSector(id))
                val row = Sector(id,v("Nombre"),v("Ubicación"),v("Descripción"),v("Estado"),old?.responsable ?: userId,old?.creado ?: now,now)
                if(old == null) dao.insert(row) else dao.update(row)
            }
            Form.CULTIVO -> {
                admin(user)
                requireNotNull(dao.getSector(n("Sector")))
                val old = if(id == 0L) null else requireNotNull(dao.getCultivo(id))
                require(old == null || old.sector == n("Sector")) { "El sector se conserva para mantener el historial" }
                val row = Cultivo(id,n("Sector"),v("Nombre"),v("Descripción"),v("Siembra"),v("Trasplante"),v("Cosecha"),v("Riego cada (días)").toInt(),v("Estado"),v("Observaciones"),v("Foto"))
                if(old == null) dao.insert(row) else dao.update(row)
            }
            Form.TAREA -> {
                admin(user)
                requireNotNull(dao.getCultivo(n("Cultivo")))
                if(dao.getUsuario(n("Responsable"))?.let { it.activo && it.rol == STUDENT } != true) throw Invalid(mapOf("Responsable" to "Selecciona un estudiante activo."))
                val old = if(id == 0L) null else requireNotNull(dao.getTarea(id))
                require(old == null || old.estado == PENDING) { "Solo puedes editar tareas pendientes" }
                val row = Tarea(id,n("Cultivo"),n("Responsable"),old?.autor ?: userId,v("Tipo"),v("Instrucciones"),v("Fecha"),PENDING,old?.creado ?: now)
                if(old == null) {
                    val task = dao.insert(row)
                    dao.insert(Evento(tarea=task,autor=userId,estado=PENDING,fecha=now))
                } else dao.update(row)
            }
            Form.ENTREGA -> {
                val task = requireNotNull(dao.getTarea(id))
                owner(user,task)
                require(task.estado == WORKING) { "Inicia la tarea antes de entregar" }
                dao.insert(Evidencia(tarea=id,autor=userId,comentario=v("Comentario"),foto=v("Foto"),fecha=now))
                dao.update(task.copy(estado=SUBMITTED))
                dao.insert(Evento(tarea=id,autor=userId,estado=SUBMITTED,fecha=now))
            }
            Form.REVISION -> {
                admin(user)
                val task = requireNotNull(dao.getTarea(id))
                require(task.estado == SUBMITTED && task.responsable != userId) { "La tarea no está disponible para revisión" }
                val evidence = requireNotNull(dao.latest(id))
                dao.insert(Validacion(evidencia=evidence.id,autor=userId,resultado=v("Resultado"),comentario=v("Comentario"),fecha=now))
                dao.update(task.copy(estado=v("Resultado")))
                dao.insert(Evento(tarea=id,autor=userId,estado=v("Resultado"),fecha=now))
            }
            Form.REPORTE -> {
                val sector = n("Sector")
                requireNotNull(dao.getSector(sector))
                val crop = n("Cultivo").takeIf { it > 0 }
                if(crop != null && dao.getCultivo(crop)?.sector != sector) throw Invalid(mapOf("Cultivo" to "El cultivo debe pertenecer al sector."))
                if(user.rol != ADMIN) require(dao.hasSector(userId,sector)) { "Sector fuera de tus asignaciones" }
                dao.insert(ReporteProblema(sector=sector,cultivo=crop,autor=userId,tipo=v("Tipo"),descripcion=v("Descripción"),foto=v("Foto"),estado=reportStates.first(),fecha=now))
            }
            Form.SEGUIMIENTO -> {
                admin(user)
                val report = requireNotNull(dao.getReporteProblema(id))
                dao.update(report.copy(estado=v("Estado")))
                dao.insert(Seguimiento(reporte=id,autor=userId,estado=v("Estado"),comentario=v("Comentario"),fecha=now))
            }
        }
        Unit
    }
    suspend fun seed() = db.withTransaction {
        if(dao.getUsuario(1) != null) return@withTransaction
        val now = System.currentTimeMillis()
        val today = LocalDate.now()
        listOf("docente","luna","nico","sol").forEachIndexed { i, name ->
            dao.insert(Usuario(nombre=name,claveHash=hash("Huerto123"),rol=if(i==0) ADMIN else STUDENT,grupo="Taller escolar"))
        }
        val sector = dao.insert(Sector(nombre="Bancal norte",ubicacion="Patio norte",descripcion="Hortalizas de hoja",estado="En uso",responsable=1,creado=now,actualizado=now))
        val crop = dao.insert(Cultivo(sector=sector,nombre="Acelga",descripcion="Variedad verde",siembra=today.minusDays(30).toString(),trasplante="",cosecha=today.plusDays(5).toString(),riego=2,estado="En crecimiento",observaciones="",foto=""))
        listOf("Riego","Observación","Mantenimiento","Siembra").forEachIndexed { i,type ->
            val task = dao.insert(Tarea(cultivo=crop,responsable=(2+i%3).toLong(),autor=1,tipo=type,descripcion=listOf("Riega la base de las acelgas y registra el resultado.","Observa el color de las hojas.","Limpia los bordes del bancal.","Prepara una hilera para siembra.")[i],fecha=today.plusDays((i-1).toLong()).toString(),estado=PENDING,creado=now))
            dao.insert(Evento(tarea=task,autor=1,estado=PENDING,fecha=now))
        }
        val sample = demoEvidence(photos)
        // One earlier submission remains available for the first teacher review.
        val submitted = dao.insert(Tarea(cultivo=crop,responsable=3,autor=1,tipo="Observación",
            descripcion="Registra el crecimiento del primer brote.",fecha=today.toString(),estado=SUBMITTED,creado=now-180000))
        for ((offset,status) in listOf(180000L to PENDING,120000L to WORKING,60000L to SUBMITTED)) {
            dao.insert(Evento(tarea=submitted,autor=if(status==PENDING) 1 else 3,estado=status,fecha=now-offset))
        }
        dao.insert(Evidencia(tarea=submitted,autor=3,comentario="Observación ficticia del brote de acelga.",foto=sample,fecha=now-60000))
        dao.insert(ReporteProblema(sector=sector,cultivo=crop,autor=2,tipo="Falta de agua",
            descripcion="Ejemplo ficticio: borde del bancal seco.",foto=sample,estado="Informado",fecha=now-30000))
        dao.insert(Lectura(sector=sector,suelo=46,temperatura=21,aire=63,estado="Normal",fecha=now))
    }
}
