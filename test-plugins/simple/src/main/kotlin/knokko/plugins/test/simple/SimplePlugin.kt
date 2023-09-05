package knokko.plugins.test.simple

import knokko.plugin.PluginInstance
import knokko.plugin.PluginsLoadedListener

@Suppress("unused")
class SimplePlugin: PluginsLoadedListener {
    private var ownTestCounter = 0

    override fun afterPluginsLoaded(instance: PluginInstance) {
        ownTestCounter += 1
        SimplePluginStore.testCounter = ownTestCounter
    }
}
