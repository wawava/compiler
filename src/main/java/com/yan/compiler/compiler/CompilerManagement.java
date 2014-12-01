package com.yan.compiler.compiler;

import java.io.IOException;
import java.lang.reflect.Type;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.yan.compiler.Log;
import com.yan.compiler.config.Config;
import com.yan.compiler.db.ConnManagement;
import com.yan.compiler.db.Session;
import com.yan.compiler.receiver.BasePackage;
import com.yan.compiler.receiver.MsgQueue;

public class CompilerManagement {
	public static final int LOG_TYPE_COPY_FILE_ERROR = 17;
	public static final int LOG_TYPE_COMPILE_PROJECT_ERROR = 18;
	public static final int LOG_TYPE_COMPLETE_COMPILE = 21;

	private static CompilerManagement obj;

	public static CompilerManagement factory() {
		if (null == obj) {
			obj = new CompilerManagement();
		}
		return obj;
	}

	private ExecutorService pool;

	private Map<String, Runnable> threads;

	private MsgQueue queue;

	private CompilerManagement() {
		pool = Executors.newCachedThreadPool();
		threads = new HashMap<String, Runnable>();
		queue = MsgQueue.factory();
	}

	public boolean createWorker(String project) {
		if (threads.containsKey(project)) {
			return true;
		}

		Task task = new Task(project);
		threads.put(project, task);
		Log.record(Log.INFO, getClass(), "Create Worker Thread: " + project);
		pool.execute(task);
		return true;
	}

	public void stop() {
		Iterator<Entry<String, Runnable>> it = threads.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, Runnable> entry = it.next();
			Task task = (Task) entry.getValue();
			task.stop();
		}

