package com.yan.compiler.config;

public enum Env {
	SVN("svn"), BETA("beta"), PREVIEW("preview"), ONLINE("online");

	private String env;

	private Env(String env) {
		this.env = env;
	}

	@Override
	public String toString() {
		return env;
	}
}
