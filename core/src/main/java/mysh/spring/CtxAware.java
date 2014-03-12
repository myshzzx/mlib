package mysh.spring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.concurrent.CountDownLatch;

/**
 * Spring 上下文。
 *
 * @author 张智贤
 */
public class CtxAware implements ApplicationContextAware {
	private static final Logger log = LoggerFactory.getLogger(CtxAware.class);
	private static final CountDownLatch latch = new CountDownLatch(1);
	private volatile static ApplicationContext ctx;

	public static ApplicationContext getCtx() {
		try {
			latch.await();
			if (ctx == null) {
				String msg = "没有上下文对象";
				log.error(msg, new RuntimeException(msg));
			}

			return ctx;
		} catch (InterruptedException e) {
			log.error("取上下文对象失败", e);
			return null;
		}
	}

	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		if (ctx != null) {
			String msg = "上下文对象重置.";
			log.error(msg, new RuntimeException(msg));
		}
		ctx = applicationContext;
		latch.countDown();
		log.info("上下文对象设置成功.");
	}

}
