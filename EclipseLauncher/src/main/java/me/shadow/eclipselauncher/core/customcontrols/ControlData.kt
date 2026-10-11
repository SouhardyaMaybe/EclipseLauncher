package me.shadow.eclipselauncher.core.customcontrols

import android.content.Context
import android.util.ArrayMap

import androidx.annotation.Keep

import me.shadow.eclipselauncher.R
import me.shadow.eclipselauncher.context.ContextExecutor
import me.shadow.eclipselauncher.core.LwjglGlfwKeycode
import me.shadow.eclipselauncher.core.Tools
import me.shadow.eclipselauncher.core.customcontrols.buttons.ControlInterface
import me.shadow.eclipselauncher.core.utils.JSONUtils
import me.shadow.eclipselauncher.setting.AllSettings
import me.shadow.eclipselauncher.utils.stringutils.StringUtils

import net.objecthunter.exp4j.ExpressionBuilder
import net.objecthunter.exp4j.function.Function

import org.lwjgl.glfw.CallbackBridge

import java.lang.ref.WeakReference
import java.util.Collections

@Keep
open class ControlData {

    // Internal usage only
    @Transient
    @JvmField
    var isHideable: Boolean = false

    /**
     * Both fields below are dynamic position data, auto updates
     * X and Y position, unlike the original one which uses fixed
     * position, so it does not provide auto-location when a control
     * is made on a small device, then import the control to a
     * bigger device or vice versa.
     */
    @JvmField
    var dynamicX: String = ""
    @JvmField
    var dynamicY: String = ""
    @JvmField
    var isToggle: Boolean = false
    @JvmField
    var passThruEnabled: Boolean = false
    @JvmField
    var name: String = ""
    @JvmField
    var keycodes: IntArray = intArrayOf()      //Should store up to 4 keys
    @JvmField
    var opacity: Float = 0f       //Alpha value from 0 to 1;
    @JvmField
    var bgColor: Int = 0
    @JvmField
    var strokeColor: Int = 0
    @JvmField
    var strokeWidth: Float = 0f     // Dp instead of % now
    @JvmField
    var cornerRadius: Float = 0f  //0-100%
    @JvmField
    var isSwipeable: Boolean = false
    @JvmField
    var displayInGame: Boolean = false
    @JvmField
    var displayInMenu: Boolean = false
    private var width: Float = 0f         //Dp instead of Px now
    private var height: Float = 0f        //Dp instead of Px now

    constructor() : this(ContextExecutor.getString(R.string.controls_add_control_button))

    constructor(name: String) : this(name, intArrayOf())

    constructor(name: String, keycodes: IntArray) : this(
        name, keycodes, Tools.currentDisplayMetrics.widthPixels / 2f, Tools.currentDisplayMetrics.heightPixels / 2f
    )

    constructor(name: String, keycodes: IntArray, x: Float, y: Float) : this(name, keycodes, x, y, 50f, 50f)

    constructor(ctx: Context, resId: Int, keycodes: IntArray, x: Float, y: Float, isSquare: Boolean) :
            this(ctx.resources.getString(resId), keycodes, x, y, isSquare)

    constructor(name: String, keycodes: IntArray, x: Float, y: Float, isSquare: Boolean) :
            this(name, keycodes, x, y, if (isSquare) 50f else 80f, if (isSquare) 50f else 30f)

    constructor(name: String, keycodes: IntArray, x: Float, y: Float, width: Float, height: Float) :
            this(name, keycodes, x.toString(), y.toString(), width, height, false)

    constructor(name: String, keycodes: IntArray, dynamicX: String, dynamicY: String) :
            this(name, keycodes, dynamicX, dynamicY, 50f, 50f, false)

    constructor(ctx: Context, resId: Int, keycodes: IntArray, dynamicX: String, dynamicY: String, isSquare: Boolean) :
            this(ctx.resources.getString(resId), keycodes, dynamicX, dynamicY, isSquare)

    constructor(name: String, keycodes: IntArray, dynamicX: String, dynamicY: String, isSquare: Boolean) :
            this(name, keycodes, dynamicX, dynamicY, if (isSquare) 50f else 80f, if (isSquare) 50f else 30f, false)

    constructor(name: String, keycodes: IntArray, dynamicX: String, dynamicY: String, width: Float, height: Float, isToggle: Boolean) :
            this(name, keycodes, dynamicX, dynamicY, width, height, isToggle, 1f, 0x4D000000, 0xFFFFFFFF.toInt(), 0f, 0f, true, true, false, false)

