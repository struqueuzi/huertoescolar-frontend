package cl.huerto.domain

import java.time.LocalDate
import java.security.MessageDigest

const val ADMIN = "Administrador"
const val STUDENT = "Colaborador"
const val PENDING = "Pendiente"
const val WORKING = "En desarrollo"
const val SUBMITTED = "Pendiente de validación"
const val APPROVED = "Validada"
const val REJECTED = "Rechazada"
val taskStates = listOf(PENDING, WORKING, SUBMITTED, APPROVED, REJECTED)
val sectorStates = listOf("Disponible","En uso","En mantenimiento","Fuera de uso")
val cropStates = listOf("Planificado","Sembrado","En crecimiento","Listo para cosechar","Cosechado","Con problemas","Finalizado")
val reportStates = listOf("Informado","En revisión","Atendido","Cerrado")
val types = listOf("Riego","Siembra","Mantenimiento","Observación","Trasplante","Cosecha","Limpieza","Revisión del suelo","Preparación del sector","Retiro de malezas","Registro del crecimiento","Otra actividad autorizada")
val problems = listOf("Falta de agua","Daño en la planta","Plaga simulada","Deterioro del sector","Problema de mantenimiento","Otro problema observado")
enum class Form { SECTOR, CULTIVO, TAREA, ENTREGA, REVISION, REPORTE, SEGUIMIENTO }
typealias Fields = Map<String,String>
fun Fields.value(key: String) = get(key).orEmpty().trim()
fun Fields.number(key: String) = value(key).toLongOrNull() ?: 0
class Invalid(val errors: Map<String,String>) : IllegalArgumentException("Revisa los campos indicados")
fun hash(value: String) = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
fun overdue(state: String, date: String, today: LocalDate = LocalDate.now()) =
    state in listOf(PENDING, WORKING, REJECTED) && date < today.toString()

object Rules {
    fun validate(form: Form, f: Fields): Map<String,String> {
        val errors = mutableMapOf<String,String>()
        fun required(vararg keys: String) = keys.forEach { if (f.value(it).isEmpty()) errors[it] = "Completa este campo." }
        fun choice(key: String, options: List<String>) { if (f.value(key) !in options) errors[key] = "Selecciona una opción válida." }
        fun date(key: String): LocalDate? = runCatching { LocalDate.parse(f.value(key)) }.getOrElse {
            errors[key] = "Usa una fecha válida con formato AAAA-MM-DD."
            null
        }
        when(form) {
            Form.SECTOR -> { required("Nombre","Ubicación","Descripción"); choice("Estado",sectorStates) }
            Form.CULTIVO -> {
                required("Nombre","Sector","Descripción")
                choice("Estado",cropStates)
                val from = date("Siembra")
                val to = date("Cosecha")
                if (from != null && to != null && to < from) errors["Cosecha"] = "La cosecha debe ser igual o posterior a la siembra."
                if (f.value("Trasplante").isNotEmpty()) {
                    val transplant = date("Trasplante")
                    if (transplant != null && ((from != null && transplant < from) || (to != null && transplant > to))) errors["Trasplante"] = "Debe quedar entre siembra y cosecha."
                }
                if ((f.value("Riego cada (días)").toIntOrNull() ?: 0) !in 1..365) errors["Riego cada (días)"] = "Ingresa entre 1 y 365 días."
            }
            Form.TAREA -> { required("Cultivo","Responsable","Instrucciones"); choice("Tipo",types); date("Fecha") }
            Form.ENTREGA -> required("Comentario","Foto")
            Form.REVISION -> { choice("Resultado",listOf(APPROVED,REJECTED)); if (f.value("Resultado") == REJECTED) required("Comentario") }
            Form.REPORTE -> { required("Sector","Descripción","Foto"); choice("Tipo",problems) }
            Form.SEGUIMIENTO -> { required("Comentario"); choice("Estado",reportStates) }
        }
        for (key in listOf("Nombre","Ubicación","Descripción","Instrucciones","Comentario","Observaciones")) {
            val max = if(key in listOf("Nombre","Ubicación")) 100 else 1000
            if(f.value(key).length > max) errors[key] = "Máximo $max caracteres."
        }
        return errors
    }
}
