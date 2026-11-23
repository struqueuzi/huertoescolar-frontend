package cl.huerto.data
import androidx.room.*
import kotlinx.coroutines.flow.Flow
@Dao
interface HuertoDao {
    @Query("SELECT EXISTS(SELECT 1 FROM Tarea JOIN Cultivo ON Cultivo.id=Tarea.cultivo WHERE Tarea.responsable=:user AND Cultivo.sector=:sector)") suspend fun hasSector(user: Long, sector: Long): Boolean
    @Insert suspend fun insert(row: Usuario): Long
    @Query("SELECT * FROM Usuario") fun observeUsuario(): Flow<List<Usuario>>
    @Query("SELECT * FROM Usuario WHERE id = :id") suspend fun getUsuario(id: Long): Usuario?
    @Insert suspend fun insert(row: Sector): Long
    @Query("SELECT * FROM Sector") fun observeSector(): Flow<List<Sector>>
    @Query("SELECT * FROM Sector WHERE id = :id") suspend fun getSector(id: Long): Sector?
    @Update suspend fun update(row: Sector)
    @Insert suspend fun insert(row: Cultivo): Long
    @Query("SELECT * FROM Cultivo") fun observeCultivo(): Flow<List<Cultivo>>
    @Query("SELECT * FROM Cultivo WHERE id = :id") suspend fun getCultivo(id: Long): Cultivo?
    @Update suspend fun update(row: Cultivo)
    @Insert suspend fun insert(row: Tarea): Long
    @Query("SELECT * FROM Tarea") fun observeTarea(): Flow<List<Tarea>>
    @Query("SELECT * FROM Tarea WHERE id = :id") suspend fun getTarea(id: Long): Tarea?
    @Update suspend fun update(row: Tarea)
    @Insert suspend fun insert(row: Evidencia): Long
    @Query("SELECT * FROM Evidencia") fun observeEvidencia(): Flow<List<Evidencia>>
    @Query("SELECT * FROM Evidencia WHERE id = :id") suspend fun getEvidencia(id: Long): Evidencia?
    @Insert suspend fun insert(row: Validacion): Long
    @Query("SELECT * FROM Validacion") fun observeValidacion(): Flow<List<Validacion>>
    @Query("SELECT * FROM Validacion WHERE id = :id") suspend fun getValidacion(id: Long): Validacion?
    @Insert suspend fun insert(row: ReporteProblema): Long
    @Query("SELECT * FROM ReporteProblema") fun observeReporteProblema(): Flow<List<ReporteProblema>>
    @Query("SELECT * FROM ReporteProblema WHERE id = :id") suspend fun getReporteProblema(id: Long): ReporteProblema?
    @Update suspend fun update(row: ReporteProblema)
    @Insert suspend fun insert(row: Seguimiento): Long
    @Query("SELECT * FROM Seguimiento") fun observeSeguimiento(): Flow<List<Seguimiento>>
    @Query("SELECT * FROM Seguimiento WHERE id = :id") suspend fun getSeguimiento(id: Long): Seguimiento?
    @Insert suspend fun insert(row: Evento): Long
    @Query("SELECT * FROM Evento") fun observeEvento(): Flow<List<Evento>>
    @Query("SELECT * FROM Evento WHERE id = :id") suspend fun getEvento(id: Long): Evento?
    @Insert suspend fun insert(row: Lectura): Long
    @Query("SELECT * FROM Lectura") fun observeLectura(): Flow<List<Lectura>>
    @Query("SELECT * FROM Lectura WHERE id = :id") suspend fun getLectura(id: Long): Lectura?

    @Query("SELECT * FROM Usuario WHERE nombre = :name AND activo = 1") suspend fun login(name: String): Usuario?
    @Query("SELECT * FROM Evidencia WHERE tarea = :id ORDER BY id DESC LIMIT 1") suspend fun latest(id: Long): Evidencia?
    @Query("DELETE FROM Sector WHERE id = :id AND NOT EXISTS(SELECT 1 FROM Cultivo WHERE sector=:id) AND NOT EXISTS(SELECT 1 FROM ReporteProblema WHERE sector=:id) AND NOT EXISTS(SELECT 1 FROM Lectura WHERE sector=:id)") suspend fun removeEmpty(id: Long): Int
}
@Database(entities = [Usuario::class, Sector::class, Cultivo::class, Tarea::class, Evidencia::class, Validacion::class, ReporteProblema::class, Seguimiento::class, Evento::class, Lectura::class], version = 1, exportSchema = false)
abstract class HuertoDb : RoomDatabase() { abstract fun dao(): HuertoDao }
