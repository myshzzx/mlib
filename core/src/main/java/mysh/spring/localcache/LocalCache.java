package mysh.spring.localcache;

import com.google.common.cache.CacheBuilder;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明本地缓存.
 *
 * @author zhangzhixian<hzzhangzhixian@corp.netease.com>
 * @since 2016/7/21
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface LocalCache {

    /**
     * 写入超时 (秒).
     * 实际设置的时间会带20%(最多10分钟)以内随机增量, 以防止其他相关的缓存块同时失效.
     * 例如设置超时时间为10分钟, 实际缓存块的超时会被设置为 10~12 分钟内的一个随机值.
     */
    int writeTimeoutSec();

    /**
     * 缓存块最大尺寸.
     */
    int maxSize();

    /**
     * 生成 key 的 spEL 表达式 (单 key / 批量 key).
     * <pre>
     * 参数列表有以下情形的, 可省略此表达式.
     * 注意以下规则需要用户自己保证. 担心误用的话还是乖乖写表达式吧.
     *
     * A. 单一 key
     *   1. 参数只含基本类型(含封装类和String, 不含数组)
     *   2. 参数是一个自定义类型, 可以用来当 key 的(带正确的 hashCode 和 equals 实现)
     *   3. 含 基本类型(含封装类和String,不含数组) 和 自定义类型(toString 结果可以用来当 key 的)
     *   4. 无参 (意味着只能存一个缓存项)
     *
     * B. 批量 key
     *   规则与上述规则相同, 只是这里要考查的参数类型为 List 容器内的类型与 非 List 参数类型.
     *   如参数表 (List&lt;String&gt;, int) 按照 (String, int) 考虑.
     *
     * </pre>
     */
    String keyExp() default "";

    /**
     * 是否批量 key. 默认 false.
     */
    boolean isMultiKey() default false;

    /**
     * 并发级别. 默认 4.
     * 用于控制并发更新操作的线程数, 类似 concurrentHashMap(1.6版本) segment 数量.
     *
     * @see CacheBuilder#concurrencyLevel(int)
     */
    int concurrencyLevel() default 4;
}
