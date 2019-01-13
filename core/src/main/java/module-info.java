/**
 * module-info
 *
 * @author mysh
 * @since 2018/4/9
 */
module mlib.core {
	exports mysh.algorithm;
	exports mysh.appcontainer;
	exports mysh.cache;
	exports mysh.codegen;
	exports mysh.collect;
	exports mysh.crawler2;
	exports mysh.db;
	exports mysh.jpipe;
	exports mysh.net;
	exports mysh.spring;
	exports mysh.sql;
	exports mysh.thrift;
	exports mysh.ui;
	exports mysh.util;
	exports mysh.net.httpclient;
	exports mysh.tulskiy.keymaster.common;

	requires spring.jdbc;
	requires spring.beans;
	requires spring.core;
	requires spring.context;
	requires spring.expression;
	requires spring.web;

	requires java.desktop;
	requires java.management;
	requires java.sql;
	requires com.sun.jna;
	requires com.sun.jna.platform;
	requires jdk.management;

	requires javafx.graphics;
	requires javafx.web;
	requires javafx.swing;

	requires commons.pool2;
	requires commons.lang3;
	requires commons.dbcp2;
	requires commons.codec;

	requires slf4j.api;
	requires libthrift;
	requires fastjson;
	requires aspectjrt;
	requires fst;
	requires javax.servlet.api;
	requires annotations;
	requires reflections;
	requires com.github.benmanes.caffeine;
	requires okhttp3;
	requires com.google.common;
}
