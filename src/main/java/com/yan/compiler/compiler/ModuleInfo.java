package com.yan.compiler.compiler;

import java.nio.file.Path;

import com.yan.compiler.config.DeployConfig;

public class ModuleInfo {
	public String name;
	public Path sourcePath;
	public Path backupPath;
	public Path backupTmp;

	public DeployConfig cfg;
}
