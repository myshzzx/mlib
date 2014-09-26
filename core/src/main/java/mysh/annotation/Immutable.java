package mysh.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * @author Mysh
 * @since 2014/9/26 18:28
 */
@Target({ElementType.TYPE})
public @interface Immutable {
}