		pool.shutdown();
		try {
			// Wait a while for existing tasks to terminate
			if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
				// Cancel currently executing tasks
				pool.shutdownNow();
				// Wait a while for tasks to respond to being cancelled
				if (!pool.awaitTermination(60, TimeUnit.SECONDS))
					System.err.println("Pool did not terminate");
			}
		} catch (InterruptedException ie) {
			// (Re-)Cancel if current thread also interrupted
			pool.shutdownNow();
			// Preserve interrupt status
			Thread.currentThread().interrupt();
		}
	}

	class Task implements Runnable {

		private String project;

		private boolean running = true;

		private Session session;

		private Gson gson;

		public Task(String project) {
			this.project = project;
			gson = new Gson();
		}

		public void run() {
			Log.record(Log.DEBUG, getClass(), "Run Compiler-Task: " + project);
			while (running) {
				BasePackage bp = queue.getPackage(project);

				try {
					session = ConnManagement.factory().connect();
					updateStatus(bp.getId(), CompilerTask.STATUS_COMPILING);
				} catch (SQLException e1) {
					Log.record(Log.ERR, Task.class.getName(), e1);
					continue;
				}

				Date begin = new Date();
				CompilerTask task = new CompilerTask(bp);
				try {
					task.deployFile();
				} catch (IOException e) {
					Log.record(Log.ERR, Task.class.getName(), e);
					HashMap<String, Object> map = new HashMap<String, Object>();
					map.put("trace", formatStackTrace(e));
					log(LOG_TYPE_COPY_FILE_ERROR, bp.getUid(),
							bp.getReversion(), bp.getId(), map, map.getClass());
					continue;
				}

				boolean hasErr = false;
				try {
					hasErr = task.compile();
				} catch (IOException e1) {
					Log.record(Log.ERR, Task.class.getName(), e1);
					HashMap<String, Object> map = new HashMap<String, Object>();
					map.put("trace", formatStackTrace(e1));
					List<String> relativeFile = task.getRelativeFiles();
					String[] files = relativeFile.toArray(new String[0]);
					map.put("relativeFiles", files);
					log(LOG_TYPE_COMPILE_PROJECT_ERROR, bp.getUid(),
							bp.getReversion(), bp.getId(), map, map.getClass());

					try {
						task.restoreFile();
					} catch (IOException e) {
						Log.record(Log.ERR, Task.class.getName(), e);
					}
					continue;
				}

				Date end = new Date();
				long lastTime = end.getTime() - begin.getTime();
				List<String> relativeFile = task.getRelativeFiles();
				HashMap<String, Object> map = mkInfo(bp.getThisProcess(),
						bp.getProcess(), lastTime, relativeFile.size());
				map.put("relativeFiles", relativeFile);
				map.put("compileOutput", task.getLog());
				log(LOG_TYPE_COMPILE_PROJECT_ERROR, bp.getUid(),
						bp.getReversion(), bp.getId(), map, map.getClass());

				try {
					if (!hasErr) {
						updateStatus(bp.getId(),
								CompilerTask.STATUS_ALREADY_EXECUTE,
								CompilerTask.OFFICIAL_ALREADY_OFFICIAL);
						// TODO upload file and restart tomcat.

					} else {
						updateStatus(bp.getId(),
								CompilerTask.STATUS_ALREADY_EXECUTE);
						try {
							task.restoreFile();
						} catch (IOException e) {
							Log.record(Log.ERR, Task.class.getName(), e);
						}
					}
					session.close();
				} catch (SQLException e) {
					Log.record(Log.ERR, Task.class.getName(), e);
				}
				session = null;
			}
		}

		/**
		 * @return the project
		 */
		public String getProject() {
			return project;
		}

		public void stop() {
			running = false;
		}

		private String insertLogSqlTpl = "insert into %s.svn_push_log (tid, uid, time, type, reversion, info) values(%s, %s, %s, %s, %s, '%s')";

		private void log(int type, int uid, int revision, int tid, Object info,
				Type typeOfSrc) {
			String jsonStr = gson.toJson(info, typeOfSrc);
			String dbName = Config.factory().get("dbName");
			long time = (new Date()).getTime();
			time = time / 1000;
			String sql = String.format(insertLogSqlTpl, dbName, tid, uid, time,
					type, revision, jsonStr);
			try {
				session.query(sql);
			} catch (SQLException e) {
				Log.record(Log.ERR, Task.class.getName(), e);
			}
		}

		private String formatStackTrace(Exception e) {
			StringBuilder sbd = new StringBuilder();
			sbd.append(e.toString());
			StackTraceElement[] trace = e.getStackTrace();
			for (StackTraceElement traceElement : trace) {
				sbd.append("\tat ");
				sbd.append(traceElement.toString());
			}
			return sbd.toString();
		}

		private HashMap<String, Object> mkInfo(Integer thisProcess,
				Integer process, Long lastTime, Integer fileCount) {
			// $info = [
			// PushLogModel::COLUMN_INFO_PREV => $thisProcess,
			// PushLogModel::COLUMN_INFO_NEXT => $process,
			// PushLogModel::COLUMN_INFO_RELATIVE_FILES => [],
			// PushLogModel::COLUMN_INFO_LAST_TIME => $endTime - $beginTime,
			// PushLogModel::COLUMN_INFO_FILE_COUNT => $fileCount
			// ];
			HashMap<String, Object> map = new HashMap<String, Object>();
			map.put("prev", thisProcess);
			map.put("next", process);
			map.put("relativeFiles", new Object());
			map.put("lastTime", lastTime);
			map.put("count", fileCount);

			return map;
		}

		private void updateStatus(Integer id, Integer status)
				throws SQLException {
			String sql = String.format(
					"update %s.svn_push set status=%s where tid=%s", Config
							.factory().get("dbName"), status, id);
			if (!session.query(sql)) {
				throw new SQLException(String.format(
						"Can not update push record status using tid=%s", id));
			}
		}

		private void updateStatus(Integer id, Integer status, Integer official)
				throws SQLException {
			String sql = String
					.format("update %s.svn_push set status=%s, official=%s where tid=%s",
							Config.factory().get("dbName"), status, official,
							id);
			if (!session.query(sql)) {
				throw new SQLException(String.format(
						"Can not update push record status using tid=%s", id));
			}
		}
	}
}
