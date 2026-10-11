package me.shadow.eclipselauncher.core.customcontrols

import android.content.Context

import me.shadow.eclipselauncher.json.JsonSyntaxException
import me.shadow.eclipselauncher.R

import me.shadow.eclipselauncher.core.LwjglGlfwKeycode
import me.shadow.eclipselauncher.core.Tools

import me.shadow.eclipselauncher.utils.io.IOUtils
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.lwjgl.glfw.CallbackBridge

import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.ArrayList

class LayoutConverter {

    companion object {
        @JvmStatic
        @Throws(IOException::class, JsonSyntaxException::class)
        fun loadAndConvertIfNecessary(ctx: Context, jsonPath: String): CustomControls? {
            val jsonLayoutData = Tools.read(jsonPath)
            return try {
                val layoutJobj = JSONObject(jsonLayoutData)
                loadAndConvertIfNecessary(ctx, layoutJobj, jsonLayoutData, jsonPath, true)
            } catch (e: Exception) {
                Tools.showError(ctx, ctx.getString(R.string.controls_load_failed), e)
                null
            }
        }

        @JvmStatic
        fun loadFromAssets(context: Context, jsonName: String): CustomControls? {
            try {
                context.assets.open(jsonName).use { is: InputStream ->
                    val string = IOUtils.toString(is, StandardCharsets.UTF_8)

                    val layoutJobj = JSONObject(string)
                    return loadAndConvertIfNecessary(context, layoutJobj, string, null, true)
                }
            } catch (e: Exception) {
                Tools.showError(context, context.getString(R.string.controls_load_failed), e)
                return null
            }
        }

        @JvmStatic
        fun loadFromJsonObject(
            ctx: Context,
            layoutJobj: JSONObject,
            jsonString: String,
            jsonPath: String?,
            showError: Boolean
        ): CustomControls? {
            return try {
                loadAndConvertIfNecessary(ctx, layoutJobj, jsonString, jsonPath, showError)
            } catch (e: Exception) {
                if (showError) Tools.showError(ctx, ctx.getString(R.string.controls_load_failed), e)
                null
            }
        }

        @Throws(Exception::class)
        private fun loadAndConvertIfNecessary(
            ctx: Context,
            layoutJobj: JSONObject,
            jsonLayoutData: String,
            jsonPath: String?,
            showError: Boolean
        ): CustomControls? {
            if (!layoutJobj.has("version")) { //v1 layout
                val layout = convertV1Layout(layoutJobj)
                if (jsonPath != null) layout.save(jsonPath)
                return layout
            } else {
                val version = layoutJobj.getInt("version")
                if (version == 2) {
                    val layout = convertV2Layout(layoutJobj)
                    if (jsonPath != null) layout.save(jsonPath)
                    return layout
                }
                if (version == 3 || version == 4 || version == 5) {
                    return convertV3_4Layout(layoutJobj)
                }
                if (version == 6 || version == 7) {
                    return convertV6_7Layout(layoutJobj)
                }
                if (version == 8) {
                    return Tools.JSON_CODEC.fromJson(jsonLayoutData, CustomControls::class.java)
                }
                if (showError) {
                    val ioException = IOException()
                    Tools.showError(ctx, ctx.getString(R.string.controls_unsupported_layout_version), ioException)
                }
                return null
            }
        }

        /**
         * Normalize the layout to v8 from v6/7. An issue from the joystick height and position has to be fixed.
         * @param oldLayoutJson The old layout
         * @return The new layout with the fixed joystick height
         */
        @JvmStatic
        fun convertV6_7Layout(oldLayoutJson: JSONObject): CustomControls {
            val layout = Tools.JSON_CODEC.fromJson(oldLayoutJson.toString(), CustomControls::class.java)
            for (data in layout.mJoystickDataList) {
                if (data.getHeight() > data.getWidth()) {
                    // Make the size square, adjust the dynamic position related to height
                    val ratio = data.getHeight() / data.getWidth()
                    data.dynamicX = data.dynamicX.replace("\${height}", "(" + ratio + " * \${height})")
                    data.dynamicY = data.dynamicY.replace("\${height}", "(" + ratio + " * \${height})") + " + (" + (ratio - 1) + " * \${height})"
                    data.setHeight(data.getWidth())
                }
            }
            layout.version = 8
            return layout
        }

        /**
         * Normalize the layout to v6 from v3/4: The stroke width is no longer dependant on the button size
         */
        private fun convertV3_4Layout(oldLayoutJson: JSONObject): CustomControls {
            val layout = Tools.JSON_CODEC.fromJson(oldLayoutJson.toString(), CustomControls::class.java)
            convertStrokeWidth(layout)
            layout.version = 6
            return layout
        }

        @Throws(JSONException::class)
        private fun convertV2Layout(oldLayoutJson: JSONObject): CustomControls {
            val layout = Tools.JSON_CODEC.fromJson(oldLayoutJson.toString(), CustomControls::class.java)
            val layoutMainArray = oldLayoutJson.getJSONArray("mControlDataList")
            layout.mControlDataList = ArrayList(layoutMainArray.length())
            for (i in 0 until layoutMainArray.length()) {
                val button = layoutMainArray.getJSONObject(i)
                val n_button = Tools.JSON_CODEC.fromJson(button.toString(), ControlData::class.java)
                if (!Tools.isValidString(n_button.dynamicX) && button.has("x")) {
                    val buttonC = button.getDouble("x")
                    val ratio = buttonC / CallbackBridge.physicalWidth
                    n_button.dynamicX = ratio.toString() + " * \${screen_width}"
                }
                if (!Tools.isValidString(n_button.dynamicY) && button.has("y")) {
                    val buttonC = button.getDouble("y")
                    val ratio = buttonC / CallbackBridge.physicalHeight
                    n_button.dynamicY = ratio.toString() + " * \${screen_height}"
                }
                layout.mControlDataList.add(n_button)
            }
            val layoutDrawerArray = oldLayoutJson.getJSONArray("mDrawerDataList")
            layout.mDrawerDataList = ArrayList()
            for (i in 0 until layoutDrawerArray.length()) {
                val button = layoutDrawerArray.getJSONObject(i)
                val buttonProperties = button.getJSONObject("properties")
                val n_button = Tools.JSON_CODEC.fromJson(button.toString(), ControlDrawerData::class.java)
                if (!Tools.isValidString(n_button.properties.dynamicX) && buttonProperties.has("x")) {
                    val buttonC = buttonProperties.getDouble("x")
                    val ratio = buttonC / CallbackBridge.physicalWidth
                    n_button.properties.dynamicX = ratio.toString() + " * \${screen_width}"
                }
                if (!Tools.isValidString(n_button.properties.dynamicY) && buttonProperties.has("y")) {
                    val buttonC = buttonProperties.getDouble("y")
                    val ratio = buttonC / CallbackBridge.physicalHeight
                    n_button.properties.dynamicY = ratio.toString() + " * \${screen_height}"
                }
                layout.mDrawerDataList.add(n_button)
            }
            convertStrokeWidth(layout)

            layout.version = 3
            return layout
        }

        @Throws(JSONException::class)
        private fun convertV1Layout(oldLayoutJson: JSONObject): CustomControls {
            val empty = CustomControls()
            val layoutMainArray = oldLayoutJson.getJSONArray("mControlDataList")
            for (i in 0 until layoutMainArray.length()) {
                val button = layoutMainArray.getJSONObject(i)
                val n_button = ControlData()
                val keycodes = intArrayOf(
                    LwjglGlfwKeycode.GLFW_KEY_UNKNOWN.toInt(),
                    LwjglGlfwKeycode.GLFW_KEY_UNKNOWN.toInt(),
                    LwjglGlfwKeycode.GLFW_KEY_UNKNOWN.toInt(),
                    LwjglGlfwKeycode.GLFW_KEY_UNKNOWN.toInt()
                )
                n_button.dynamicX = button.getString("dynamicX")
                n_button.dynamicY = button.getString("dynamicY")
                if (!Tools.isValidString(n_button.dynamicX) && button.has("x")) {
                    val buttonC = button.getDouble("x")
                    val ratio = buttonC / CallbackBridge.physicalWidth
                    n_button.dynamicX = ratio.toString() + " * \${screen_width}"
                }
                if (!Tools.isValidString(n_button.dynamicY) && button.has("y")) {
                    val buttonC = button.getDouble("y")
                    val ratio = buttonC / CallbackBridge.physicalHeight
                    n_button.dynamicY = ratio.toString() + " * \${screen_height}"
                }
                n_button.name = button.getString("name")
                n_button.opacity = ((button.getInt("transparency") - 100) * -1).toFloat() / 100f
                n_button.passThruEnabled = button.getBoolean("passThruEnabled")
                n_button.isToggle = button.getBoolean("isToggle")
                n_button.setHeight(button.getInt("height").toFloat())
                n_button.setWidth(button.getInt("width").toFloat())
                n_button.bgColor = 0x4d000000
                n_button.strokeWidth = 0f
                if (button.getBoolean("isRound")) {
                    n_button.cornerRadius = 35f
                }
                var next_idx = 0
                if (button.getBoolean("holdShift")) {
                    keycodes[next_idx] = LwjglGlfwKeycode.GLFW_KEY_LEFT_SHIFT.toInt()
                    next_idx++
                }
                if (button.getBoolean("holdCtrl")) {
                    keycodes[next_idx] = LwjglGlfwKeycode.GLFW_KEY_LEFT_CONTROL.toInt()
                    next_idx++
                }
                if (button.getBoolean("holdAlt")) {
                    keycodes[next_idx] = LwjglGlfwKeycode.GLFW_KEY_LEFT_ALT.toInt()
                    next_idx++
                }
                keycodes[next_idx] = button.getInt("keycode")
                n_button.keycodes = keycodes
                empty.mControlDataList.add(n_button)
            }
            empty.scaledAt = oldLayoutJson.getDouble("scaledAt").toFloat()
            empty.version = 3
            return empty
        }

        /**
         * Convert the layout stroke width to the V5 form
         */
        private fun convertStrokeWidth(layout: CustomControls) {
            for (data in layout.mControlDataList) {
                data.strokeWidth = Tools.pxToDp(computeStrokeWidth(data.strokeWidth, data.getWidth(), data.getHeight()).toFloat())
            }

            for (data in layout.mDrawerDataList) {
                data.properties.strokeWidth = Tools.pxToDp(
                    computeStrokeWidth(data.properties.strokeWidth, data.properties.getWidth(), data.properties.getHeight()).toFloat()
                )
                for (subButtonData in data.buttonProperties) {
                    subButtonData.strokeWidth = Tools.pxToDp(
                        computeStrokeWidth(subButtonData.strokeWidth, data.properties.getWidth(), data.properties.getWidth()).toFloat()
                    )
                }
            }
        }

        /**
         * Convert a size percentage into a px size, used by older layout versions
         */
        private fun computeStrokeWidth(widthInPercent: Float, width: Float, height: Float): Int {
            val maxSize = Math.max(width, height)
            return ((maxSize / 2) * (widthInPercent / 100)).toInt()
        }
    }
}
