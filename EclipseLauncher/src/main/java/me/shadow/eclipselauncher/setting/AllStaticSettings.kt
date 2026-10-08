package me.shadow.eclipselauncher.setting

/**
 * Values of static settings, used for settings that only take effect temporarily
 * The values here are not saved to the settings config and disappear when the software restarts!
 */
class AllStaticSettings {
    companion object {
        /**
         * Notch width (Int)
         */
        @JvmField var notchSize = 0

        /**
         * Scale factor (Float)
         */
        @JvmField var scaleFactor = AllSettings.resolutionRatio.getValue() / 100f

        /**
         * Disable double-tap to swap the held item (Boolean)
         */
        @JvmField var disableDoubleTap = AllSettings.disableDoubleTap.getValue()

        /**
         * Long-press trigger delay (Int)
         */
        @JvmField var timeLongPressTrigger = AllSettings.timeLongPressTrigger.getValue()

        /**
         * Enable gyroscope control (Boolean)
         */
        @JvmField var enableGyro = AllSettings.enableGyro.getValue()

        /**
         * Gyroscope control sensitivity (Int)
         */
        @JvmField var gyroSensitivity = AllSettings.gyroSensitivity.getValue()

        /**
         * Invert the gyroscope X axis (Boolean)
         */
        @JvmField var gyroInvertX = AllSettings.gyroInvertX.getValue()

        /**
         * Invert the gyroscope Y axis (Boolean)
         */
        @JvmField var gyroInvertY = AllSettings.gyroInvertY.getValue()

    }
}