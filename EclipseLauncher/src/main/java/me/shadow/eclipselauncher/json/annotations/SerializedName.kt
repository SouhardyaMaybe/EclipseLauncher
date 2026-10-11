package me.shadow.eclipselauncher.json.annotations

/** Alternate JSON name of an annotated field, honored on both read and write. */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class SerializedName(val value: String)
