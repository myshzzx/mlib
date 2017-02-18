package mysh.spring.invoke;

import com.google.common.base.Strings;
import mysh.util.Tick;
import org.apache.commons.lang3.tuple.MutableTriple;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * spring 调用链性能检视器.
 * <p>
 * <pre>
 * &lt;aop:config&gt;
 * 	&lt;aop:aspect ref=&quot;performanceInspector&quot; &gt;
 * 		&lt;aop:around method=&quot;inspect&quot; pointcut=&quot;execution(* *(..))&quot;/&gt;
 * 	&lt;/aop:aspect&gt;
 * &lt;/aop:config&gt;
 * </pre>
 *
 * @author zhangzhixian<hzzhangzhixian@corp.netease.com>
 * @since 2017/2/17
 */
@Component("performanceInspector")
public class PerformanceInspector {

	private static class InvokeInfo {
		List<MutableTriple<Integer, MethodSignature, Long>> infos = new ArrayList<>();
		int currentLevel;
	}

	private static final ThreadLocal<InvokeInfo> invokeInfoThreadLocal = new InheritableThreadLocal<>();

	public Object inspect(final ProceedingJoinPoint pjp) throws Throwable {
		MethodSignature signature = (MethodSignature) pjp.getSignature();

		InvokeInfo invokeInfo = invokeInfoThreadLocal.get();
		boolean isRoot = false;
		if (invokeInfo == null) {
			invokeInfo = new InvokeInfo();
			invokeInfoThreadLocal.set(invokeInfo);
			isRoot = true;
		}

		MutableTriple<Integer, MethodSignature, Long> currInvokeInfo =
				MutableTriple.of(invokeInfo.currentLevel++, signature, 0L);
		invokeInfo.infos.add(currInvokeInfo);

		try {
			Tick tick = Tick.tick();
			Object result = pjp.proceed();
			currInvokeInfo.setRight(tick.nip());
			return result;
		} finally {
			invokeInfo.currentLevel--;

			if (isRoot) {
				for (MutableTriple<Integer, MethodSignature, Long> info : invokeInfo.infos) {
					System.out.println(genInfoStr(info));
				}
				invokeInfoThreadLocal.set(null);
			}
		}
	}

	private String genInfoStr(MutableTriple<Integer, MethodSignature, Long> info) {
		return Strings.repeat("\t", info.getLeft())
				+ info.getMiddle().getDeclaringType().getSimpleName() + "." + info.getMiddle().getName()
				+ " :" + info.getRight() + "ms";
	}
}
