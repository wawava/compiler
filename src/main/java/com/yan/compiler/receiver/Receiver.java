package com.yan.compiler.receiver;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.yan.compiler.Log;
import com.yan.compiler.config.Config;

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

	/**
	 * The port.
	 */
	private int port;
	/**
	 * Address.
	 */
	private String addr;
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

	public void init() {
		initListener(null);
		initDeliver();
	}

	public void init(DatagramSocket socket) {
		initListener(socket);
		initDeliver();
	}

	public void initListener(DatagramSocket socket) {
		Log.record(Log.DEBUG, getClass(), "Start Listener.");
		if (null == socket) {
			listener = new ListenerTask(addr, port);
		} else {
			listener = new ListenerTask(socket);
		}

	}

	public void initDeliver() {
		Log.record(Log.DEBUG, getClass(), "Start Deliver.");
		deliver = new DeliverTask();
	}

	public void start() {
		startListener();
		startDeliver();
	}

	/**
	 * Start the Listener.
	 */
	public void startListener() {
		if (null != listener)
			pool.execute(listener);
	}

	/**
	 * start the Deliver.
	 */
	public void startDeliver() {
		if (null != deliver)
			pool.execute(deliver);
	}

	/**
	 * Stop the receiver.
	 */
	public void stop() {
		try {
			if (null != deliver)
				deliver.interrupt();
			if (null != listener)
				listener.interrupt();
		} catch (Exception e) {
			Log.record(Log.ERR, getClass(), e.getMessage());
		}

		pool.shutdown();
		try {
			// Wait a while for existing tasks to terminate
			if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
				// Cancel currently executing tasks
				pool.shutdownNow();
				// Wait a while for tasks to respond to being cancelled
				if (!pool.awaitTermination(60, TimeUnit.SECONDS))
					Log.record(Log.ERR, getClass(), "Pool did not terminate");
			}
		} catch (InterruptedException ie) {
			// (Re-)Cancel if current thread also interrupted
			pool.shutdownNow();
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
	private class ListenerTask extends Thread {
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
			super("ListenerTask");
			try {
				Log.record(Log.DEBUG, getClass(), String.format(
						"Open socket with addr: %s, port: %s.", addr, port));
				socket = new DatagramSocket(port, InetAddress.getByName(addr));
			} catch (Exception e) {
				Log.record(Log.ERR, ListenerTask.class.getName(), e);
				System.exit(0);
			}
		}

		public ListenerTask(DatagramSocket socket) {
			Log.record(Log.DEBUG, getClass(), String.format(
					"Open socket with addr: %s, port: %s.",
					socket.getInetAddress(), socket.getPort()));
			this.socket = socket;
		}

		private boolean running = true;

		@Override
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

		@Override
		public void interrupt() {
			Log.record(Log.INFO, getClass(), "Interrupt thread " + getName());
			running = false;
			socket.close();
			super.interrupt();
		}
	}

	/**
	 * A Deliver thread. This class get massage from massage queue, which as a
	 * json string, decrypt it to {@link PackageBasePackage}, and add packages to a
	 * worker queue dependent on {@link PackageBasePackage#getProject()}.
	 * 
	 * @author Yan
	 * 
	 */
	private class DeliverTask extends Thread {
		private MsgQueue queue;

		public DeliverTask() {
			super("DeliverTask");
			queue = MsgQueue.factory();
		}

		private boolean running = true;

		private String getMsg() {
			return queue.getMsg();
		}

		private AbstractBasePackage parseMsg(String msg) {
			AbstractBasePackage bp = PackageFactory.factory(msg);
			Log.record(Log.DEBUG, getClass(),
					"Decrypt from Json: " + bp.toString());
			return bp;
		}

		private boolean chkStatus(AbstractBasePackage bp) {
			return bp.chkPackage();
		}

		private void deliver(AbstractBasePackage bp) {
			bp.deliver();
		}

		@Override
		public void run() {
			Log.record(Log.DEBUG, getClass(), "Run DeliverTask");
			
			try {
				while (running) {
					Log.record(Log.DEBUG, getClass(),
							"Get massage from MsgQueue");
					String msg = getMsg();
					if (null == msg) {
						continue;
					}
					try {
						AbstractBasePackage bp = parseMsg(msg);
						if (!chkStatus(bp)) {
							continue;
						}
						deliver(bp);
					} catch (Exception e) {
						Log.record(Log.ERR, DeliverTask.class.getName(), e);
					}
				}
			} catch (Exception ie) {
				Log.record(Log.INFO, getClass(), ie.getMessage());
			}
		}

		@Override
		public void interrupt() {
			Log.record(Log.INFO, getClass(), "Interrupt thread " + getName());
			running = false;
			super.interrupt();
			MsgQueue.factory().waikupOnQueue();
		}
	}
}
