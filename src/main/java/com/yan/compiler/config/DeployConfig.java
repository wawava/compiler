package com.yan.compiler.config;

public class DeployConfig implements Cloneable {
	public String host;
	public String path;
	public String tmp;

	@Override
	public DeployConfig clone() {
		DeployConfig obj = new DeployConfig();
		obj.host = this.host;
		obj.path = this.path;
		obj.tmp = this.tmp;
		return obj;
	}
}
