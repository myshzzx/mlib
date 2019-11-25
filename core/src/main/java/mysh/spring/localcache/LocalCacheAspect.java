package mysh.spring.localcache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Joiner;
import com.google.common.util.concurrent.UncheckedExecutionException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 本地缓存AOP
 *
 * @author zhangzhixian<hzzhangzhixian @ corp.netease.com>
 * @since 2016/7/22
 */
@Aspect
// @Component
public class LocalCacheAspect {
	private static final Logger log = LoggerFactory.getLogger(LocalCacheAspect.class);

	private static final Joiner keyJoiner = Joiner.on('\u0000');

	@Around(value = "@annotation(LocalCache)", argNames = "pjp")
	public Object getLocalCache(final ProceedingJoinPoint pjp) throws Throwable {
		MethodSignature signature = (MethodSignature) pjp.getSignature();
		Method method = pjp.getTarget().getClass().getMethod(signature.getName(), signature.getMethod().getParameterTypes());
		Object[] args = pjp.getArgs();
		LocalCache conf = method.getAnnotation(LocalCache.class);
		Cache<Object, AtomicReference<?>> cache = getCache(method, conf);

		if (conf.isMultiKey()) { // multiple keys
			//            MultiKeyInvokeWrap keyInfo = parseMultiKey(conf.keyExp(), pjp.getTarget(), signature.getMethod(), args);
			return null;
		} else { // single key
			Object key;
			if (StringUtils.isEmpty(conf.keyExp())) {
				// 自动生成 key
				key = genKey(args);
			} else
				key = parseSingleKey(conf.keyExp(), method, args);

			try {
				AtomicReference<?> resultWrapper = cache.get(key, k -> {
					try {
						return new AtomicReference<>(pjp.proceed());
					} catch (Throwable t) {
						if (t instanceof UncheckedExecutionException)
							throw (RuntimeException) t;
						else
							throw new RuntimeException(t);
					}
				});
				return resultWrapper.get();
			} catch (UncheckedExecutionException e) {
				throw e.getCause();
			}
		}
	}

	/**
	 * 自动生成key.<br/>
	 * 规则:<br/>
	 * 参数表为空, key = 0.
	 * 参数表长度为 1, key = 参数[0].
	 * 其他情况, key = join(args, \u0000).
	 *
	 * @param args 参数表
	 */
	private Object genKey(Object[] args) {
		Object key;
		if (args == null || args.length == 0)
			key = 0;
		else if (args.length == 1)
			key = args[0];
		else
			key = keyJoiner.join(args);
		return key;
	}

	/**
	 * spEL parser
	 */
	private final SpelExpressionParser spelParser = new SpelExpressionParser();
	/**
	 * spEL expression map
	 */
	private ConcurrentMap<String, Expression> exps = new ConcurrentHashMap<>();
	/**
	 * method param name discover
	 */
	private static final DefaultParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();

	/**
	 * 计算单一 key
	 */
	private Object parseSingleKey(String key, Method method, Object[] args) {
		// compile spEL expression
		Expression exp = exps.get(key);
		if (exp == null) {
			exp = spelParser.parseExpression(key);
			exps.put(key, exp);
		}

		// prepare spEL context
		EvaluationContext ctx = new StandardEvaluationContext();
		String[] paramNames = nameDiscoverer.getParameterNames(method);
		for (int i = 0; i < paramNames.length; i++) {
			ctx.setVariable(paramNames[i], args[i]);
		}

		return exp.getValue(ctx);
	}

	private final Map<Method, Cache<Object, AtomicReference<?>>> cacheMap = new ConcurrentHashMap<>();
	private static final Random rnd = new Random();

	/**
	 * 以目标方法为 key 取缓存块.
	 * 缓存块超时时间带上 20% 以内随机增量, 以避免相关的缓存数据同时失效.
	 */
	private Cache<Object, AtomicReference<?>> getCache(Method method, LocalCache conf) {
		Cache<Object, AtomicReference<?>> cache = cacheMap.get(method);
		if (cache == null) {
			synchronized (cacheMap) {
				cache = cacheMap.get(method);
				if (cache == null) {
					Assert.isTrue(conf.writeTimeoutSec() > 0, "本地缓存超时时间要大于0, " + method);
					Assert.isTrue(conf.maxSize() > 0, "本地缓存最大尺寸要大于0, " + method);
					int timeout = conf.writeTimeoutSec();
					int timeoutMilli = timeout > 50 * 60 ?
							timeout * 1000 + rnd.nextInt(600 * 1000)
							: (int) (timeout * 1000 * (1 + rnd.nextDouble() / 5));
					cache = Caffeine.newBuilder()
							.expireAfterWrite(timeoutMilli, TimeUnit.MILLISECONDS)
							.maximumSize(conf.maxSize())
							.build();
					cacheMap.put(method, cache);
					log.info("using-local-cache: timeout={}, maxSize={}, method={},",
							timeoutMilli, conf.maxSize(), method);
				}
			}
		}
		return cache;
	}
}
