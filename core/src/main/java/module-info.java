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

	requires httpcore;
	requires spring.jdbc;
	requires com.google.common;
	requires slf4j.api;
	requires google.http.client;
	requires httpclient;
	requires commons.pool2;
	requires libthrift;
	requires fastjson;
	requires commons.lang3;
	requires spring.beans;
	requires spring.core;
	requires spring.context;
	requires aspectjrt;
	requires java.desktop;
	requires java.management;
	requires fst;
	requires com.sun.jna;
	requires com.sun.jna.platform;
	requires jdk.management;
	requires spring.web;
	requires servlet.api;
	requires javafx.graphics;
	requires javafx.web;
	requires javafx.swing;
	requires java.sql;
	requires commons.dbcp2;
	requires annotations;
	requires reflections;
	requires spring.expression;
	requires commons.codec;

}
