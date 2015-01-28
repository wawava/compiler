package com.yan.compiler.receiver;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import com.yan.compiler.Log;
import com.yan.compiler.compiler.CompilerManagement;

public class MsgQueue {
	private static MsgQueue obj = null;

	public static MsgQueue factory() {
		if (null == obj) {
			obj = new MsgQueue();
		}
		return obj;
	}

	/**
	 * udp msg queue.
	 */
	private LinkedList<String> queue = null;
	/**
	 * The worker package queue.
	 */
	private Map<String, LinkedList<AbstractBasePackage>> workQueue;

	private MsgQueue() {
		queue = new LinkedList<String>();
		workQueue = new HashMap<String, LinkedList<AbstractBasePackage>>();
	}

	/**
	 * Add massage to massage queue. and this method will notify all threads
	 * waiting on {@link #queue}
	 * 
	 * @param msg
	 */
	public void addMsg(String msg) {
		synchronized (queue) {
			queue.addLast(msg);
			queue.notifyAll();
			Log.record(Log.INFO, getClass(), "addMsg: " + msg);
		}
	}

	/**
	 * Get massage from massage queue. If message is empty, the thread want to
	 * get message will wait on {@link #queue} until a message string has been
	 * add to queue.
	 * 
	 * @return A json string.
	 */
	public String getMsg() {
		String msg = null;
		synchronized (queue) {
			if (queue.size() <= 0) {
				try {
					queue.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			try {
				msg = queue.removeFirst();
			} catch (Exception e) {
				msg = null;
			}
			Log.record(Log.INFO, getClass(), "Get Massage: " + msg);
		}
		return msg;
	}

	public void waikupOnQueue() {
		synchronized (queue) {
			queue.notifyAll();
		}
	}

	public void waikupOnWork() {
		Iterator<Entry<String, LinkedList<AbstractBasePackage>>> it = workQueue
				.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, LinkedList<AbstractBasePackage>> entry = it.next();
			LinkedList<AbstractBasePackage> list = entry.getValue();
			synchronized (list) {
				list.notifyAll();
			}
		}
	}

	/**
	 * Add a {@linkplain PackageBasePackage} to {@link #workQueue}. This method
	 * will notify all threads waiting on a work queue.
	 * 
	 * @param bp
	 */
	public void addPackage(AbstractBasePackage bp) {
		String project = bp.getProject();
		if (null == project) {
			return;
		}
		Log.record(Log.DEBUG, getClass(), "Lock workQueue");
		synchronized (workQueue) {
			if (!workQueue.containsKey(project)) {
				LinkedList<AbstractBasePackage> list = new LinkedList<AbstractBasePackage>();
				Log.record(Log.INFO, getClass(), "Create BasePackage List: "
						+ project);
				workQueue.put(project, list);
			}
			CompilerManagement manage = CompilerManagement.factory();
			manage.createWorker(project);
		}
		Log.record(Log.DEBUG, getClass(), "Unlock workQueue");
		LinkedList<AbstractBasePackage> list = workQueue.get(project);
		Log.record(Log.DEBUG, getClass(), "Lock workQueue.list: " + project);
		synchronized (list) {
			list.addLast(bp);
			list.notifyAll();
			Log.record(Log.INFO, getClass(), String.format(
					"Add Package to workQueue.list: [%s] - [%s]", project,
					bp.toString()));
		}
		Log.record(Log.DEBUG, getClass(), "Unlock workQueue.list: " + project);
	}

	/**
	 * Get a {@linkplain PackageBasePackage} from work queue. If a queue is empty,
	 * the thread witch will wait until a package has been add to the queue.
	 * 
	 * @param project
	 *            The project the thread wants to get.
	 * @return {@linkplain AbstractBasePackage}
	 */
	public AbstractBasePackage getPackage(String project) {
		LinkedList<AbstractBasePackage> list;
		AbstractBasePackage bp = null;
		synchronized (workQueue) {
			list = workQueue.get(project);
		}
		if (null != list) {
			synchronized (list) {
				if (list.size() <= 0) {
					try {
						list.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

				}
				try {
					bp = list.removeFirst();
					Log.record(Log.INFO, getClass(),
							"Get BasePackage: " + bp.toString());
				} catch (Exception e) {
					bp = null;
				}
			}
		}
		return bp;
	}

}
