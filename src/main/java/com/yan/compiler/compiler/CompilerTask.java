package com.yan.compiler.compiler;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.yan.compiler.Log;
import com.yan.compiler.config.Config;
import com.yan.compiler.receiver.BasePackage;

public class CompilerTask {

	private String project;
	private String env;
	private String name;
	private int id;

	private String compileDir;
	private String sourceDir;

	private String log;
	private String[] cmd;

	public CompilerTask(BasePackage bp) {
		Config config = Config.factory();
		project = bp.getProject();
		env = bp.getEnv();
		name = bp.getName();
		id = bp.getId();

		compileDir = config.getCompileDir(project, env);
		sourceDir = config.getCacheDir(name);

		String cmd = config.get("compileCmd");
		this.cmd = cmd.split("\\|\\|");
	}

	private List<String> scanDir(String dir) {
		File file = new File(dir);
		List<String> list = new LinkedList<String>();
		_scanDir(file, list);
		return list;
	}

	private void _scanDir(File root, List<String> output) {
		if (root.isDirectory()) {
			File[] sub = root.listFiles();
			for (File file : sub) {
				if (file.isDirectory()) {
					_scanDir(file, output);
				} else {
					try {
						output.add(file.getCanonicalPath());
					} catch (IOException e) {
						Log.record(Log.ERR, CompilerTask.class.getSimpleName(),
								e);
					}
				}
			}
		} else {
			try {
				output.add(root.getCanonicalPath());
			} catch (IOException e) {
				Log.record(Log.ERR, CompilerTask.class.getSimpleName(), e);
			}
		}
	}

	public void deployFile() throws IOException {
		Path sourceRoot = Paths.get(sourceDir);
		Path targetRoot = Paths.get(compileDir);

		List<String> files = scanDir(sourceDir);
		Iterator<String> it = files.iterator();
		while (it.hasNext()) {
			String path = it.next();
			Path source = Paths.get(path);
			Path rel = sourceRoot.relativize(source);
			Path target = targetRoot.resolve(rel);
			Path targetParent = target.getParent();
			File parent = targetParent.toFile();
			if (!parent.exists()) {
				parent.mkdirs();
			}
			Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	public boolean compile() throws IOException {
		Map<String, String> env = System.getenv();
		String jdkHome = env.get("JDK_HOME");
		List<String> envList = new LinkedList<String>();
		for (Entry<String, String> entry : env.entrySet()) {
			String key = entry.getKey();
			String val;
			if (key.equals("JAVA_HOME")) {
				val = jdkHome;
			} else {
				val = entry.getValue();
			}
			String _env = String.format("%s=%s", key, val);
			envList.add(_env);
		}
		String[] envp = envList.toArray(new String[0]);

		Runtime runtime = Runtime.getRuntime();
		Process process = runtime.exec(cmd, envp, new File(compileDir));
		BufferedReader br = new BufferedReader(new InputStreamReader(
				process.getInputStream()));
		LinkedList<String> output = new LinkedList<String>();
		String str;
		while (null != (str = br.readLine())) {
			output.addLast(str);
		}
		br.close();

		LinkedList<String> error = new LinkedList<String>();
		boolean hasErr = false;
		Pattern p = Pattern.compile("^\\[ERROR]");
		for (String _str : output) {
			Matcher m = p.matcher(_str);
			if (m.find()) {
				hasErr = true;
				_str = m.replaceFirst("").trim();
				if (_str.length() > 0) {
					error.addLast(_str);
				}
			}
			Log.record(Log.INFO, CompilerTask.class.getSimpleName(), _str);
		}

		StringBuilder outputBuilder = new StringBuilder();
		if (hasErr) {
			output = error;
		}
		for (String _str : output) {
			outputBuilder.append(_str);
			outputBuilder.append("\n");
		}
		log = outputBuilder.toString();
		return !hasErr;
	}

	/**
	 * @return the log
	 */
	public String getLog() {
		return log;
	}

	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}
}
