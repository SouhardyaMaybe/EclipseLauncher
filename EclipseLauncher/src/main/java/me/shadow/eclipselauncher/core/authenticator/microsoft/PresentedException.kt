package me.shadow.eclipselauncher.core.authenticator.microsoft

import android.content.Context

import me.shadow.eclipselauncher.R
import me.shadow.eclipselauncher.utils.stringutils.StringUtils

/**
 * The primary constructor is private and its last parameter is intentionally not a property:
 * naming it after the public [Throwable] parameter of the second constructor would let that
 * constructor delegate to itself (a delegation cycle), so it is called `cause` instead.
 */
class PresentedException private constructor(
    val localizationStringId: Int,
    val suspectedNoMinecraftPurchase: Boolean,
    val extraArgs: Array<out Any>,
    cause: Throwable?
) : RuntimeException(cause) {

    constructor(localizationStringId: Int, suspectedNoMinecraftPurchase: Boolean, vararg extraArgs: Any) :
        this(
            localizationStringId = localizationStringId,
            suspectedNoMinecraftPurchase = suspectedNoMinecraftPurchase,
            extraArgs = extraArgs,
            cause = null
        )

    constructor(
        throwable: Throwable,
        localizationStringId: Int,
        suspectedNoMinecraftPurchase: Boolean,
        vararg extraArgs: Any
    ) : this(
        localizationStringId = localizationStringId,
        suspectedNoMinecraftPurchase = suspectedNoMinecraftPurchase,
        extraArgs = extraArgs,
        cause = throwable
    )

    fun toString(context: Context): String {
        var string = context.getString(localizationStringId, *extraArgs)
        if (suspectedNoMinecraftPurchase) {
            string = StringUtils.insertNewline(string, context.getString(R.string.no_minecraft_purchase))
        }
        return string
    }
}
