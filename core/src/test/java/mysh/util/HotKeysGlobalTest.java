package mysh.util;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.*;

/**
 * HotKeysGlobalTest
 *
 * @author mysh
 * @since 2019/1/13
 */
@Ignore
public class HotKeysGlobalTest {
	private static final Logger log = LoggerFactory.getLogger(HotKeysGlobalTest.class);

	@Test
	public void t1() throws InterruptedException {
		HotKeysGlobal.registerKeyListener("alt shift F", e -> log.info("key pressed"));
		new CountDownLatch(1).await();
	}

}
