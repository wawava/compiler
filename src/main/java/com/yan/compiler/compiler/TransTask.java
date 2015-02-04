package com.yan.compiler.compiler;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.yan.compiler.App;
import com.yan.compiler.Log;
import com.yan.compiler.compiler.trans.AbstractTransTask;
import com.yan.compiler.compiler.trans.BackupTask;
import com.yan.compiler.compiler.trans.TaskResult;
import com.yan.compiler.config.Config;
import com.yan.compiler.config.DeployConfig;
import com.yan.compiler.config.Env;
import com.yan.compiler.db.ConnManagement;
import com.yan.compiler.db.Session;
import com.yan.compiler.entities.PushEntity;
import com.yan.compiler.entities.PushLogEntity;
import com.yan.compiler.entities.TestGroupEntity;
import com.yan.compiler.receiver.Action;

public class TransTask implements Task {

	private Integer groupId;
	private Integer uid;
	private Env env;
	private Action action;

	private String name;

	private Session session;
	private Path sourcePath;
	private Path backupPath;
	private Path backupTmp;

	private List<String> moduleInfo;
	private List<ModuleInfo> moduleList;
	private Map<String, Object> push;
	// private Map<String, List<String>> fileList;

	private CountDownLatch doneSignal;
	private CountDownLatch startSignal;

	public TransTask(Integer groupId, Env env, Action action, Integer uid) {
		this.groupId = groupId;
		this.uid = uid;
		this.env = env;
		this.action = action;
		setName();
	}

	@Override
	public void run() {

		if (false == init()) {
			return;
		}

		// backup if action is push
		if (false == backup()) {
			return;
		}

		// transport files
		if (false == pushFile()) {
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
			session = ConnManagement.factory().connect(false);
		} catch (SQLException e) {
			Log.record(Log.ERR, getClass(), e);
			return false;
		}
		return true;
	}

