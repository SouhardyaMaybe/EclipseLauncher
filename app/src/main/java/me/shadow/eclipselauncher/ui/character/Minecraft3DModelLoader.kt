/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * Ported exactly from dev.obsidianreborn.android.ui.launcher.Minecraft3DModelLoader
 */
package me.shadow.eclipselauncher.ui.character

import android.content.Context
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.InputStream
import java.io.InputStreamReader

object Minecraft3DModelLoader {
    private const val ASSET_PATH = "models/player_models.json"

    private val OVERLAY_PART_NAMES = setOf(
        "hat",
        "jacket",
        "right_sleeve",
        "left_sleeve",
        "right_pants",
        "left_pants"
    )

    private var cachedSteve: ModelPart3D? = null
    private var cachedAlex: ModelPart3D? = null

    fun getPlayerModel(context: Context, slim: Boolean): ModelPart3D {
        return if (slim) {
            cachedAlex ?: loadModel(context, true).also { cachedAlex = it }
        } else {
            cachedSteve ?: loadModel(context, false).also { cachedSteve = it }
        }
    }

    private fun loadModel(context: Context, slim: Boolean): ModelPart3D {
        return try {
            val inputStream = context.assets.open(ASSET_PATH)
            inputStream.use { stream ->
                val reader = InputStreamReader(stream)
                val jsonText = reader.readText()
                reader.close()
                val root = JSONObject(jsonText)
                val variantKey = if (slim) "player_slim" else "player"
                val variantObj = root.getJSONObject(variantKey)
                parsePart("root", variantObj, false)
            }
        } catch (e: Exception) {
            buildFallbackModel()
        }
    }

    private fun parsePart(name: String, json: JSONObject, isOverlay: Boolean): ModelPart3D {
        val poseArray = json.optJSONArray("pose")
        val px = poseArray?.optDouble(0, 0.0)?.toFloat() ?: 0f
        val py = poseArray?.optDouble(1, 0.0)?.toFloat() ?: 0f
        val pz = poseArray?.optDouble(2, 0.0)?.toFloat() ?: 0f

        val quads = mutableListOf<Quad3D>()
        val polysArray = json.optJSONArray("polygons")
        if (polysArray != null) {
            for (i in 0 until polysArray.length()) {
                val quadArray = polysArray.optJSONArray(i)
                if (quadArray != null && quadArray.length() >= 4) {
                    val v0 = parseVertex(quadArray.getJSONArray(0))
                    val v1 = parseVertex(quadArray.getJSONArray(1))
                    val v2 = parseVertex(quadArray.getJSONArray(2))
                    val v3 = parseVertex(quadArray.getJSONArray(3))
                    quads.add(Quad3D(v0, v1, v2, v3, isOverlay))
                }
            }
        }

        val children = mutableListOf<ModelPart3D>()
        val childrenObj = json.optJSONObject("children")
        if (childrenObj != null) {
            val keys = childrenObj.keys()
            while (keys.hasNext()) {
                val childName = keys.next()
                val childJson = childrenObj.getJSONObject(childName)
                val childOverlay = childName in OVERLAY_PART_NAMES
                children.add(parsePart(childName, childJson, childOverlay))
            }
        }

        return ModelPart3D(name, px, py, pz, quads, children, isOverlay)
    }

    private fun parseVertex(arr: org.json.JSONArray): Vertex3D {
        return Vertex3D(
            x = arr.optDouble(0, 0.0).toFloat(),
            y = arr.optDouble(1, 0.0).toFloat(),
            z = arr.optDouble(2, 0.0).toFloat(),
            u = arr.optDouble(3, 0.0).toFloat(),
            v = arr.optDouble(4, 0.0).toFloat()
        )
    }

    private fun buildFallbackModel(): ModelPart3D {
        val box = Quad3D(
            Vertex3D(-4f, -8f, -4f, 0f, 0f),
            Vertex3D(-4f, 24f, -4f, 0f, 64f),
            Vertex3D(4f, 24f, -4f, 64f, 64f),
            Vertex3D(4f, -8f, -4f, 64f, 0f),
            false
        )
        return ModelPart3D("root", 0f, 0f, 0f, listOf(box), emptyList(), false)
    }
}
