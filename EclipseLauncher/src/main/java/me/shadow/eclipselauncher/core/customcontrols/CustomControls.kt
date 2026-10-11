package me.shadow.eclipselauncher.core.customcontrols

import androidx.annotation.Keep

import me.shadow.eclipselauncher.ui.subassembly.customcontrols.ControlInfoData

import me.shadow.eclipselauncher.core.Tools

import java.io.IOException
import java.util.ArrayList

@Keep
class CustomControls {
    @JvmField
    var version: Int = 7
    @JvmField
    var scaledAt: Float = 0f
    @JvmField
    var mControlDataList: MutableList<ControlData>
    @JvmField
    var mDrawerDataList: MutableList<ControlDrawerData>
    @JvmField
    var mJoystickDataList: MutableList<ControlJoystickData>
    @JvmField
    var mControlInfoDataList: ControlInfoData

    constructor() : this(ArrayList(), ArrayList(), ArrayList(), ControlInfoData())

    constructor(
        mControlDataList: MutableList<ControlData>,
        mDrawerDataList: MutableList<ControlDrawerData>,
        mJoystickDataList: MutableList<ControlJoystickData>,
        mControlInfoDataList: ControlInfoData
    ) {
        this.mControlDataList = mControlDataList
        this.mDrawerDataList = mDrawerDataList
        this.mJoystickDataList = mJoystickDataList
        this.mControlInfoDataList = mControlInfoDataList
        this.scaledAt = 100f
    }

    @Throws(IOException::class)
    fun save(path: String?) {
        //Current version is the V3.2 so the version as to be marked as 8 !
        version = 8
        Tools.write(path!!, Tools.JSON_CODEC.toJson(this))
    }
}
