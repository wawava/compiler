package com.yan.compiler.compiler.trans;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import com.yan.compiler.Log;
import com.yan.compiler.compiler.ModuleInfo;
import com.yan.compiler.compiler.TransTask;
import com.yan.compiler.config.Config;
import com.yan.compiler.config.DeployConfig;

public abstract class AbstractTransTask implements Callable<TaskResult> {
	protected CountDownLatch doneSignal;
	protected CountDownLatch startSignal;
	protected Path sourcePath;
	protected Path backupPath;
	protected Path backupTmp;
	protected DeployConfig cfg;
	protected String name;

	protected ProcessBuilder pb;

	public AbstractTransTask(CountDownLatch doneSignal,
			CountDownLatch startSignal, ModuleInfo moduleInfo) {
		this.doneSignal = doneSignal;
		this.startSignal = startSignal;
		this.sourcePath = moduleInfo.sourcePath;
		this.backupPath = moduleInfo.backupPath;
		this.backupTmp = moduleInfo.backupTmp;
		this.cfg = moduleInfo.cfg;
		this.name = moduleInfo.name;

		pb = new ProcessBuilder().redirectErrorStream(true);
		Map<String, String> env = pb.environment();
		env.putAll(Config.factory().getEnvm());
	}

	protected File makeTmpFile() throws IOException {
		File cfgFile = File.createTempFile(Config.factory().md5(name)
				.toString(), null);
		return cfgFile;
	}

	protected List<String> exec(String[] cmd, File dir) throws IOException {
		Log.record(Log.INFO, getClass(), "Run shell " + Arrays.toString(cmd));

		pb.command(cmd).directory(dir);
		return runShell();
	}

	protected List<String> exec(List<String> cmd, File dir) throws IOException {
		Log.record(Log.INFO, getClass(), "Run shell " + cmd.toString());

		pb.command(cmd).directory(dir);
		return runShell();
	}

	protected List<String> runShell() throws IOException {
		Process process = pb.start();
		BufferedReader br = new BufferedReader(new InputStreamReader(
				process.getInputStream()));
		LinkedList<String> output = new LinkedList<String>();

		String str;
		while (null != (str = br.readLine())) {
			output.addLast(str);
		}
		br.close();

		// br = new BufferedReader(new
		// InputStreamReader(process.getErrorStream()));
		// while (null != (str = br.readLine())) {
		// output.addLast(str);
		// }
		// br.close();
		// Log.record(Log.DEBUG, TransTask.class, output.toString());
		return output;
	}

	protected static List<String> diff(List<String> source,
			String sourcePrefix, List<String> target, String targetPrefix) {
		Set<String> targetSet = new HashSet<>();
		Integer tPrefix = targetPrefix.length();
		for (String str : target) {
			str = str.substring(tPrefix);
			targetSet.add(str);
		}
		List<String> result = new LinkedList<String>();
		Integer sPrefix = sourcePrefix.length();
		for (String str : source) {
			str = str.substring(sPrefix);
			if (!targetSet.contains(str)) {
				result.add(str);
			}
		}
		return result;
	}
}
