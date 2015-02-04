package com.yan.compiler;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.yan.compiler.compiler.ShutdownTask;
import com.yan.compiler.compiler.TransTask;
import com.yan.compiler.config.Config;
import com.yan.compiler.config.Env;
import com.yan.compiler.receiver.Action;
import com.yan.compiler.receiver.Receiver;

/**
 * Hello world!
 * 
 */
public class App {
	public static void main(String[] args) throws SQLException,
			NoSuchAlgorithmException, IOException {
		boolean isDebug = false;
		if (args.length > 0) {
			List<String> argArr = Arrays.asList(args);
			LinkedList<String> argList = new LinkedList<String>(argArr);
			while (argList.size() > 0) {
				String key = argList.removeFirst();
				switch (key) {
				case "-d":
				case "--debug":
				case "-D":
					isDebug = true;
					break;
				default:
					break;
				}
			}
		}
		Log.init(isDebug);
		Config.factory(isDebug);
		Runtime.getRuntime().addShutdownHook(new ShutdownTask());
		// Receiver r = Receiver.factory();
		// r.start();

		// Session session = ConnManagement.factory().connect();
		// session.query("Select * from svn_test_group order by id desc limit 3");
		// LinkedList<Map<String, Object>> result = session.getResult();
		// Iterator<Map<String, Object>> it = result.iterator();
		// while (it.hasNext()) {
		// Map<String, Object> map = it.next();
		// System.out.println(map);
		// }

		TransTask trans = new TransTask(990, Env.BETA, Action.PUSH, 6);
		trans.run();

		// String[] cmd = {
		// "ssh",
		// "10.202.241.19",
		// "find /tmp/4fac9ba115140ac4f1c22da82aa0bc7f/htdocs/www -type f -print | xargs md5sum"
		// };
		// //
		// "ssh 10.202.241.19 mkdir -p -m 777 /tmp/4fac9ba115140ac4f1c22da82aa0bc7f/htdocs/pmadmin 2>&1";
		// Runtime runtime = Runtime.getRuntime();
		// Process process = runtime.exec(cmd);
		// BufferedReader br = new BufferedReader(new InputStreamReader(
		// process.getInputStream()));
		// LinkedList<String> output = new LinkedList<String>();
		//
		// String str;
		// while (null != (str = br.readLine())) {
		// output.add(str);
		// }
		// br.close();
		// br = new BufferedReader(new
		// InputStreamReader(process.getErrorStream()));
		// while (null != (str = br.readLine())) {
		// output.addLast(str);
		// }
		// br.close();
		// System.out.println(output);
	}

	public static List<String> scanDir(String dir) {
		return scanDir(dir, true);
	}

	public static List<String> scanDir(String dir, boolean recursive) {
		File file = new File(dir);
		List<String> list = new LinkedList<String>();
		_scanDir(file, list, recursive);
		Log.record(Log.DEBUG, App.class,
				String.format("Scan dir: [%s], result: %s", dir, list.size()));
		return list;
	}

	private static void _scanDir(File root, List<String> output,
			boolean recursive) {
		if (root.isDirectory()) {
			File[] sub = root.listFiles();
			for (File file : sub) {
				if (file.isDirectory() && recursive) {
					_scanDir(file, output, true);
				} else {
					try {
						output.add(file.getCanonicalPath());
					} catch (IOException e) {
						Log.record(Log.ERR, App.class, e);
					}
				}
			}
		} else {
			try {
				output.add(root.getCanonicalPath());
			} catch (IOException e) {
				Log.record(Log.ERR, App.class, e);
			}
		}
	}
}
