package knokko.plugins.test.twin

import knokko.plugin.PluginInstance
import knokko.plugin.PluginsLoadedListener

@Suppress("unused")
class TwinPluginB: PluginsLoadedListener {
    override fun afterPluginsLoaded(instance: PluginInstance) {
        TwinStore.testString = getTestString()
    }
}
