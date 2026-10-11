package me.shadow.eclipselauncher.json

class JsonNull private constructor() : JsonElement() {

    override fun toString(): String {
        return "null"
    }

    companion object {
        @JvmField
        val INSTANCE = JsonNull()
    }
}
