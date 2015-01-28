package com.yan.compiler.compiler;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.yan.compiler.App;
import com.yan.compiler.Log;
import com.yan.compiler.config.Config;
import com.yan.compiler.config.DeployConfig;
import com.yan.compiler.config.Env;
import com.yan.compiler.db.ConnManagement;
import com.yan.compiler.db.Session;
import com.yan.compiler.receiver.Action;

public class TransTask implements Task {

	// 流程状态
	public static final Integer STATUS_NO_EXECUTE = 0;
	public static final Integer STATUS_ALREADY_EXECUTE = 1;
	public static final Integer STATUS_ALREADY_ROLLBACK = 2;
	public static final Integer STATUS_READY_TO_EXECUTE = 6;
	public static final Integer STATUS_RECEIVED = 7;
	public static final Integer STATUS_BACKUP = 8;
	public static final Integer STATUS_TRANS = 9;
	public static final Integer STATUS_SWITCH = 10;

	private Integer groupId;
	private Env env;
	private Action action;

	private String name;

	private Session session;
	private Path sourcePath;
	private Path backupPath;
	private Path backupTmp;

	private List<String> moduleInfo;
	private Map<String, Object> push;
	// private Map<String, List<String>> fileList;

	private CountDownLatch doneSignal;
	private CountDownLatch startSignal;

	public TransTask(Integer groupId, Env env, Action action) {
		setGroupId(groupId);
		setEnv(env);
		setAction(action);
		setName();
	}

	@Override
	public void run() {

		if (false == init()) {
			return;
		}

		// backup if action is push
		if (false == backup()) {
			// TODO log
			return;
		}

		// transport files
		if (false == pushFile()) {
			// TODO log
			return;
		}

		// switch
		if (false == switchFile()) {
			// TODO log
			return;
		}

		logout();
	}

	private boolean mkDbConn() {
		try {
			session = ConnManagement.factory().connect();
		} catch (SQLException e) {
			Log.record(Log.ERR, getClass(), e);
			return false;
		}
		return true;
	}

	private boolean verify() {
		Map<String, Object> push = getPushRecord();
		Integer status = Integer.valueOf(String.valueOf(push.get("status")));
		if (!STATUS_READY_TO_EXECUTE.equals(status)
				&& !Config.factory().isDebug()) {
			Log.record(Log.INFO, getClass(), String.format(
					"Push:[%s] - Status:[%s] is not READY_TO_EXECUTE", groupId,
					status));
			return false;
		}
		this.push = push;

		if (null == (moduleInfo = getModule())) {
			return false;
		}

		return true;
	}

	private boolean mkDirs() {
		String sourceDir;
		if (action == Action.ROLLBACK) {
			sourceDir = Config.factory().getBackupDir(groupId, getEnv());
		} else {
			sourceDir = Config.factory().getCacheDir(groupId);
		}
		if (false == _verifyDir(sourceDir)) {
			return false;
		}
		sourcePath = Paths.get(sourceDir);

		String backupDir = Config.factory().getBackupDir(groupId, getEnv());
		if (false == _verifyDir(backupDir)) {
			return false;
		}
		backupPath = Paths.get(backupDir);

		if (null == (backupTmp = Config.factory().getBackupTplPath(groupId))) {
			return false;
		}

		return true;
	}

	private boolean _verifyDir(String dir) {
		File source = new File(dir);
		if ((!source.exists() || !source.isDirectory()) && !source.mkdirs()) {
			Log.record(Log.ERR, getClass(),
					String.format("Can not create dir[%s]", dir));
			return false;
		}
		return true;
	}

	private boolean init() {
		startSignal = new CountDownLatch(1);
		// get db connection.
		if (false == mkDbConn()) {
			return false;
		}

		// verify the test status.
		if (false == verify()) {
			return false;
		}
		doneSignal = new CountDownLatch(moduleInfo.size());

		// make dirs.
		if (false == mkDirs()) {
			return false;
		}

		if (false == updateStatus(STATUS_RECEIVED)) {
			return false;
		}
		return true;
	}

