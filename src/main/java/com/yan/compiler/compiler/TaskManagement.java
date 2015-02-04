package com.yan.compiler.compiler;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.yan.compiler.receiver.AbstractBasePackage;

public class TaskManagement {

	private static TaskManagement instance;

	public static TaskManagement factory() {
		if (null == instance) {
			instance = new TaskManagement();
		}
		return instance;
	}

	/**
	 * The thread pool
	 */
	private ExecutorService pool;

	/**
	 * The ssh task pool
	 */
	private ExecutorService sshTaskPool;

	/**
	 * 
	 */
	private Set<Integer> set;

	private TaskManagement() {
		pool = Executors.newCachedThreadPool();
		sshTaskPool = Executors.newFixedThreadPool(10);
		set = new HashSet<Integer>();
	}

	/**
	 * Returns {@code true} if the executor contain a specified ID
	 * 
	 * @param id
	 * @return
	 */
	public boolean contains(Integer id) {
		synchronized (set) {
			return set.contains(id);
		}
	}

	/**
	 * Register a ID
	 * 
	 * @param id
	 * @return
	 */
	public boolean register(Integer id) {
		synchronized (set) {
			return set.add(id);
		}
	}

	/**
	 * Remove a ID
	 * 
	 * @param id
	 * @return
	 */
	public boolean logout(Integer id) {
		synchronized (set) {
			return set.remove(id);
		}
	}

	public void clear() {
		synchronized (set) {
			set.clear();
		}
	}

	/**
	 * Create a {@link Task}
	 * 
	 * @param pkg
	 * @return
	 */
	public Task createTask(AbstractBasePackage pkg) {
		register(pkg.getTaskId());
		return pkg.createTask();
	}

	/**
	 * Execute a task.
	 * 
	 * @param task
	 */
	public void execute(Task task) {
		pool.execute(task);
	}

	/**
	 * 
	 * @param task
	 * @return
	 */
	public <V> Future<V> submit(Callable<V> task) {
		return sshTaskPool.submit(task);
	}

}