	private boolean verify() {
		Map<String, Object> push = getPushRecord();
		Integer status = Integer.valueOf(String.valueOf(push
				.get(PushEntity.COLUMN_STATUS)));
		Integer official = Integer.valueOf(String.valueOf(push
				.get(PushEntity.COLUMN_OFFICIAL)));
		if (!PushEntity.STATUS_READY_TO_EXECUTE.equals(status)
				&& !PushEntity.OFFICIAL_ALREADY_OFFICIAL.equals(official)
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

		if (null == (backupTmp = Config.factory()
				.getBackupTplPath(getGroupId()))) {
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

		if (false == updateStatus(PushEntity.STATUS_RECEIVED)) {
			return false;
		}

		if (1 == moduleInfo.size()) {
			moduleList = mkSingleModule();
		} else {
			moduleList = mkMultiModule();
		}

		return true;
	}

	/**
	 * Make module info
	 * 
	 * @return
	 */
	private List<ModuleInfo> mkSingleModule() {
		String module = moduleInfo.get(0);

		List<ModuleInfo> moduleList = new LinkedList<>();
		DeployConfig cfg = Config.factory().getDeployConfig(module, getEnv());
		cfg.updateTmp(Config.factory().md5(getGroupId().toString()).toString());
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

	/**
	 * Make module info
	 * 
	 * @return
	 */
	private List<ModuleInfo> mkMultiModule() {
		String hash = Config.factory().md5(getGroupId().toString()).toString();
		List<ModuleInfo> moduleList = new LinkedList<>();
		for (String module : moduleInfo) {
			ModuleInfo mInfo = new ModuleInfo();
			DeployConfig cfg = Config.factory().getDeployConfig(module,
					getEnv());
			cfg.updateTmp(hash);
			mInfo.sourcePath = sourcePath.resolve(module);
			mInfo.backupPath = backupPath.resolve(module);
			mInfo.backupTmp = backupTmp.resolve(module);
			mInfo.name = module;

			mInfo.cfg = cfg;
			moduleList.add(mInfo);
		}
		return moduleList;
	}

	/**
	 * @return True if action is rollback
	 */
	private boolean isRollback() {
		return action != Action.PUSH;
	}

	private Long waitFor() {
		Date before = new Date();
		startSignal.countDown();
		try {
			doneSignal.await();
		} catch (InterruptedException e) {
			Log.record(Log.ERR, getClass(), e);
			return -1L;
		}
		Date after = new Date();
		return after.getTime() - before.getTime();
	}

	private LinkedList<Future<TaskResult>> makeSubTread(
			Class<? extends AbstractTransTask> clazz)
			throws NoSuchMethodException, SecurityException,
			InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException {
		Constructor<? extends AbstractTransTask> cons = clazz.getConstructor(
				CountDownLatch.class, CountDownLatch.class, ModuleInfo.class);

		LinkedList<Future<TaskResult>> futureList = new LinkedList<>();
		for (ModuleInfo module : moduleList) {
			AbstractTransTask task = cons.newInstance(doneSignal, startSignal,
					module);
			Future<TaskResult> future = TaskManagement.factory().submit(task);
			futureList.addLast(future);
		}
		return futureList;
	}

	private LinkedList<TaskResult> getTaskResult(
			List<Future<TaskResult>> futureList) {
		LinkedList<TaskResult> resultList = new LinkedList<>();
		for (Future<TaskResult> future : futureList) {
			try {
				resultList.addLast(future.get());
			} catch (Exception e) {
				Log.record(Log.ERR, getClass(), e);
			}
		}

		return resultList;
	}

	/**
	 * backup files.
	 * 
	 * @return
	 */
	private boolean backup() {
		if (isRollback()) {
			return true;
		}
		updateStatus(PushEntity.STATUS_BACKUP);

		LinkedList<Future<TaskResult>> futureList;
		try {
			futureList = makeSubTread(BackupTask.class);
		} catch (NoSuchMethodException | SecurityException
				| InstantiationException | IllegalAccessException
				| IllegalArgumentException | InvocationTargetException e1) {
			Log.record(Log.ERR, getClass(), e1);
			return false;
		}

		Long last = 0L;
		if (-1L == (last = waitFor())) {
			return false;
		}

		LinkedList<TaskResult> resultList = getTaskResult(futureList);

		boolean success = true;
		JsonObject jObject = new JsonObject();
		Integer files = 0;
		for (TaskResult result : resultList) {
			try {
				success = success && result.success;
				String module = result.module;
				JsonArray fileList = (JsonArray) result.obj;
				files += fileList.size();
				jObject.add(module, fileList);
			} catch (Exception e) {
				Log.record(Log.ERR, getClass(), e);
				success = false;
			}

		}
		Log.record(Log.INFO, getClass(),
				String.format("Backup result: [%s]", success));
		actionLog(Integer.valueOf((int) (last / 1000)), jObject, files,
				PushLogEntity.PUSH_SPECIES_BACKUP, success);
		return success;
	}

	private boolean pushFile() {
		updateStatus(PushEntity.STATUS_TRANS);

		LinkedList<Future<TaskResult>> futureList;
		try {
			futureList = makeSubTread(BackupTask.class);
		} catch (NoSuchMethodException | SecurityException
				| InstantiationException | IllegalAccessException
				| IllegalArgumentException | InvocationTargetException e1) {
			Log.record(Log.ERR, getClass(), e1);
			return false;
		}

		Long last = 0L;
		if (-1L == (last = waitFor())) {
			return false;
		}

		LinkedList<TaskResult> resultList = getTaskResult(futureList);

		boolean success = true;
		JsonObject jObject = new JsonObject();
		Integer files = 0;
		for (TaskResult result : resultList) {
			try {
				success = success && result.success;
				String module = result.module;
				JsonArray fileList = (JsonArray) result.obj;
				files += fileList.size();
				jObject.add(module, fileList);
			} catch (Exception e) {
				Log.record(Log.ERR, getClass(), e);
				success = false;
			}

		}
		Log.record(Log.INFO, getClass(),
				String.format("Push result: [%s]", success));

		actionLog(Integer.valueOf((int) (last / 1000)), jObject, files,
				PushLogEntity.PUSH_SPECIES_BACKUP, success);

		return success;
	}

	private boolean switchFile() {
		updateStatus(PushEntity.STATUS_TRANS);

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

	/**
	 * Update push status.
	 * 
	 * @param status
	 * @return
	 */
	private boolean updateStatus(Integer status) {
		try {
			String sql = PushEntity.updateStatus(groupId, status);
			if (!session.query(sql)) {
				throw new SQLException(String.format(
						"Can not update push record status using tid=%s",
						groupId));
			}
			session.commit();
		} catch (Exception e) {
			Log.record(Log.ERR, getClass(), e);
			try {
				session.rollback();
			} catch (SQLException e1) {
				Log.record(Log.ERR, getClass(), e1);
			}
			return false;
		}
		return true;
	}

	/**
	 * Set name
	 */
	private void setName() {
		name = String.format("%s [%s] to %s", action, groupId, env);
	}

	@Override
	public String getName() {
		return name;
	}

	/**
	 * @return the groupId
	 */
	public Integer getGroupId() {
		return groupId;
	}

	/**
	 * @return the env
	 */
	public Env getEnv() {
		return env;
	}

	/**
	 * @return the action
	 */
	public Action getAction() {
		return action;
	}

	/**
	 * @return the uid
	 */
	public Integer getUid() {
		return uid;
	}

	/**
	 * 记录操作日志，不管是否成功都略过
	 * 
	 * @param last
	 * @param files
	 * @param fileCount
	 * @param $dir
	 * @param logType
	 * @param $callback
	 */
	protected void actionLog(Integer last, JsonObject files, Integer fileCount,
			Integer logType, Boolean success) {
		Integer prev = Integer.valueOf(String.valueOf(push.get("process")));
		Integer next = env.toInteger();
		JsonObject info = PushLogEntity.mkInfo(prev, next, last, fileCount,
				files, success);
		String insertSql = PushLogEntity.mkInsertSql(logType, getGroupId(),
				getUid(), Integer.valueOf(String.valueOf(push.get("version"))),
				info);

		String updateSql = TestGroupEntity.mkUpdateSql(logType, getUid(),
				getGroupId());

		try {
			session.query(insertSql);
			session.query(updateSql);
			session.commit();
		} catch (SQLException e) {
			Log.record(Log.ERR, getClass(), e);
		}
	}

}
