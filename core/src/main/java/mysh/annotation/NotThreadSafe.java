package mysh.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * 声明非线程安全的组件.
 *
 * @author ZhangZhx
 */
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
public @interface NotThreadSafe {
}
