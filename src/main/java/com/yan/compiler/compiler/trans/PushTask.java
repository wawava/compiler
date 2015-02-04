package com.yan.compiler.compiler.trans;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.yan.compiler.App;
import com.yan.compiler.Log;
import com.yan.compiler.compiler.ModuleInfo;
import com.yan.compiler.config.Config;

public class PushTask extends AbstractTransTask {

	public PushTask(CountDownLatch doneSignal, CountDownLatch startSignal,
			ModuleInfo moduleInfo) {
		super(doneSignal, startSignal, moduleInfo);
		Log.record(Log.INFO, getClass(),
				String.format("Create PushTask: [%s]", name));
	}

	private boolean mkRemoteDir() {
		Log.record(Log.INFO, getClass(), String.format(
				"Module: [%s], make remote dir: [%s]", name, cfg.tmp));
		// String[] cmd = {
		// Config.factory().get("shell"),
		// String.format("ssh %s 'mkdir -p -m 777 %s'", cfg.host,
		// cfg.tmp), "2>&1" };
		String[] cmd = { "ssh", cfg.host,
				String.format("mkdir -p -m 777 %s", cfg.tmp), "2>&1" };
		try {
			List<String> result = exec(cmd, null);
			if (result.isEmpty()) {
				return true;
			} else {
				Log.record(
						Log.ERR,
						getClass(),
						String.format("Make remote dir: [%s]",
								result.toString()));
				return false;
			}
		} catch (IOException e) {
			Log.record(Log.ERR, getClass(), e);
			return false;
		}
	}

	private boolean clearRemoteDir() {
		Log.record(Log.INFO, getClass(), String.format(
				"Module: [%s], clear remote dir: [%s]", name, cfg.tmp));
		String[] cmd = { "ssh", cfg.host,
				String.format("rm -f -r %s/*", cfg.tmp), "2>&1" };
		try {
			List<String> result = exec(cmd, null);
			if (result.isEmpty()) {
				return true;
			} else {
				Log.record(
						Log.ERR,
						getClass(),
						String.format("Make remote dir: [%s]",
								result.toString()));
				return false;
			}
		} catch (IOException e) {
			Log.record(Log.ERR, getClass(), e);
			return false;
		}
	}

	private List<String> scanCache() {
		List<String> list = App.scanDir(sourcePath.toString(), false);
		return list;
	}

	private String writeCfg(List<String> files) {
		try {
			File cfgFile = makeTmpFile();
			PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(
					cfgFile)));
			for (String file : files) {
				String str = String.format("%s %s %s", file, cfg.host, cfg.tmp);
				pw.println(str);
				Log.record(
						Log.INFO,
						getClass(),
						String.format("Write [%s] to file [%s]", str,
								cfgFile.toString()));
			}
			pw.flush();
			pw.close();
			return cfgFile.getAbsolutePath();
		} catch (Exception e) {
			Log.record(Log.ERR, getClass(), e);
			return null;
		}
	}

	private boolean push(String filePath) {
		String shell = Config.factory().get("uploadShell");
		String[] cmd = { shell, filePath, "2>&1" };

		try {
			List<String> output = exec(cmd, null);
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

	private Set<String> diff(String remote) {
		String[] remoteCmd = {
				"ssh",
				cfg.host,
				String.format("find %s -type f -print0 | xargs -0 md5sum",
						remote) };
		String[] localCmd = {
				Config.factory().get("shell"),
				"-c",
				String.format("find %s -type f -print0 | xargs -0 md5sum",
						sourcePath.toString()) };
		Set<String> diff = new HashSet<>();

		try {
			List<String> remoteOutput = exec(remoteCmd, null);
			List<String> localOutput = exec(localCmd, null);

			Map<String, String> remoteHash = new HashMap<>();
			Path remotePath = Paths.get(remote);
			for (String str : remoteOutput) {
				try {
					str = str.trim();
					if (str.isEmpty()) {
						continue;
					}
					String[] arr = str.split("\\s+");
					Path path = Paths.get(arr[1]);
					path = remotePath.relativize(path);
					remoteHash.put(path.toString(), arr[0]);
				} catch (Exception e) {
					StackTraceElement[] trace = e.getStackTrace();
					Log.record(Log.ERR, getClass(), trace[0].toString());
					Log.record(Log.ERR, getClass(), str);
					// System.err.println(remoteOutput.toString());
				}
			}

			for (String str : localOutput) {
				try {
					str = str.trim();
					if (str.isEmpty()) {
						continue;
					}
					String[] arr = str.split("\\s+");
					String hash = arr[0];
					Path path = Paths.get(arr[1]);
					path = sourcePath.relativize(path);
					String filePath = path.toString();
					if (!remoteHash.containsKey(filePath)
							|| !remoteHash.get(filePath).equals(hash)) {
						diff.add(filePath);
					}
				} catch (Exception e) {
					StackTraceElement[] trace = e.getStackTrace();
					Log.record(Log.ERR, getClass(), trace[0].toString());
					Log.record(Log.ERR, getClass(), str);
					// System.err.println(localOutput.toString());
				}
			}
		} catch (IOException e) {
			Log.record(Log.ERR, getClass(), e);
		}
		return diff;
	}

	private JsonArray makeLog(Set<String> diffBefore, Set<String> diffAfter,
			List<String> fileList) {
		JsonArray jArray = new JsonArray();
		for (String filePath : fileList) {
			Path path = Paths.get(filePath);
			path = sourcePath.relativize(path);
			JsonObject jObject = new JsonObject();
			filePath = path.toString();
			jObject.addProperty("filePath", filePath);
			Boolean diff = false;
			if (diffBefore.contains(filePath)) {
				diff = true;
			}
			jObject.addProperty("diff", diff);
			Boolean trans = true;
			if (diffAfter.contains(filePath)) {
				trans = false;
			}
			jObject.addProperty("trans", trans);
			jArray.add(jObject);
		}
		return jArray;
	}

	@Override
	public TaskResult call() {
		TaskResult result = new TaskResult();
		result.success = false;
		result.module = name;
		try {
			startSignal.await();
			// make remote directive
			if (false == mkRemoteDir()) {
				doneSignal.countDown();
				return result;
			}
			// clear remote directive
			if (false == clearRemoteDir()) {
				doneSignal.countDown();
				return result;
			}
			// scan the cache directive
			List<String> files = scanCache();
			// diff file
			List<String> fileList = App.scanDir(sourcePath.toString());
			Log.record(Log.INFO, getClass(), "Diff before transpot.");
			Set<String> diffBefore = diff(cfg.path);
			if (null == files || files.isEmpty()) {
				Log.record(Log.INFO, getClass(),
						String.format("Module: [%s] no files to push", name));
				doneSignal.countDown();
				result.obj = makeLog(diffBefore, new HashSet<String>(),
						fileList);
				result.success = true;
				return result;
			}
			// write config file
			String filePath;
			if (null == (filePath = writeCfg(files))) {
				doneSignal.countDown();
				return result;
			}

			// run shell
			if (false == push(filePath)) {
				doneSignal.countDown();
				return result;
			}

			// diff file again
			Set<String> diffAfter = diff(cfg.tmp);
			Log.record(Log.INFO, getClass(),
					String.format("Transport result: [%s]", diffAfter.size()));

			JsonArray jArray = makeLog(diffBefore, diffAfter, fileList);
			result.obj = jArray;
			result.success = true;
			doneSignal.countDown();
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			Log.record(Log.ERR, getClass(), e);
			return result;
		}
	}

}