    constructor(
        name: String, keycodes: IntArray, dynamicX: String, dynamicY: String, width: Float, height: Float,
        isToggle: Boolean, opacity: Float, bgColor: Int, strokeColor: Int, strokeWidth: Float, cornerRadius: Float,
        displayInGame: Boolean, displayInMenu: Boolean, isSwipable: Boolean, mousePassthrough: Boolean
    ) {
        this.name = name
        this.keycodes = inflateKeycodeArray(keycodes)
        this.dynamicX = dynamicX
        this.dynamicY = dynamicY
        this.width = width
        this.height = height
        this.isToggle = isToggle
        this.opacity = opacity
        this.bgColor = bgColor
        this.strokeColor = strokeColor
        this.strokeWidth = strokeWidth
        this.cornerRadius = cornerRadius
        this.displayInGame = displayInGame
        this.displayInMenu = displayInMenu
        this.isSwipeable = isSwipable
        this.passThruEnabled = mousePassthrough
    }

    //Deep copy constructor
    constructor(controlData: ControlData) : this(
        controlData.name,
        controlData.keycodes,
        controlData.dynamicX,
        controlData.dynamicY,
        controlData.width,
        controlData.height,
        controlData.isToggle,
        controlData.opacity,
        controlData.bgColor,
        controlData.strokeColor,
        controlData.strokeWidth,
        controlData.cornerRadius,
        controlData.displayInGame,
        controlData.displayInMenu,
        controlData.isSwipeable,
        controlData.passThruEnabled
    )

    fun insertDynamicPos(dynamicPos: String): Float {
        // Insert value to ${variable}
        val insertedPos = JSONUtils.insertSingleJSONValue(dynamicPos, fillConversionMap())

        // Calculate, because the dynamic position contains some math equations
        return calculate(insertedPos)
    }

    @Suppress("BooleanMethodIsAlwaysInverted")
    fun containsKeycode(keycodeToCheck: Int): Boolean {
        for (keycode in keycodes)
            if (keycodeToCheck == keycode)
                return true

        return false
    }

    //Getters || setters (with conversion for ease of use)
    fun getWidth(): Float {
        return Tools.dpToPx(width)
    }

    fun setWidth(widthInPx: Float) {
        width = Tools.pxToDp(widthInPx)
    }

    fun getHeight(): Float {
        return Tools.dpToPx(height)
    }

    fun setHeight(heightInPx: Float) {
        height = Tools.pxToDp(heightInPx)
    }

    /**
     * Fill the conversionMap with controlData dependent values.
     * The returned valueMap should NOT be kept in memory.
     *
     * @return the valueMap to use.
     */
    private fun fillConversionMap(): Map<String, String> {
        var valueMap: ArrayMap<String, String>? = conversionMap.get()
        if (valueMap == null) {
            buildConversionMap()
            valueMap = conversionMap.get()
        }

        valueMap!!.put("right", (CallbackBridge.physicalWidth - getWidth()).toString())
        valueMap.put("bottom", (CallbackBridge.physicalHeight - getHeight()).toString())
        valueMap.put("width", getWidth().toString())
        valueMap.put("height", getHeight().toString())
        valueMap.put("screen_width", CallbackBridge.physicalWidth.toString())
        valueMap.put("screen_height", CallbackBridge.physicalHeight.toString())
        valueMap.put("preferred_scale", AllSettings.buttonScale.getValue().toFloat().toString())

        return valueMap
    }

