package com.yan.compiler.receiver;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.yan.compiler.Log;
import com.yan.compiler.compiler.CompilerTask;
import com.yan.compiler.config.Config;
import com.yan.compiler.db.ConnManagement;
import com.yan.compiler.db.Session;

public class Receiver {
	/**
	 * The buffer length.
	 */
	public static final int PACKAGE_LENGTH = 65535;

	private static Receiver obj = null;

	public static Receiver factory() {
		if (null == obj) {
			Log.record(Log.DEBUG, Receiver.class, "factory Receiver");
			Config config = Config.factory();
			obj = new Receiver(config.getAddr(), config.getPort());
		}
		return obj;
	}

	private Gson gson = new Gson();

	/**
	 * The port.
	 */
	private int port;
	/**
	 * Address.
	 */
	private String addr;
	/**
	 * The thread pool of listener. only one thread will be add to this pool.
	 */
	private ExecutorService listenerThreadPool;
	/**
	 * The listener thread.
	 */
	private ListenerTask listener;
	/**
	 * The thread pool contain the listener and diliver.
	 */
	private ExecutorService pool;
	/**
	 * The deliver thread.
	 */
	private DeliverTask deliver;

	/**
	 * @param addr
	 * @param port
	 */
	private Receiver(String addr, int port) {
		this.addr = addr;
		this.port = port;
		Log.record(Log.DEBUG, getClass(),
				String.format("addr: %s, port: %s", addr, port));
		pool = Executors.newCachedThreadPool();
	}

	/**
	 * Start this receiver.
	 */
	public void start() {
		Log.record(Log.DEBUG, getClass(), "Start Listener.");
		listener = new ListenerTask(addr, port);
		pool.execute(listener);

		Log.record(Log.DEBUG, getClass(), "Start Deliver.");
		deliver = new DeliverTask();
		pool.execute(deliver);
	}

	/**
	 * Stop the receiver.
	 */
	public void stop() {
		listener.stop();
		deliver.stop();
		listenerThreadPool.shutdown();
		try {
			// Wait a while for existing tasks to terminate
			if (!listenerThreadPool.awaitTermination(60, TimeUnit.SECONDS)) {
				// Cancel currently executing tasks
				listenerThreadPool.shutdownNow();
				// Wait a while for tasks to respond to being cancelled
				if (!listenerThreadPool.awaitTermination(60, TimeUnit.SECONDS))
					System.err.println("Pool did not terminate");
			}
		} catch (InterruptedException ie) {
			// (Re-)Cancel if current thread also interrupted
			listenerThreadPool.shutdownNow();
			// Preserve interrupt status
			Thread.currentThread().interrupt();
			Log.record(Log.ERR, Receiver.class.getName(), ie);
		}
	}

	/**
	 * The listener task. This task only listen to specific port and add
	 * massages it receive to a queue.
	 * 
	 * @author Yan
	 * 
	 */
	private class ListenerTask implements Runnable {
		/**
		 * Socket.
		 */
		private DatagramSocket socket;

		/**
		 * 
		 * @param addr
		 * @param port
		 */
		public ListenerTask(String addr, int port) {
			try {
				Log.record(Log.DEBUG, getClass(), String.format(
						"Open socket with addr: %s, port: %s.", addr, port));
				socket = new DatagramSocket(port, InetAddress.getByName(addr));
			} catch (Exception e) {
				Log.record(Log.ERR, ListenerTask.class.getName(), e);
				System.exit(0);
			}
		}

		/**
		 * true if running. false else.
		 */
		private boolean running = true;

		public void run() {
			Log.record(Log.DEBUG, getClass(), "Run ListenerTask");
			MsgQueue queue = MsgQueue.factory();
			while (running) {
				byte[] buf = new byte[PACKAGE_LENGTH];
				DatagramPacket dp = new DatagramPacket(buf, buf.length);
				try {
					socket.receive(dp);
					buf = dp.getData();
					String msg = new String(buf, 0, dp.getLength());
					Log.record(Log.DEBUG, getClass(), "Receive msg: " + msg);
					queue.addMsg(msg);
				} catch (IOException e) {
					Log.record(Log.ERR, ListenerTask.class.getName(), e);
				}
			}
		}

		/**
		 * Stop this thread.
		 */
		public void stop() {
			running = false;
		}
	}

	/**
	 * A Deliver thread. This class get massage from massage queue, which as a
	 * json string, decrypt it to {@link BasePackage}, and add packages to a
	 * worker queue dependent on {@link BasePackage#getProject()}.
	 * 
	 * @author Yan
	 * 
	 */
	private class DeliverTask implements Runnable {
		private Session session;

		public DeliverTask() {
			try {
				session = ConnManagement.factory().connect();
			} catch (SQLException e) {
				Log.record(Log.ERR, getClass(), e.getMessage());
			}
		}

		/**
		 * true while thread running, false else.
		 */
		private boolean running = true;

		/**
		 * stop the thread.
		 */
		public void stop() {
			running = false;
		}

		public void run() {
			Log.record(Log.DEBUG, getClass(), "Run DeliverTask");
			MsgQueue queue = MsgQueue.factory();

			String selectSqlTpl = "select * from %s.svn_push where tid=%s";
			String updateSqlTpl = "update %s.svn_push set status=%s where tid=%s";
			String dbName = Config.factory().get("dbName");

			while (running) {
				Log.record(Log.DEBUG, getClass(), "Get massage from MsgQueue");
				String msg = queue.getMsg();
				if (null != msg) {
					try {
						BasePackage bp = gson.fromJson(msg, BasePackage.class);
						Log.record(Log.DEBUG, getClass(), "Decrypt from Json: "
								+ bp.toString());
						String sql = String.format(selectSqlTpl, dbName,
								bp.getId());
						if (!session.query(sql)) {
							throw new Exception(
									String.format(
											"Can not find push record from svn_push using tid=%s",
											bp.getId()));
						}
						LinkedList<Map<String, Object>> result = session
								.getResult();
						Map<String, Object> map = result.getFirst();
						Integer status = Integer.valueOf(String.valueOf(map
								.get("status")));
						if (!CompilerTask.STATUS_PRE_COMPILING.equals(status)) {
							Log.record(Log.INFO, getClass(), String.format(
									"Push status [%s] is not pre_compiling",
									status));
							continue;
						}
						sql = String.format(updateSqlTpl, dbName,
								CompilerTask.STATUS_COMPILER_RECEIVED,
								bp.getId());
						if (!session.query(sql)) {
							throw new Exception(
									String.format(
											"Can not update push record status to received using tid=%s",
											bp.getId()));
						}
						queue.addPackage(bp);
					} catch (Exception e) {
						Log.record(Log.ERR, DeliverTask.class.getName(), e);
					}
				}
			}
		}
	}
}
