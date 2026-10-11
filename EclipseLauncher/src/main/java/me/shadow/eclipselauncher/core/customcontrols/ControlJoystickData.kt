package me.shadow.eclipselauncher.core.customcontrols

class ControlJoystickData : ControlData {

    /* Whether the joystick can stay forward */
    @JvmField
    var forwardLock: Boolean = false
    /*
     * Whether the finger tracking is absolute (joystick jumps to where you touched)
     * or relative (joystick stays in the center)
     */
    @JvmField
    var absolute: Boolean = false

    constructor() : super()

    constructor(properties: ControlJoystickData) : super(properties) {
        forwardLock = properties.forwardLock
        absolute = properties.absolute
    }
}
