package com.yan.compiler.receiver;

import java.lang.reflect.Field;

import com.yan.compiler.Log;

public class BasePackage {

	private int id;

	private String project;

	private String env;

	private String name;

	private int uid;

	private int reversion;

	/**
	 * @return the project
	 */
	public String getProject() {
		return project;
	}

	/**
	 * @param project
	 *            the project to set
	 */
	public void setProject(String project) {
		this.project = project;
	}

	/**
	 * @return the env
	 */
	public String getEnv() {
		return env;
	}

	/**
	 * @param env
	 *            the env to set
	 */
	public void setEnv(String env) {
		this.env = env;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name
	 *            the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	public String toString() {
		StringBuilder sbd = new StringBuilder(1024);
		Class<? extends BasePackage> clazz = getClass();
		Field[] fields = clazz.getDeclaredFields();
		for (Field field : fields) {
			sbd.append(field.getName());
			sbd.append(": ");
			try {
				sbd.append(field.get(this));
			} catch (Exception e) {
				Log.record(Log.ERR, BasePackage.class.getName(), e);
			}
			sbd.append("; ");
		}
		return sbd.toString();
	}

	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}

	/**
	 * @param id
	 *            the id to set
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * @return the uid
	 */
	public int getUid() {
		return uid;
	}

	/**
	 * @param uid
	 *            the uid to set
	 */
	public void setUid(int uid) {
		this.uid = uid;
	}

	/**
	 * @return the reversion
	 */
	public int getReversion() {
		return reversion;
	}

	/**
	 * @param reversion
	 *            the reversion to set
	 */
	public void setReversion(int reversion) {
		this.reversion = reversion;
	}
}
