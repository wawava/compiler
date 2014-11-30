package com.yan.compiler.compiler;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.yan.compiler.Log;
import com.yan.compiler.db.ConnManagement;
import com.yan.compiler.db.Session;
import com.yan.compiler.receiver.BasePackage;
import com.yan.compiler.receiver.MsgQueue;

public class CompilerManagement {

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
		Log.record(Log.INFO, "Create Worker Thread", project);
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

		public Task(String project) {
			this.project = project;
		}

		public void run() {
			while (running) {
				try {
					session = ConnManagement.factory().connect();
				} catch (SQLException e1) {
					Log.record(Log.ERR, Task.class.getName(), e1);
					continue;
				}

				BasePackage bp = queue.getPackage(project);
				CompilerTask task = new CompilerTask(bp);
				try {
					task.deployFile();
				} catch (IOException e) {
					Log.record(Log.ERR, Task.class.getName(), e);

				}

				try {
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

		private void log(int type) {

		}

	}
}
