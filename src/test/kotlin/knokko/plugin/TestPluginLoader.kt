package knokko.plugin

import knokko.plugin.loading.JarContent
import knokko.plugin.loading.PluginClassLoader
import knokko.plugin.loading.scanDirectoriesOfClasses
import knokko.plugin.loading.scanJar
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File
import java.util.jar.JarInputStream

class TestPluginLoader {

    @Test
    fun testSimpleJarPluginLoader() {
        val simpleTestPluginContent =
            scanJar(JarInputStream(ClassLoader.getSystemResourceAsStream("knokko/plugins/test/simple.jar")))
        testSimplePluginLoader(listOf(Pair(simpleTestPluginContent, PluginInstance.createTestInstance("simple"))))
    }

    // NOTE: This test will fail before running ./gradlew shadowJar
    @Test
    fun testSimpleClassesPluginLoader() {
        val simpleTestPluginContent = runBlocking { scanDirectoriesOfClasses(listOf(
            File("test-plugins/simple/build/classes/java/main"),
            File("test-plugins/simple/build/classes/kotlin/main")
        )) }
        testSimplePluginLoader(listOf(Pair(simpleTestPluginContent, PluginInstance.createTestInstance("simple"))))
    }

    private fun testSimplePluginLoader(content: Collection<Pair<JarContent, PluginInstance>>) {
        val classLoader = PluginClassLoader(content)
        val pluginManager = PluginManager(classLoader.magicInstances)

        repeat(2) {
            val loadListeners = pluginManager.getImplementations(PluginsLoadedListener::class)
            assertEquals(1, loadListeners.size)

            val firstLoadListener = loadListeners.first()
            firstLoadListener.first.afterPluginsLoaded(firstLoadListener.second)
        }

        assertEquals(2,
            Class.forName("knokko.plugins.test.simple.SimplePluginStore", true, classLoader)
                .getField("testCounter").get(null)
        )
    }

    @Test
    fun testTwinJarPluginLoader() {
        val twinContentA = scanJar(JarInputStream(ClassLoader.getSystemResourceAsStream("knokko/plugins/test/twinA.jar")))
        val twinContentB = scanJar(JarInputStream(ClassLoader.getSystemResourceAsStream("knokko/plugins/test/twinB.jar")))
        testTwinPluginLoader(twinContentA, twinContentB)
    }

    // NOTE: This test will fail before running ./gradlew shadowJar
    @Test
    fun testTwinClassesPluginLoader() {
        val twinPluginContentA = runBlocking { scanDirectoriesOfClasses(listOf(
            File("test-plugins/twin-a/build/classes/kotlin/main"),
            File("test-plugins/twin-a/build/resources/main"),
        )) }
        val twinPluginContentB = runBlocking { scanDirectoriesOfClasses(listOf(
            File("test-plugins/twin-b/build/classes/kotlin/main"),
            File("test-plugins/twin-b/build/classes/java/main"),
            File("test-plugins/twin-b/build/resources/main")
        )) }
        testTwinPluginLoader(twinPluginContentA, twinPluginContentB)
    }

    private fun testTwinPluginLoader(twinContentA: JarContent, twinContentB: JarContent) {
        val twinContent = listOf(
            Pair(twinContentA, PluginInstance.createTestInstance("twinA")),
            Pair(twinContentB, PluginInstance.createTestInstance("twinB"))
        )
        val classLoader = PluginClassLoader(twinContent)
        val pluginManager = PluginManager(classLoader.magicInstances)

        repeat(2) {
            val loadListeners = pluginManager.getImplementations(PluginsLoadedListener::class)
            assertEquals(1, loadListeners.size)
            val firstListener = loadListeners.first()
            firstListener.first.afterPluginsLoaded(firstListener.second)
        }

        assertEquals("abcdef",
            Class.forName("knokko.plugins.test.twin.TwinStore", true, classLoader)
                .getField("testString").get(null)
        )
    }
}
