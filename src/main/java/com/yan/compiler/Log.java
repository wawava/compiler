package com.yan.compiler;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Log {
	public static final String ERR = "ERR";
	public static final String INFO = "INFO";

	private static final String logTpl = "%s:[%s]-[%s]: %s";

	private static final SimpleDateFormat format = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss");

	public static void record(String level, String key, Exception e) {
		String msg = e.getMessage();
		record(level, key, msg);
	}

	public static void record(String level, String key, String msg) {
		String time = format.format(new Date());
		String str = String.format(logTpl, level, time, key, msg);
		System.out.println(str);
	}
}
