package com.yan.compiler.receiver;

import com.google.gson.Gson;

public class PackageFactory {

	private static Gson gson = new Gson();

	private ProjectType type;

	private String data;

	public static AbstractBasePackage factory(String msg) {
		PackageFactory bp = gson.fromJson(msg, PackageFactory.class);
		AbstractBasePackage _package = null;
		switch (bp.type) {
		case PACKAGE:
			_package = gson.fromJson(bp.data, FileBasePackage.class);
			break;
		case FILE:
		default:
			_package = gson.fromJson(bp.data, FileBasePackage.class);
			break;
		}
		return _package;
	}

	/**
	 * @return the type
	 */
	public ProjectType getType() {
		return type;
	}

	/**
	 * @param type
	 *            the type to set
	 */
	public void setType(ProjectType type) {
		this.type = type;
	}

	/**
	 * @return the data
	 */
	public String getData() {
		return data;
	}

	/**
	 * @param data
	 *            the data to set
	 */
	public void setData(String data) {
		this.data = data;
	}

}
