package mysh.spring.invoke;

import com.alibaba.fastjson.JSON;
import mysh.util.Tick;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @author zhangzhixian<hzzhangzhixian@corp.netease.com>
 * @since 2017/2/10
 */
@Aspect
@Order(1)
@Component
public class InvokeStatAspect {
	private static final Logger log = LoggerFactory.getLogger(InvokeStatAspect.class);

	@Around(value = "@annotation(InvokeStat)", argNames = "pjp, InvokeStat")
	public Object recStat(final ProceedingJoinPoint pjp, InvokeStat invokeStat) throws Throwable {
		MethodSignature signature = (MethodSignature) pjp.getSignature();
		String recKey = StringUtils.isBlank(invokeStat.value()) ?
						pjp.getTarget().getClass().getSimpleName() + '.' + signature.getName()
						: invokeStat.value();
		Tick tick = Tick.tick();
		Object result = pjp.proceed();
		long time = tick.nip();
		String params = invokeStat.recParams() ? JSON.toJSONString(pjp.getArgs()) : null;
		if (invokeStat.writeLog())
			log.info("invoke-time-{}: {}ms, params:{}", recKey, time, params);
		return result;
	}
}
