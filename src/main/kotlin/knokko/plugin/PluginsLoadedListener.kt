package knokko.plugin

interface PluginsLoadedListener: MagicPluginInterface {

    fun afterPluginsLoaded(instance: PluginInstance)
}
