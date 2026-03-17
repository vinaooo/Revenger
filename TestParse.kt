import org.json.JSONArray
import java.io.File

fun main() {
    val content = File("app/src/main/assets/default_settings.json").readText()
    try {
        val arr = JSONArray(content)
        val obj = arr.getJSONObject(0)
        println("Has button_allow: " + obj.has("button_allow_multiple_presses_action"))
        println("Has gp_allow: " + obj.has("gp_allow_multiple_presses_action"))
        val test = obj.getBoolean("button_allow_multiple_presses_action")
        println("Success")
    } catch (e: Exception) {
        println("Failed: " + e.message)
    }
}
