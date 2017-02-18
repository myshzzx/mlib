package mysh.spring.invoke;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 方法调用统计
 * 日志 耗时->INFO
 *
 * @author zhangzhixian<hzzhangzhixian@corp.netease.com>
 * @since 2017/2/10
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface InvokeStat {

	/**
	 * 记录项名称.
	 */
	String value() default "";

	/**
	 * 记录参数表. default false.
	 */
	boolean recParams() default false;

	/**
	 * 将信息写入日志. default true.
	 * INFO 级别耗时统计
	 */
	boolean writeLog() default true;
}
