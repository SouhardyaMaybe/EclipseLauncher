package me.shadow.eclipselauncher.core.customcontrols

import android.content.Context

import androidx.annotation.Keep

import me.shadow.eclipselauncher.R
import me.shadow.eclipselauncher.context.ContextExecutor

import me.shadow.eclipselauncher.core.Tools

@Keep
class ControlDrawerData {

    @JvmField
    val buttonProperties: ArrayList<ControlData>
    @JvmField
    val properties: ControlData
    @JvmField
    var orientation: Orientation

    @Keep
    enum class Orientation {
        DOWN,
        LEFT,
        UP,
        RIGHT,
        FREE
    }

    constructor() : this(ArrayList())

    constructor(buttonProperties: ArrayList<ControlData>) : this(
        buttonProperties,
        ControlData(
            ContextExecutor.getString(R.string.controls_add_control_drawer),
            intArrayOf(),
            Tools.currentDisplayMetrics.widthPixels / 2f,
            Tools.currentDisplayMetrics.heightPixels / 2f
        )
    )

    constructor(buttonProperties: ArrayList<ControlData>, properties: ControlData) :
            this(buttonProperties, properties, Orientation.LEFT)

    constructor(buttonProperties: ArrayList<ControlData>, properties: ControlData, orientation: Orientation) {
        this.buttonProperties = buttonProperties
        this.properties = properties
        this.orientation = orientation
    }

    constructor(drawerData: ControlDrawerData) {
        buttonProperties = ArrayList(drawerData.buttonProperties.size)
        for (controlData in drawerData.buttonProperties) {
            buttonProperties.add(ControlData(controlData))
        }
        properties = ControlData(drawerData.properties)
        orientation = drawerData.orientation
    }

    companion object {
        @JvmStatic
        fun getOrientations(context: Context): Array<String> {
            return arrayOf(
                context.getString(R.string.controls_orientation_down),
                context.getString(R.string.controls_orientation_left),
                context.getString(R.string.controls_orientation_up),
                context.getString(R.string.controls_orientation_right),
                context.getString(R.string.controls_orientation_free)
            )
        }

        @JvmStatic
        fun orientationToInt(orientation: Orientation): Int {
            when (orientation) {
                Orientation.DOWN -> return 0
                Orientation.LEFT -> return 1
                Orientation.UP -> return 2
                Orientation.RIGHT -> return 3
                Orientation.FREE -> return 4
            }
            return -1
        }

        @JvmStatic
        fun intToOrientation(by: Int): Orientation? {
            when (by) {
                0 -> return Orientation.DOWN
                1 -> return Orientation.LEFT
                2 -> return Orientation.UP
                3 -> return Orientation.RIGHT
                4 -> return Orientation.FREE
            }
            return null
        }
    }
}
