package cl.huerto.data

import androidx.room.*

@Entity
data class Usuario(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val claveHash: String,
    val rol: String,
    val grupo: String,
    val activo: Boolean = true
)

@Entity(foreignKeys = [
    ForeignKey(entity = Usuario::class, parentColumns = ["id"], childColumns = ["responsable"])
], indices = [Index("responsable")])
data class Sector(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val ubicacion: String,
    val descripcion: String,
    val estado: String,
    val responsable: Long,
    val creado: Long,
    val actualizado: Long
)

@Entity(foreignKeys = [
    ForeignKey(entity = Sector::class, parentColumns = ["id"], childColumns = ["sector"])
], indices = [Index("sector")])
data class Cultivo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sector: Long,
    val nombre: String,
    val descripcion: String,
    val siembra: String,
    val trasplante: String,
    val cosecha: String,
    val riego: Int,
    val estado: String,
    val observaciones: String,
    val foto: String
)

@Entity(foreignKeys = [
    ForeignKey(entity = Cultivo::class, parentColumns = ["id"], childColumns = ["cultivo"]),
    ForeignKey(entity = Usuario::class, parentColumns = ["id"], childColumns = ["responsable"]),
    ForeignKey(entity = Usuario::class, parentColumns = ["id"], childColumns = ["autor"])
], indices = [Index("cultivo"), Index("responsable"), Index("autor")])
data class Tarea(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cultivo: Long,
    val responsable: Long,
    val autor: Long,
    val tipo: String,
    val descripcion: String,
    val fecha: String,
    val estado: String,
    val creado: Long
)

@Entity(foreignKeys = [
    ForeignKey(entity = Tarea::class, parentColumns = ["id"], childColumns = ["tarea"]),
    ForeignKey(entity = Usuario::class, parentColumns = ["id"], childColumns = ["autor"])
], indices = [Index("tarea"), Index("autor")])
data class Evidencia(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tarea: Long,
    val autor: Long,
    val comentario: String,
    val foto: String,
    val fecha: Long
)

@Entity(foreignKeys = [
    ForeignKey(entity = Evidencia::class, parentColumns = ["id"], childColumns = ["evidencia"]),
    ForeignKey(entity = Usuario::class, parentColumns = ["id"], childColumns = ["autor"])
], indices = [Index("evidencia"), Index("autor")])
data class Validacion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val evidencia: Long,
    val autor: Long,
    val resultado: String,
    val comentario: String,
    val fecha: Long
)

@Entity(foreignKeys = [
    ForeignKey(entity = Sector::class, parentColumns = ["id"], childColumns = ["sector"]),
    ForeignKey(entity = Cultivo::class, parentColumns = ["id"], childColumns = ["cultivo"]),
    ForeignKey(entity = Usuario::class, parentColumns = ["id"], childColumns = ["autor"])
], indices = [Index("sector"), Index("cultivo"), Index("autor")])
data class ReporteProblema(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sector: Long,
    val cultivo: Long?,
    val autor: Long,
    val tipo: String,
    val descripcion: String,
    val foto: String,
    val estado: String,
    val fecha: Long
)

@Entity(foreignKeys = [
    ForeignKey(entity = ReporteProblema::class, parentColumns = ["id"], childColumns = ["reporte"]),
    ForeignKey(entity = Usuario::class, parentColumns = ["id"], childColumns = ["autor"])
], indices = [Index("reporte"), Index("autor")])
data class Seguimiento(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reporte: Long,
    val autor: Long,
    val estado: String,
    val comentario: String,
    val fecha: Long
)

@Entity(foreignKeys = [
    ForeignKey(entity = Tarea::class, parentColumns = ["id"], childColumns = ["tarea"]),
    ForeignKey(entity = Usuario::class, parentColumns = ["id"], childColumns = ["autor"])
], indices = [Index("tarea"), Index("autor")])
data class Evento(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tarea: Long,
    val autor: Long,
    val estado: String,
    val fecha: Long
)

@Entity(foreignKeys = [
    ForeignKey(entity = Sector::class, parentColumns = ["id"], childColumns = ["sector"])
], indices = [Index("sector")])
data class Lectura(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sector: Long,
    val suelo: Int,
    val temperatura: Int,
    val aire: Int,
    val estado: String,
    val fecha: Long
)
