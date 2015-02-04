package com.yan.compiler.config;

public enum Env {
	SVN(0), BETA(6), PREVIEW(8), ONLINE(10);

	private Integer env;

	private Env(Integer env) {
		this.env = env;
	}

	@Override
	public String toString() {
		return env.toString();
	}

	public static String getString(Env env) {
		switch (env) {
		case BETA:
			return "beta";
		case PREVIEW:
			return "preview";
		case ONLINE:
			return "online";
		default:
			return "";
		}
	}

	public Integer toInteger() {
		return env;
	}
}
