package mysh.spring.invoke;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 方法调用统计日志.<br/>
 * <p>
 * 可用于 方法体/实现类/实现类的父类(接口无效), 优先级递减<br/>
 * 默认输出格式为 |调用类型|调用目标|耗时(ms)|自定义el表达式...|<br/>
 * 调用类型包括 normal(正常), nullReturn(返回null), exception(异常抛出)<br/>
 * 可自定义的el表达式见 {@link #exps()}
 *
 * @author <pre>凯泓(zhixian.zzx@alibaba-inc.com)</pre>
 * @since 2020-05-22
 */
@Target(value = {ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface MethodMonitor {
	
	/**
	 * 可指定日志输出目标logger名.
	 */
	String logger() default "MethodMonitorLogger";
	
	/**
	 * spEl表达式. 形如 #param1.getProp1() <br/>
	 * <a href='http://itmyhome.com/spring/expressions.html'>翻译文档</a>
	 * <a href='https://docs.spring.io/spring/docs/4.3.10.RELEASE/spring-framework-reference/html/expressions.html'>官方文档</a>
	 * <pre>
	 * 内置变量如下
	 * _return: 方法返回值, 例如 #_return.isSuccess()
	 * _traceId: 鹰眼id
	 * _this: spring 代理的 AOP 实例
	 * _target: 被代理的原始 bean 对象实例
	 *
	 * 内置函数如下
	 * _toJSON(obj): 将对象转为json串返回
	 * </pre>
	 */
	String[] exps() default {};
	
	/**
	 * 日志分隔符
	 */
	String delimiter() default "|";
	
	/**
	 * 表达式计算失败是否打印日志. 日志打到 MethodMonitor 此类全名log中.
	 */
	boolean elFailLog() default true;
}
