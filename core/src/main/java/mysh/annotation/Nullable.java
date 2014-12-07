package mysh.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Indicate that the value can be <code>null</code>
 *
 * @author Mysh
 * @since 2014/12/3 11:42
 */
@Target({ElementType.FIELD, ElementType.LOCAL_VARIABLE, ElementType.PARAMETER})
public @interface Nullable {
}
