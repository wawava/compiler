package com.yan.compiler.receiver;

public enum Action {
	PUSH("PUSH"), ROLLBACK("ROLLBACK");

	private String code;

	private Action(String code) {
		this.code = code;
	}

	@Override
	public String toString() {
		return code.toString();
	}

}
