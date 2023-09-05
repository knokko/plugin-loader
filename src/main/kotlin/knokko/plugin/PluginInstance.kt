package knokko.plugin

class PluginInstance(
        /**
     * General information about the plug-in. Currently, just the name.
     */
    val info: PluginInfo,
        /**
     * The current state of the plug-in. Plug-ins can read from and write to this state whenever they want. Every
     * plug-in is free to set its state to whatever it wants.
     *
     * The state can be useful when a plug-in has multiple event listeners/actors that need to communicate with each
     * other (it avoids the need for work-arounds like static state for communication).
     */
    var state: Any? = null,
) {
    companion object {
        fun createTestInstance(pluginName: String) = PluginInstance(info = PluginInfo(name = pluginName))
    }
}

class PluginInfo(val name: String)
