package knokko.plugin.loading

import knokko.plugin.PluginInfo
import knokko.plugin.PluginInstance
import kotlinx.coroutines.*
import java.io.File
import java.nio.file.Files
import java.util.jar.JarInputStream

@Throws(InvalidPluginsFolderException::class)
suspend fun scanDefaultPluginLocations(
        prepareStandardJars: () -> Unit,
        developmentProjects: Set<String>,
        pluginsFolder: File = File("plug-ins/"),
): Collection<Pair<JarContent, PluginInstance>> {
    if (pluginsFolder.exists() && !pluginsFolder.isDirectory) {
        throw InvalidPluginsFolderException("Plug-ins folder exists, but is not a directory")
    }
    if (!pluginsFolder.isDirectory && !pluginsFolder.mkdirs()) {
        throw InvalidPluginsFolderException("Failed to create plug-ins folder")
    }

    prepareStandardJars()

    val thirdPartyContent = scanDirectoryOfJars(pluginsFolder)

    val developmentContent = developmentProjects.map { name ->
        val buildDirectories = listOf(
            File("$name/build/classes/kotlin/main"),
            File("$name/build/classes/java/main"),
            File("$name/build/resources/main")
        )
        val projectContent = scanDirectoriesOfClasses(buildDirectories)
        Pair(projectContent, PluginInstance(PluginInfo(
            name = name.replace("-plugin", ""))
        ))
    }

    return thirdPartyContent + developmentContent
}

fun scanJar(jarStream: JarInputStream): JarContent {
    var currentEntry = jarStream.nextJarEntry

    val classMap = mutableMapOf<String, ByteArray>()
    val resourceMap = mutableMapOf<String, ByteArray>()

    while (currentEntry != null) {
        if (currentEntry.size < 1_000_000_000) {
            // Skip META-INF
            if (!currentEntry.name.startsWith("META-INF")) {
                if (currentEntry.name.endsWith(".class")) {
                    classMap[classPathToName(currentEntry.name)] = jarStream.readAllBytes()
                } else if (!currentEntry.isDirectory) {
                    resourceMap[currentEntry.name] = jarStream.readAllBytes()
                }
            }
        } else {
            throw IllegalArgumentException("Skipping ridiculously big entry ${currentEntry.name}")
        }
        currentEntry = jarStream.nextJarEntry
    }
    jarStream.close()

    return JarContent(classMap, resourceMap)
}

fun classPathToName(classPath: String): String {
    return classPath.substring(0 until classPath.length - 6).replace('/', '.')
}

suspend fun scanDirectoryOfJars(rootDirectory: File): Collection<Pair<JarContent, PluginInstance>> = withContext(Dispatchers.IO) {
    val jarList = mutableListOf<Pair<JarContent, PluginInstance>>()
    scanJarOrDirectory(this, rootDirectory, jarList)
    return@withContext jarList
}

suspend fun scanDirectoriesOfClasses(rootDirectories: Collection<File>): JarContent = withContext(Dispatchers.IO) {
    val classByteMap = mutableMapOf<String, ByteArray>()
    val resourceByteMap = mutableMapOf<String, ByteArray>()
    for (rootDirectory in rootDirectories) {
        if (rootDirectory.exists()) {
            if (!rootDirectory.isDirectory) {
                throw IllegalArgumentException("$rootDirectory is not a directory")
            }
            for (rootChild in rootDirectory.listFiles()!!) {
                scanClassOrResourceOrDirectory(this, rootChild, "", classByteMap, resourceByteMap)
            }
        }
    }
    return@withContext JarContent(classByteMap, resourceByteMap)
}

suspend fun scanDirectoryOfClasses(rootDirectory: File): JarContent {
    return scanDirectoriesOfClasses(listOf(rootDirectory))
}

private fun scanClassOrResourceOrDirectory(
    scope: CoroutineScope, file: File, parentRelativePath: String,
    classByteMap: MutableMap<String, ByteArray>, resourceByteMap: MutableMap<String, ByteArray>
) {
    if (file.isFile) {
        val resourceName = "$parentRelativePath${file.name}"
        if (file.name.endsWith(".class")) {
            val className = classPathToName(resourceName)
            scope.launch {
                val bytes = Files.readAllBytes(file.toPath())
                synchronized(classByteMap) {
                    classByteMap[className] = bytes
                }
            }
        } else {
            scope.launch {
                val bytes = Files.readAllBytes(file.toPath())
                synchronized(resourceByteMap) {
                    resourceByteMap[resourceName] = bytes
                }
            }
        }
    } else if (file.isDirectory) {
        val newRelativePath = "$parentRelativePath${file.name}/"
        for (childFile in file.listFiles()!!) {
            scope.launch {
                scanClassOrResourceOrDirectory(scope, childFile, newRelativePath, classByteMap, resourceByteMap)
            }
        }
    }
}

private fun scanJarOrDirectory(
        scope: CoroutineScope, file: File, jarList: MutableCollection<Pair<JarContent, PluginInstance>>,
) {
    if (file.isFile) {
        if (file.name.endsWith(".jar")) {
            scope.launch {
                val jarContent = scanJar(JarInputStream(Files.newInputStream(file.toPath())))
                // TODO Give plug-ins the opportunity to choose a different name than their file name
                val pluginInstance = PluginInstance(
                    info = PluginInfo(name = file.name.substring(0 until file.name.length - 4))
                )
                synchronized(jarList) {
                    jarList.add(Pair(jarContent, pluginInstance))
                }
            }
        }
    } else if (file.isDirectory) {
        val children = file.listFiles()!!
        for (child in children) {
            scope.launch {
                scanJarOrDirectory(scope, child, jarList)
            }
        }
    }
}

/**
 * Represents the content (class and resources) of 1 JAR file
 */
class JarContent(val classByteMap: Map<String, ByteArray>, val resourceByteMap: Map<String, ByteArray>)