	/**
	 * 
	 * @return
	 */
	private boolean backup() {
		if (isRollback()) {
			return true;
		}

		List<ModuleInfo> moduleList;
		if (1 == moduleInfo.size()) {
			moduleList = mkSingleModule();
		} else {
			moduleList = mkMultiModule();
		}

		LinkedList<Future<Boolean>> futureList = new LinkedList<>();
		for (ModuleInfo module : moduleList) {
			BackupTask task = new BackupTask(doneSignal, startSignal, module);
			Future<Boolean> future = TaskManagement.factory().submit(task);
			futureList.addLast(future);
		}
		startSignal.countDown();
		try {
			doneSignal.await();
		} catch (InterruptedException e) {
			Log.record(Log.ERR, getClass(), e);
			return false;
		}

		boolean success = true;
		for (Future<Boolean> future : futureList) {
			try {
				success = success && future.get();
			} catch (Exception e) {
				Log.record(Log.ERR, getClass(), e);
				success = false;
			}
		}

		return success;
	}

	private List<ModuleInfo> mkSingleModule() {
		String module = moduleInfo.get(0);
		// String ProjectName = Config.factory().getProjectConfig(module);
		Path backupTmp = Config.factory().getBackupTplPath(getGroupId());

		List<ModuleInfo> moduleList = new LinkedList<>();
		DeployConfig cfg = Config.factory().getDeployConfig(module, getEnv());
		cfg.tmp = String.format(cfg.tmp,
				Config.factory().md5(getGroupId().toString()).toString());
		List<String> dirs = App.scanDir(sourcePath.toString(), false);
		for (String dir : dirs) {
			ModuleInfo mInfo = new ModuleInfo();
			Path path = mInfo.sourcePath = Paths.get(dir);
			Path rel = sourcePath.relativize(path);
			mInfo.backupPath = backupPath.resolve(rel);
			mInfo.backupTmp = backupTmp.resolve(rel);
			mInfo.name = path.getFileName().toString();

			DeployConfig subCfg = cfg.clone();
			Path remote = Paths.get(cfg.path);
			remote = remote.resolve(rel);
			subCfg.path = remote.toString();

			remote = Paths.get(cfg.tmp);
			remote = remote.resolve(rel);
			subCfg.tmp = remote.toString();

			mInfo.cfg = subCfg;
			moduleList.add(mInfo);
		}
		return moduleList;
	}

	private List<ModuleInfo> mkMultiModule() {
		String hash = Config.factory().md5(getGroupId().toString()).toString();
		List<ModuleInfo> moduleList = new LinkedList<>();
		Path backupTmp = Config.factory().getBackupTplPath(getGroupId());
		for (String module : moduleInfo) {
			ModuleInfo mInfo = new ModuleInfo();
			DeployConfig cfg = Config.factory().getDeployConfig(module,
					getEnv());
			cfg.tmp = String.format(cfg.tmp, hash);
			mInfo.sourcePath = sourcePath.resolve(module);
			mInfo.backupPath = backupPath.resolve(module);
			mInfo.backupTmp = backupTmp.resolve(module);
			mInfo.name = module;

			mInfo.cfg = cfg;
			moduleList.add(mInfo);
		}
		return moduleList;
	}

	private boolean isRollback() {
		return action != Action.PUSH;
	}

	private boolean pushFile() {
		return true;
	}

	private boolean switchFile() {
		return true;
	}

	private void logout() {
		TaskManagement.factory().logout(groupId);
	}

	/**
	 * @return Push record.
	 */
	private Map<String, Object> getPushRecord() {
		String selectSqlTpl = "select * from %s.svn_push where tid=%s";
		String sql = String.format(selectSqlTpl,
				Config.factory().get("dbName"), groupId);
		try {
			if (!session.query(sql)) {
				Log.record(Log.INFO, getClass(), String.format(
						"Can not find push record from svn_push using tid=%s",
						groupId));
				return null;
			}
			Map<String, Object> map = session.find();
			return map;
		} catch (Exception e) {
			Log.record(Log.ERR, getClass(), e);
			return null;
		}
	}

	/**
	 * @return Module list.
	 */
	private List<String> getModule() {
		String sqlTpl = "select * from %s.svn_test_module as m where m.group_id = %s";
		String sql = String.format(sqlTpl, Config.factory().get("dbName"),
				groupId);
		try {
			if (!session.query(sql)) {
				Log.record(
						Log.INFO,
						getClass(),
						String.format(
								"Can not get module record from svn_test_module using group_id=%s",
								groupId));
				return null;
			}
			List<Map<String, Object>> list = session.getResult();
			List<String> modules = new LinkedList<>();

			Iterator<Map<String, Object>> it = list.iterator();
			while (it.hasNext()) {
				Map<String, Object> item = it.next();
				modules.add(String.valueOf(item.get("module")));
			}
			return modules;
		} catch (Exception e) {
			Log.record(Log.ERR, getClass(), e);
			return null;
		}
	}