    companion object {
        const val SPECIALBTN_KEYBOARD = -1
        const val SPECIALBTN_TOGGLECTRL = -2
        const val SPECIALBTN_MOUSEPRI = -3
        const val SPECIALBTN_MOUSESEC = -4
        const val SPECIALBTN_VIRTUALMOUSE = -5
        const val SPECIALBTN_MOUSEMID = -6
        const val SPECIALBTN_SCROLLUP = -7
        const val SPECIALBTN_SCROLLDOWN = -8
        const val SPECIALBTN_MENU = -9

        private var SPECIAL_BUTTONS: Array<ControlData>? = null
        private var SPECIAL_BUTTON_NAME_ARRAY: MutableList<String>? = null
        private var builder: WeakReference<ExpressionBuilder> = WeakReference(null)
        private var conversionMap: WeakReference<ArrayMap<String, String>> = WeakReference(null)

        init {
            buildExpressionBuilder()
            buildConversionMap()
        }

        @JvmStatic
        fun getSpecialButtons(context: Context): Array<ControlData> {
            if (SPECIAL_BUTTONS == null) {
                SPECIAL_BUTTONS = arrayOf(
                    ControlData(context.getString(R.string.keycode_special_keyboard), intArrayOf(SPECIALBTN_KEYBOARD), "\${margin} * 3 + \${width} * 2", "\${margin}", false),
                    ControlData("GUI", intArrayOf(SPECIALBTN_TOGGLECTRL), "\${margin}", "\${bottom} - \${margin}"),
                    ControlData(context.getString(R.string.keycode_special_pri), intArrayOf(SPECIALBTN_MOUSEPRI), "\${margin}", "\${screen_height} - \${margin} * 3 - \${height} * 3"),
                    ControlData(context.getString(R.string.keycode_special_sec), intArrayOf(SPECIALBTN_MOUSESEC), "\${margin} * 3 + \${width} * 2", "\${screen_height} - \${margin} * 3 - \${height} * 3"),
                    ControlData(context.getString(R.string.keycode_special_mouse), intArrayOf(SPECIALBTN_VIRTUALMOUSE), "\${right}", "\${margin}", false),

                    ControlData(context.getString(R.string.keycode_special_mid), intArrayOf(SPECIALBTN_MOUSEMID), "\${margin}", "\${margin}"),
                    ControlData(context.getString(R.string.keycode_special_scrollup), intArrayOf(SPECIALBTN_SCROLLUP), "\${margin}", "\${margin}"),
                    ControlData(context.getString(R.string.keycode_special_scrolldown), intArrayOf(SPECIALBTN_SCROLLDOWN), "\${margin}", "\${margin}"),
                    ControlData(context.getString(R.string.keycode_special_menu), intArrayOf(SPECIALBTN_MENU), "\${margin}", "\${margin}")
                )
            }

            return SPECIAL_BUTTONS!!
        }

        @JvmStatic
        fun buildSpecialButtonArray(context: Context): List<String> {
            if (SPECIAL_BUTTON_NAME_ARRAY == null) {
                val nameList = ArrayList<String>()
                for (btn in getSpecialButtons(context)) {
                    nameList.add(StringUtils.insertSpace(context.getString(R.string.keycode_special), btn.name))
                }
                SPECIAL_BUTTON_NAME_ARRAY = nameList
                Collections.reverse(SPECIAL_BUTTON_NAME_ARRAY!!)
            }

            return SPECIAL_BUTTON_NAME_ARRAY!!
        }

        private fun calculate(math: String): Float {
            setExpression(math)
            return builder.get()!!.build().evaluate().toFloat()
        }

        private fun inflateKeycodeArray(keycodes: IntArray): IntArray {
            val inflatedArray = intArrayOf(
                LwjglGlfwKeycode.GLFW_KEY_UNKNOWN.toInt(),
                LwjglGlfwKeycode.GLFW_KEY_UNKNOWN.toInt(),
                LwjglGlfwKeycode.GLFW_KEY_UNKNOWN.toInt(),
                LwjglGlfwKeycode.GLFW_KEY_UNKNOWN.toInt()
            )
            System.arraycopy(keycodes, 0, inflatedArray, 0, keycodes.size)
            return inflatedArray
        }

        /**
         * Create a builder, keep a weak reference to it to use it with all views on first inflation
         */
        private fun buildExpressionBuilder() {
            val expressionBuilder = ExpressionBuilder("1 + 1")
                .function(object : Function("dp", 1) {
                    override fun apply(vararg args: Double): Double {
                        return Tools.pxToDp(args[0].toFloat()).toDouble()
                    }
                })
                .function(object : Function("px", 1) {
                    override fun apply(vararg args: Double): Double {
                        return Tools.dpToPx(args[0].toFloat()).toDouble()
                    }
                })
            builder = WeakReference(expressionBuilder)
        }

        /**
         * wrapper for the WeakReference to the expressionField.
         *
         * @param stringExpression the expression to set.
         */
        private fun setExpression(stringExpression: String) {
            if (builder.get() == null) buildExpressionBuilder()
            builder.get()!!.expression(stringExpression)
        }

        /**
         * Build a shared conversion map without the ControlData dependent values
         * You need to set the view dependent values before using it.
         */
        private fun buildConversionMap() {
            // Values in the map below may be always changed
            val keyValueMap = ArrayMap<String, String>(10)
            keyValueMap.put("top", "0")
            keyValueMap.put("left", "0")
            keyValueMap.put("right", "DUMMY_RIGHT")
            keyValueMap.put("bottom", "DUMMY_BOTTOM")
            keyValueMap.put("width", "DUMMY_WIDTH")
            keyValueMap.put("height", "DUMMY_HEIGHT")
            keyValueMap.put("screen_width", "DUMMY_DATA")
            keyValueMap.put("screen_height", "DUMMY_DATA")
            keyValueMap.put("margin", ControlInterface.getMarginDistance().toInt().toString())
            keyValueMap.put("preferred_scale", "DUMMY_DATA")

            conversionMap = WeakReference(keyValueMap)
        }
    }
}
