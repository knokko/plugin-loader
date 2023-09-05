package knokko.plugin.loading

import knokko.plugin.PluginInstance
import knokko.plugin.MagicPluginInterface
import java.lang.reflect.Modifier
import java.net.URI
import java.net.URL

private fun extractClasses(jars: Collection<Pair<JarContent, PluginInstance>>): Map<String, Pair<ByteArray, PluginInstance>> {
    val mapList = jars.map { jarPair ->
        jarPair.first.classByteMap.mapValues {
                entry -> Pair(entry.value, jarPair.second)
        }
    }

    val combinedMap = mutableMapOf<String, Pair<ByteArray, PluginInstance>>()
    for (map in mapList) {
        combinedMap.putAll(map)
    }
    return combinedMap
}

private fun extractResources(jars: Collection<Pair<JarContent, PluginInstance>>): Map<String, ByteArray> {
    val mapList = jars.map { pair -> pair.first.resourceByteMap }
    val combinedMap = mutableMapOf<String, ByteArray>()
    for (map in mapList) {
        combinedMap.putAll(map)
    }
    return combinedMap
}

class PluginClassLoader(
        private var classByteMap: Map<String, Pair<ByteArray, PluginInstance>>?,
        private val resourceMap: Map<String, ByteArray>
): ClassLoader() {

    constructor(jars: Collection<Pair<JarContent, PluginInstance>>) : this(extractClasses(jars), extractResources(jars))

    private val classMap = mutableMapOf<String, Pair<Class<*>, PluginInstance>>()
    private val urlHandler = PluginURLHandler(resourceMap)

    val magicInstances: Collection<Pair<MagicPluginInterface, PluginInstance>>

    private fun getOrCreateClass(name: String, knownEntry: Pair<ByteArray, PluginInstance>?): Class<*> {

        val existing = classMap[name]
        if (existing != null) return existing.first

        val newEntry = if (knownEntry == null && classByteMap != null) {
            classByteMap!![name]
        } else {
            knownEntry
        }

        if (newEntry != null) {
            val newClass = defineClass(name, newEntry.first, 0, newEntry.first.size)
            classMap[name] = Pair(newClass, newEntry.second)
            return newClass
        }

        throw ClassNotFoundException(name)
    }

    init {
        if (classByteMap == null) {
            throw IllegalArgumentException("classByteMap must not be null")
        }

        for ((name, classPair) in classByteMap!!) {
            getOrCreateClass(name, classPair)
        }

        // We don't need them anymore after class loading has finished, so allow the garbage collector to free them
        classByteMap = null

        val collectMagicClasses = mutableListOf<Pair<MagicPluginInterface, PluginInstance>>()
        for (classPair in ArrayList(classMap.values)) {
            val definedClass = classPair.first
            resolveClass(definedClass)

            if (!definedClass.isInterface && isMagic(definedClass)) {
                if ((definedClass.modifiers and Modifier.FINAL) == 0) {
                    throw InvalidPluginException("All classes that implement MagicPluginInterface must be final, but ${definedClass.name} isn't")
                }

                try {
                    collectMagicClasses.add(Pair(definedClass.getConstructor().newInstance() as MagicPluginInterface, classPair.second))
                } catch (noSuitableConstructor: NoSuchMethodException) {
                    throw InvalidPluginException("All classes that implement MagicPluginInterface must have a public constructor without parameters, but ${definedClass.name} doesn't")
                }
            }
        }

        magicInstances = List(collectMagicClasses.size) { index -> collectMagicClasses[index] }
    }

    private fun isMagic(candidate: Class<*>): Boolean {
        if (candidate == MagicPluginInterface::class.java) {
            return true
        }

        for (child in candidate.interfaces) {
            if (isMagic(child)) {
                return true
            }
        }

        return false
    }

    override fun findClass(name: String): Class<*> {
        return getOrCreateClass(name, null)
    }

    override fun findResource(name: String?): URL {
        val resourceBytes = resourceMap[name]
        return if (resourceBytes != null) {
            URL("knokkoplugin", "pluginname", 0, name, urlHandler)
        } else {
            println("resourceMap is $resourceMap and resource name is $name")
            super.findResource(name)
        }
    }
}
