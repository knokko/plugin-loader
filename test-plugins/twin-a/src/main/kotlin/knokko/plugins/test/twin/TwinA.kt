package knokko.plugins.test.twin

import java.util.*

fun getTestString(): String {
    fun parseTestResource(suffix: String): String {
        val testResource = DummyClass::class.java.classLoader.getResource("knokko/plugins/test/twin/string$suffix.txt")
        val testScanner = Scanner(testResource!!.openStream())
        val result = testScanner.next()
        testScanner.close()
        return result
    }

    return parseTestResource("A") + parseTestResource("B")
}

private class DummyClass
