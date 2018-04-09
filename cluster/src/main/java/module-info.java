/**
 * module-info
 *
 * @author mysh
 * @since 2018/4/9
 */
module mlib.cluster {
	requires mlib.core;
	requires slf4j.api;
	requires annotations;
	requires java.management;
	requires com.google.common;
	requires libthrift;
	requires java.rmi;
	requires jdk.management;
}
