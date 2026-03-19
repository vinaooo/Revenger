import com.swordfish.libretrodroid.ShaderConfig

fun main() {
    val clazz = ShaderConfig::class.java
    println(clazz.name)
    println("Methods:")
    clazz.declaredMethods.forEach { println(it.name) }
    println("Fields:")
    clazz.declaredFields.forEach { println(it.name) }
}
