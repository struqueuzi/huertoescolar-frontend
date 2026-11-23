package cl.huerto.data

import android.graphics.*
import java.io.File

/** Illustration used only for the fictitious records installed on first launch. */
internal fun demoEvidence(directory: File): String {
    directory.mkdirs()
    val file=File(directory,"muestra.png")
    val bitmap=Bitmap.createBitmap(480,300,Bitmap.Config.ARGB_8888)
    val canvas=Canvas(bitmap)
    val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    canvas.drawColor(Color.rgb(239,240,216))
    paint.color=Color.rgb(84,66,39)
    canvas.drawRect(0f,210f,480f,300f,paint)
    paint.color=Color.rgb(62,105,65)
    canvas.drawRect(235f,115f,244f,215f,paint)
    canvas.drawOval(145f,83f,241f,145f,paint)
    canvas.drawOval(239f,60f,330f,120f,paint)
    paint.color=Color.WHITE
    paint.textSize=22f
    canvas.drawText("Huerto escolar · muestra ficticia",28f,258f,paint)
    file.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG,100,it)) }
    bitmap.recycle()
    return file.absolutePath
}
