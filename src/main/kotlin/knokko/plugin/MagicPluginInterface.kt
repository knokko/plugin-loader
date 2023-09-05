package knokko.plugin

/**
 * Whenever the `PluginClassLoader` loads a class, it will check whether it (indirectly) implements this 'magic'
 * interface. If so, it will register the class as an implementation of this interface (or subinterface).
 */
interface MagicPluginInterface
