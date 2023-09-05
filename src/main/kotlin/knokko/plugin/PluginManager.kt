package knokko.plugin

import kotlin.reflect.KClass

class PluginManager(private val magicInstances: Collection<Pair<MagicPluginInterface, PluginInstance>>) {

    private val magicInterfaceMap = mutableMapOf<Class<*>, Collection<*>>()

    /**
     * Gets a list of all plugin classes that implement the given 'magic' interface. That interface must extend
     * MagicPluginInterface!
     */
    @Synchronized
    @Suppress("UNCHECKED_CAST")
    fun <T: MagicPluginInterface> getImplementations(magicInterface: Class<T>): Collection<Pair<T, PluginInstance>> {
        return magicInterfaceMap.getOrPut(magicInterface) {
            if (!implementsOrExtendsInterface(magicInterface, MagicPluginInterface::class.java)) {
                throw IllegalArgumentException("$magicInterface doesn't extend MagicPluginInterface")
            }

            val untypedResult = magicInstances.filter { candidateInstance ->
                implementsOrExtendsInterface(candidateInstance.first::class.java, magicInterface)
            }
            val result = ArrayList<Pair<T, PluginInstance>>(untypedResult.size)
            for (element in untypedResult) {
                result.add(element as Pair<T, PluginInstance>)
            }
            result
        } as Collection<Pair<T, PluginInstance>>
    }

    fun <T: MagicPluginInterface> getImplementations(magicInterface: KClass<T>): Collection<Pair<T, PluginInstance>> {
        return getImplementations(magicInterface.java)
    }
}

private fun implementsOrExtendsInterface(candidate: Class<*>, desiredInterface: Class<*>): Boolean {
    return candidate.interfaces.any {
            candidateInterface -> candidateInterface == desiredInterface || implementsOrExtendsInterface(candidateInterface, desiredInterface)
    }
}
