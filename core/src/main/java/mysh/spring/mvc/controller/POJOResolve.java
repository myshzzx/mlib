package mysh.spring.mvc.controller;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自动将传入参数包装成一个 POJO。
 * 如将 dev.devName=name&dev.devId=id 包装成一个 dev 对象。
 * User: Allen
 * Time: 13-5-29 上午8:46
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface POJOResolve {
	/**
	 * 传入参数前缀。若无有效数据，则将参数名当前缀使用。
	 */
	String value() default "";

	/**
	 * 是否验证 POJO 属性有效性。默认验证。
	 */
	boolean validate() default true;
}
