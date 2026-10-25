/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.domain.mojang

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement

/** One element of the modern `arguments.game` or `arguments.jvm` array. */
sealed interface ArgumentEntry {
    data class Literal(val value: String) : ArgumentEntry

    data class Conditional(
        val rules: List<Rule>,
        val values: List<String>,
    ) : ArgumentEntry
}

/**
 * Parses and expands the argument templates from a version JSON.
 *
 * Macro expansion is strict. An unknown `${...}` is a metadata failure, not a
 * string left in the command line: a launcher that silently passes
 * `${auth_access_token}` to the game has a very confusing bug.
 */
object ArgumentTemplate {
    private val MACRO = Regex("\\$\\{([^}]+)}")

    fun parse(entries: List<JsonElement>): List<ArgumentEntry> = entries.map(::parseOne)

    private fun parseOne(element: JsonElement): ArgumentEntry = when (element) {
        is JsonPrimitive -> {
            if (!element.isString) {
                throw MetadataException("argument must be a string or an object: $element")
            }
            ArgumentEntry.Literal(element.content)
        }

        is JsonObject -> {
            val rules = element["rules"]?.let {
                MojangJson.decodeFromJsonElement<List<Rule>>(it)
            } ?: emptyList()
            val value = element["value"]
                ?: throw MetadataException("argument object has no value: $element")
            ArgumentEntry.Conditional(rules = rules, values = parseValues(value))
        }

        else -> throw MetadataException("argument must be a string or an object: $element")
    }

    private fun parseValues(value: JsonElement): List<String> = when (value) {
        is JsonPrimitive -> {
            if (!value.isString) {
                throw MetadataException("argument value must be a string: $value")
            }
            listOf(value.content)
        }

        is JsonArray -> value.map {
            val primitive = it as? JsonPrimitive
                ?: throw MetadataException("argument value must be a string: $it")
            if (!primitive.isString) {
                throw MetadataException("argument value must be a string: $it")
            }
            primitive.content
        }

        else -> throw MetadataException("argument value must be a string or array: $value")
    }

    /**
     * Expands every matching entry in order. [default] is `false` for modern
     * arguments: an entry with no matching rule is omitted.
     */
    fun expand(
        entries: List<ArgumentEntry>,
        context: LaunchContext,
        values: Map<String, String>,
        default: Boolean = false,
    ): List<String> = buildList {
        for (entry in entries) {
            when (entry) {
                is ArgumentEntry.Literal -> add(expandMacros(entry.value, values))
                is ArgumentEntry.Conditional -> {
                    if (RuleEvaluator.allows(entry.rules, context, default)) {
                        entry.values.mapTo(this) { expandMacros(it, values) }
                    }
                }
            }
        }
    }

    fun expandMacros(value: String, values: Map<String, String>): String =
        MACRO.replace(value) { match ->
            val name = match.groupValues[1]
            values[name] ?: throw MetadataException("unknown argument macro: \${$name}")
        }

    /**
     * Normalises the pre-1.13 `minecraftArguments` string into the same entry
     * type. It has no rules, and its values are separated by whitespace; the
     * quotes are honoured so a path with a space stays one argument.
     */
    fun legacy(minecraftArguments: String): List<ArgumentEntry> =
        splitLegacy(minecraftArguments).map(ArgumentEntry::Literal)

    private fun splitLegacy(value: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var quote: Char? = null
        var escaped = false

        for (character in value) {
            when {
                escaped -> {
                    current.append(character)
                    escaped = false
                }

                character == '\\' -> escaped = true

                quote != null && character == quote -> quote = null

                quote == null && (character == '"' || character == '\'') -> quote = character

                quote == null && character.isWhitespace() -> {
                    if (current.isNotEmpty()) {
                        result.add(current.toString())
                        current.clear()
                    }
                }

                else -> current.append(character)
            }
        }
        if (escaped) current.append('\\')
        if (quote != null) throw MetadataException("unterminated quote in legacy arguments")
        if (current.isNotEmpty()) result.add(current.toString())
        return result
    }
}
