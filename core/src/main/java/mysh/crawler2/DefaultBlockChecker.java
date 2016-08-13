package mysh.crawler2;

import com.google.common.collect.Sets;
import mysh.net.httpclient.HttpClientAssist;

import java.util.Set;

/**
 * DefaultBlockChecker
 *
 * @author mysh
 * @since 2016/1/9
 */
public class DefaultBlockChecker implements UrlClassifierConf.BlockChecker {
	private final Set<Integer> blockedStatusCodes = Sets.newHashSet(401, 403, 502, 503, 504, 509);

	@Override
	public boolean isBlocked(HttpClientAssist.UrlEntity ue) {
		int statusCode = ue.getStatusCode();

		return statusCode >= 400 && statusCode != 404
						|| (!ue.getReqUrl().contains("login") && ue.getCurrentURL().contains("login"))
						;
	}
}
