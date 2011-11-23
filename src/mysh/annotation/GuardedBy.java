package mysh.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * 声明线程安全性受某种策略守护.
 * 
 * @author ZhangZhx
 * 
 */
@Target({ ElementType.METHOD, ElementType.FIELD, ElementType.LOCAL_VARIABLE })
public @interface GuardedBy {
	public String value() default "";
}
