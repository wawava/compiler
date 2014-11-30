package com.yan.compiler;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.yan.compiler.config.Config;

public class Log {
	public static final String ERR = "ERR";
	public static final String INFO = "INFO";
	public static final String DEBUG = "DEBUG";

	public static String showLog;

	public static final void init() {
		Config config = Config.factory();
		showLog = config.get("showLog");
		if (showLog.length() <= 0) {
			showLog = null;
		}
	}

	private static final String logTpl = "%s:[%s][%s]-[%s]: %s";

	private static final SimpleDateFormat format = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss");

	public static void record(String level, String key, Exception e) {
		String msg = e.getMessage();
		record(level, key, msg);
	}

	public static void record(String level, Class<? extends Object> clazz,
			String msg) {
		record(level, clazz.getName(), msg);
	}

	public static void record(String level, String key, String msg) {
		if (null == showLog || showLog.equals(level)) {
			String time = format.format(new Date());
			String str = String.format(logTpl, level, Thread.currentThread()
					.getId(), time, key, msg);
			System.out.println(str);
		}
	}
}
