package com.yan.compiler.receiver;

public enum ProjectType {
	PACKAGE(1), FILE(2);

	private Integer code;

	private ProjectType(Integer i) {
		code = i;
	}

	@Override
	public String toString() {
		return code.toString();
	}
}
