package me.shadow.eclipselauncher.json.reflect;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Captures a generic type that is only available through the type argument of a
 * subclass. Usage:
 * <pre>
 * Type listType = new TypeToken&lt;List&lt;SettingAttribute&gt;&gt;() { }.getType();
 * </pre>
 * Kotlin code can use the reified {@code typeTokenOf()} helper instead.
 */
public abstract class TypeToken<T> {

    private final Type type;

    @SuppressWarnings("unchecked")
    protected TypeToken() {
        Type superClass = getClass().getGenericSuperclass();
        if (!(superClass instanceof ParameterizedType)) {
            throw new IllegalStateException("TypeToken must be created with a type argument");
        }
        this.type = ((ParameterizedType) superClass).getActualTypeArguments()[0];
    }

    public final Type getType() {
        return type;
    }
}
