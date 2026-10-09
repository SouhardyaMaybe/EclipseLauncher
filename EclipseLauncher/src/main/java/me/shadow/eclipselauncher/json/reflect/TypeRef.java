package me.shadow.eclipselauncher.json.reflect;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Captures a generic type that is only available through the type argument of a
 * subclass. Usage:
 * <pre>
 * Type listType = new TypeRef&lt;List&lt;SettingAttribute&gt;&gt;() { }.getType();
 * </pre>
 * Kotlin code can use the reified {@code typeRefOf()} helper instead.
 */
public abstract class TypeRef<T> {

    private final Type type;

    @SuppressWarnings("unchecked")
    protected TypeRef() {
        Type superClass = getClass().getGenericSuperclass();
        if (!(superClass instanceof ParameterizedType)) {
            throw new IllegalStateException("TypeRef must be created with a type argument");
        }
        this.type = ((ParameterizedType) superClass).getActualTypeArguments()[0];
    }

    public final Type getType() {
        return type;
    }
}
