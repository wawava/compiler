package com.yan.compiler;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Log {
	public static final String ERR = "ERR";
	public static final String INFO = "INFO";
	public static final String DEBUG = "DEBUG";

	public static boolean isDebug = false;

	public static final void init(boolean isDebug) {
		Log.isDebug = isDebug;
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

	public static void record(String level, Class<? extends Object> clazz,
			Exception e) {
		record(level, clazz.getName(), e.getMessage());
	}

	public static void record(String level, String key, String msg) {
		if (isDebug || level.equals(ERR)) {
			String time = format.format(new Date());
			String str = String.format(logTpl, level, Thread.currentThread()
					.getId(), time, key, msg);
			System.out.println(str);
		}
	}
}
