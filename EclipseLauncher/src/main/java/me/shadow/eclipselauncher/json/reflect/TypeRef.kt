package me.shadow.eclipselauncher.json.reflect

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * Captures a generic type that is only available through the type argument of a
 * subclass. Usage:
 * <pre>
 * Type listType = new TypeRef&lt;List&lt;SettingAttribute&gt;&gt;() { }.getType();
 * </pre>
 * Kotlin code can use the reified {@code typeRefOf()} helper instead.
 */
abstract class TypeRef<T> {

    val type: Type

    protected constructor() {
        val superClass = javaClass.genericSuperclass
        if (superClass !is ParameterizedType) {
            throw IllegalStateException("TypeRef must be created with a type argument")
        }
        type = superClass.actualTypeArguments[0]
    }
}
