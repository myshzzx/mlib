package mysh.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * @author Mysh
 * @since 2014/9/26 18:29
 */
@Target(value = {ElementType.TYPE})
public @interface Mutable {
}
