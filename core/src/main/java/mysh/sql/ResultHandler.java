package mysh.sql;

import java.util.Map;

/**
 * ResultHandler
 *
 * @author mysh
 * @since 2015/9/7
 */
public interface ResultHandler {
	void handle(Map<String, Object> item);
}
