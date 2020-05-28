package mysh.spring.invoke;

import com.alibaba.fastjson.JSON;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.annotation.Order;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <pre>凯泓(zhixian.zzx@alibaba-inc.com)</pre>
 * @since 2020-05-22
 */
@Aspect
@Component
@Order(1)
public class MethodMonitorAspect {
	private static final Logger log = LoggerFactory.getLogger(MethodMonitor.class);
	
	private final SpelExpressionParser elParser = new SpelExpressionParser();
	private final DefaultParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();
	private final Map<String, Expression> expMap = new ConcurrentHashMap<>();
	private static Method toJSONMethod;
	
	static {
		try {
			toJSONMethod = JSON.class.getMethod("toJSONString", Object.class);
		} catch (NoSuchMethodException e) {
			log.error("MethodMonitorAspect-init-toJSONMethod-fail", e);
		}
	}
	
	@Around("@within(methodMonitor)")
	public Object doTypeAround(final ProceedingJoinPoint pjp, MethodMonitor methodMonitor) throws Throwable {
		MethodSignature signature = (MethodSignature) pjp.getSignature();
		Method method = pjp.getTarget().getClass().getMethod(signature.getName(), signature.getMethod().getParameterTypes());
		if (method.getAnnotation(MethodMonitor.class) != null) {
			return pjp.proceed();
		} else {
			return doMethodAround(pjp, methodMonitor);
		}
	}
	
	@Around("@annotation(methodMonitor)")
	public Object doMethodAround(final ProceedingJoinPoint pjp, MethodMonitor methodMonitor) throws Throwable {
		String invokeName = "";
		StandardEvaluationContext ctx = new StandardEvaluationContext();
		Logger logger = log;
		boolean isMethodReturnVoid = false;
		
		try {
			MethodSignature signature = (MethodSignature) pjp.getSignature();
			Method method = pjp.getTarget().getClass().getMethod(signature.getName(), signature.getMethod().getParameterTypes());
			isMethodReturnVoid = method.getReturnType() == Void.class;
			Object[] args = pjp.getArgs();
			invokeName = pjp.getTarget().getClass().getSimpleName() + "." + method.getName();
			
			// prepare spEL context
			String[] paramNames = nameDiscoverer.getParameterNames(method);
			if (paramNames != null) {
				for (int i = 0; i < paramNames.length; i++) {
					ctx.setVariable(paramNames[i], args[i]);
				}
			}
			
			// logger
			if (methodMonitor.logger().length() > 0) {
				logger = LoggerFactory.getLogger(methodMonitor.logger());
			}
		} catch (Throwable t) {
			log.error("MethodMonitorAspect-init-fail,{}", invokeName, t);
		}
		
		Throwable exp = null;
		Object r = null;
		long start = System.nanoTime();
		try {
			r = pjp.proceed();
		} catch (Throwable t) {
			exp = t;
		} finally {
			long end = System.nanoTime();
			ctx.setVariable("_return", r);
			// ctx.setVariable("_traceId", EagleEye.getTraceId());
			ctx.registerFunction("_toJSON", toJSONMethod);
			printLog(logger, ctx, invokeName, methodMonitor, (end - start) / 1000_000, exp,
					!isMethodReturnVoid && r == null);
		}
		if (exp != null) {
			throw exp;
		} else {
			return r;
		}
	}
	
	private void printLog(Logger logger, EvaluationContext ctx, String invokeName, MethodMonitor methodMonitor,
	                      long costMs, Throwable exp, boolean isReturnNull) {
		StringJoiner sj = new StringJoiner(methodMonitor.delimiter());
		
		sj.add("");
		// type
		if (exp != null) {
			sj.add("exception");
		} else if (isReturnNull) {
			sj.add("nullReturn");
		} else {
			sj.add("normal");
		}
		// name
		sj.add(invokeName);
		// cost
		sj.add(String.valueOf(costMs));
		// custom params
		for (String es : methodMonitor.exps()) {
			Object expValue = "N/A";
			try {
				Expression expression = expMap.computeIfAbsent(es, elParser::parseExpression);
				expValue = expression.getValue(ctx);
			} catch (Throwable t) {
				if (methodMonitor.elFailLog()) {
					log.error("parse-elExpression-error,{},exp={}", invokeName, es, t);
				}
			} finally {
				sj.add(String.valueOf(expValue));
			}
		}
		sj.add("");
		logger.info(sj.toString());
	}
	
}
