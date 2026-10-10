package me.shadow.eclipselauncher.core.utils

/**
 * Inserts `${'$'}{key}` placeholders into argument strings from a key/value map.
 */
object JSONUtils {

    @JvmStatic
    fun insertJSONValueList(args: Array<String>, keyValueMap: Map<String, String?>): Array<String> {
        for (i in args.indices) {
            args[i] = insertSingleJSONValue(args[i], keyValueMap)
        }
        return args
    }

    @JvmStatic
    fun insertSingleJSONValue(value: String, keyValueMap: Map<String, String?>): String {
        var valueInserted = value
        for ((key, keyValue) in keyValueMap) {
            valueInserted = valueInserted.replace("\${$key}", keyValue ?: "")
        }
        return valueInserted
    }
}
