package com.yan.compiler.compiler.trans;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import com.yan.compiler.App;
import com.yan.compiler.Log;
import com.yan.compiler.compiler.ModuleInfo;
import com.yan.compiler.config.Config;
import com.yan.compiler.config.DeployConfig;

public class BackupTask extends AbstractTransTask {

	public BackupTask(CountDownLatch doneSignal, CountDownLatch startSignal,
			ModuleInfo moduleInfo) {
		super(doneSignal, startSignal, moduleInfo);
		Log.record(Log.INFO, getClass(),
				String.format("Create BackupTask: [%s]", name));
	}

	private List<String> scanFiles(Path path) {
		List<String> files = App.scanDir(path.toAbsolutePath().toString());
		return files;
	}

	/**
	 * Scan the {@link #sourcePath} and {@link #backupPath}，get the file list
	 * contained in the sourcePath but not in the backupPath.
	 * 
	 * @return A file list.
	 */
	public List<String> parseFiles() {
		List<String> sourceFiles = scanFiles(sourcePath);
		List<String> backupFiles = scanFiles(backupPath);

		List<String> finalFiles = diff(sourceFiles, sourcePath.toString(),
				backupFiles, backupPath.toString());
		return finalFiles;
	}

	/**
	 * Write files list to a temp file. <br/>
	 * Note. The path prefix provided by {@link DeployConfig#path}
	 * 
	 * @param finalFiles
	 *            A file list.
	 * @return
	 */
	public String writeCfg(List<String> finalFiles) {
		try {
			File cfgFile = makeTmpFile();
			PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(
					cfgFile)));
			Path targetPath = Paths.get(cfg.path);
			for (String filePath : finalFiles) {
				if (filePath.charAt(0) == File.separatorChar) {
					filePath = filePath.substring(1);
				}
				Path target = targetPath.resolve(filePath);
				pw.println(target.toString());
				Log.record(
						Log.INFO,
						getClass(),
						String.format("Write [%s] to file [%s]",
								target.toString(), cfgFile.toString()));
			}
			pw.flush();
			pw.close();
			return cfgFile.getAbsolutePath();
		} catch (Exception e) {
			Log.record(Log.ERR, getClass(), e);
			return null;
		}
	}

	/**
	 * Run shell and backup files.
	 * 
	 * @param filePath
	 * @return
	 */
	public boolean backup(String filePath) {
		String shell = Config.factory().get("backupShell");
		String host = cfg.host;
		String[] cmd = { shell, filePath, host, backupTmp.toString(), "2>&1" };
		try {
			List<String> output = exec(cmd, null);
			if (output.isEmpty()) {
				return false;
			}
			StringBuilder sbd = new StringBuilder();
			for (String out : output) {
				sbd.append(out.trim());
			}
			String out = sbd.toString();
			Log.record(Log.INFO, getClass(), String.format(
					"Module:[%s] run shell result: [%s]", name, out));
			if (out.equals("1")) {
				return true;
			} else {
				return false;
			}
		} catch (IOException e) {
			Log.record(Log.ERR, getClass(), e);
			return false;
		}
	}

	/**
	 * Transport file from tmp dir to backup dir
	 */
	public boolean trans() {
		List<String> fileList = App.scanDir(backupTmp.toString());
		String remotePath = cfg.path;
		for (String path : fileList) {
			Path tmpPath = Paths.get(path);
			Integer i = path.indexOf(remotePath);
			path = path.substring(i + remotePath.length());
			if (path.charAt(0) == File.separatorChar) {
				path = path.substring(1);
			}
			Path targetPath = backupPath.resolve(path);
			try {
				Files.copy(tmpPath, targetPath,
						StandardCopyOption.REPLACE_EXISTING);
				Log.record(Log.INFO, getClass(), String.format(
						"Copy [%s] to [%s]", tmpPath.toString(),
						targetPath.toString()));
			} catch (IOException e) {
				Log.record(Log.ERR, getClass(), e);
			}
		}
		return true;
	}

	@Override
	public TaskResult call() {
		TaskResult result = new TaskResult();
		result.success = false;
		result.module = name;
		try {
			startSignal.await();
			// make module path.
			List<String> finalFiles = parseFiles();
			Log.record(Log.INFO, getClass(), String.format(
					"Module: [%s] - [%s] files need to backup", name,
					finalFiles.size()));
			if (finalFiles.size() <= 0) {
				Log.record(Log.INFO, getClass(), String.format(
						"Module: [%s] - No files to backup", name));
				doneSignal.countDown();
				return result;
			}
			String cfgFile = writeCfg(finalFiles);
			if (false == backup(cfgFile)) {
				doneSignal.countDown();
				return result;
			}

			if (false == trans()) {
				doneSignal.countDown();
				return result;
			}
			JsonArray jArray = new JsonArray();
			for (String file : finalFiles) {
				jArray.add(new JsonPrimitive(file));
			}
			result.obj = jArray;
			result.success = true;
			doneSignal.countDown();
			return result;
		} catch (Exception e) {
			Log.record(Log.ERR, getClass(), e);
			return result;
		}

	}
}