	private boolean updateStatus(Integer status) {
		try {
			String sql = String.format(
					"update %s.svn_push set status=%s where tid=%s", Config
							.factory().get("dbName"), status, groupId);
			if (!session.query(sql)) {
				throw new SQLException(String.format(
						"Can not update push record status using tid=%s",
						groupId));
			}
		} catch (Exception e) {
			Log.record(Log.ERR, getClass(), e);
			return false;
		}
		return true;
	}

	@Override
	public String getName() {
		return name;
	}

	/**
	 * Set name
	 */
	private void setName() {
		name = String.format("%s [%s] to %s", action, groupId, env);
	}

	/**
	 * @return the groupId
	 */
	public Integer getGroupId() {
		return groupId;
	}

	/**
	 * @param groupId
	 *            the groupId to set
	 */
	public void setGroupId(Integer groupId) {
		this.groupId = groupId;
	}

	/**
	 * @return the env
	 */
	public Env getEnv() {
		return env;
	}

	/**
	 * @param env
	 *            the env to set
	 */
	public void setEnv(Env env) {
		this.env = env;
	}

	/**
	 * @return the action
	 */
	public Action getAction() {
		return action;
	}

	/**
	 * @param action
	 *            the action to set
	 */
	public void setAction(Action action) {
		this.action = action;
	}

	private static List<String> diff(List<String> source, String sourcePrefix,
			List<String> target, String targetPrefix) {
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

	private static List<String> exec(String[] cmd, File dir) throws IOException {
		Log.record(Log.INFO, TransTask.class,
				"Run shell " + Arrays.toString(cmd));
		String[] envp = Config.factory().getEnvp();

		Runtime runtime = Runtime.getRuntime();
		Process process = runtime.exec(cmd, envp, dir);
		BufferedReader br = new BufferedReader(new InputStreamReader(
				process.getInputStream()));
		LinkedList<String> output = new LinkedList<String>();

		String str;
		while (null != (str = br.readLine())) {
			output.add(str);
			Log.record(Log.INFO, TransTask.class, "InputStream: " + str);
		}
		br.close();
		return output;
	}

	class BackupTask implements Callable<Boolean> {

		private CountDownLatch doneSignal;
		private CountDownLatch startSignal;
		private Path sourcePath;
		private Path backupPath;
		private Path backupTmp;
		private DeployConfig cfg;

		public BackupTask(CountDownLatch doneSignal,
				CountDownLatch startSignal, ModuleInfo moduleInfo) {
			this.doneSignal = doneSignal;
			this.startSignal = startSignal;
			this.sourcePath = moduleInfo.sourcePath;
			this.backupPath = moduleInfo.backupPath;
			this.backupTmp = moduleInfo.backupTmp;
			this.cfg = moduleInfo.cfg;
		}

		private List<String> scanFiles(Path path) {
			List<String> files = App.scanDir(path.toAbsolutePath().toString());
			return files;
		}

		/**
		 * Scan the {@link #sourcePath} and {@link #backupPath}，get the file
		 * list contained in the sourcePath but not in the backupPath.
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
				File cfgFile = File.createTempFile(
						Config.factory().md5(cfg.path).toString(), null);
				PrintWriter pw = new PrintWriter(new BufferedWriter(
						new FileWriter(cfgFile)));
				Path targetPath = Paths.get(cfg.path);
				for (String filePath : finalFiles) {
					if (filePath.charAt(0) == File.separatorChar) {
						filePath = filePath.substring(1);
					}
					Path target = targetPath.resolve(filePath);
					pw.println(target.toString());
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
			String[] cmd = { shell, filePath, host, backupTmp.toString(),
					"2>&1" };
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
				} catch (IOException e) {
					Log.record(Log.ERR, getClass(), e);
				}
			}
			return true;
		}

		@Override
		public Boolean call() {
			try {
				startSignal.await();
				// make module path.
				List<String> finalFiles = parseFiles();
				if (finalFiles.size() <= 0) {
					doneSignal.countDown();
					return true;
				}
				String cfgFile = writeCfg(finalFiles);
				if (false == backup(cfgFile)) {
					doneSignal.countDown();
					return false;
				}

				if (false == trans()) {
					doneSignal.countDown();
					return false;
				}
				doneSignal.countDown();
				return true;
			} catch (Exception e) {
				Log.record(Log.ERR, getClass(), e);
				return false;
			}
		}
	}
}
