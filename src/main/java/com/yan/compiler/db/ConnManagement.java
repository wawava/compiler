package com.yan.compiler.db;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.yan.compiler.Log;
import com.yan.compiler.config.Config;

public class ConnManagement {
	private static String connTpl = "jdbc:mysql://%s:%s/%s?useUnicode=true&characterEncoding=UTF-8";

	private static ConnManagement obj;

	public static ConnManagement factory() {
		if (null == obj) {
			Log.record(Log.DEBUG, ConnManagement.class,
					"Factory ConnManagement.");
			obj = new ConnManagement();
		}
		return obj;
	}

	private String dbAddr;
	private Integer dbPort;
	private String dbUsername;
	private String dbPassword;
	private String dbName;
	private String dbUrl;

	private LinkedList<MyConnection> normal;
	private Map<Integer, MyConnection> transaction;

	private ConnManagement() {
		Config config = Config.factory();
		dbAddr = config.get("dbAddr");
		dbPort = Integer.valueOf(config.get("dbPort"));
		dbUsername = config.get("dbUsername");
		dbPassword = config.get("dbPassword");
		dbName = config.get("dbName");

		dbUrl = String.format(connTpl, dbAddr, dbPort, dbName);

		normal = new LinkedList<MyConnection>();
		transaction = new HashMap<Integer, MyConnection>();

		Boolean sshTunnel = config.usingSSHTunnel();
		if (sshTunnel) {
			String sshHost = config.get("sshHost");
			Integer sshPort = Integer.valueOf(config.get("sshPort"));
			String sshUsername = config.get("sshUsername");
			String sshPassword = config.get("sshPassword");
			String sshLocalPort = config.get("sshLocalPort");

			JSch jsch = new JSch();
			// Create SSH session. Port 22 is your SSH port which
			// is open in your firewall setup.
			com.jcraft.jsch.Session session;
			try {
				session = jsch.getSession(sshUsername, sshHost, sshPort);
				session.setPassword(sshPassword);

				// Additional SSH options. See your ssh_config manual for
				// more options. Set options according to your requirements.
				java.util.Properties sshConfig = new java.util.Properties();
				sshConfig.put("StrictHostKeyChecking", "no");
				sshConfig.put("Compression", "yes");
				sshConfig.put("ConnectionAttempts", "2");

				session.setConfig(sshConfig);
				// Connect
				session.connect();

				// Create the tunnel through port forwarding.
				// This is basically instructing jsch session to send
				// data received from local_port in the local machine to
				// remote_port of the remote_host
				// assigned_port is the port assigned by jsch for use,
				// it may not always be the same as
				// local_port.
				// port:host:hostport
				dbPort = session.setPortForwardingL(String.format("%s:%s:%s",
						sshLocalPort, sshHost, sshPort));
				dbAddr = "localhost";
			} catch (JSchException e) {
				Log.record(Log.ERR, getClass(), e.getMessage());
				System.exit(0);
			}
		}

		// this will load the MySQL driver, each DB has its own driver
		try {
			Log.record(Log.DEBUG, getClass(),
					"Load driver com.mysql.jdbc.Driver");
			Class.forName("com.mysql.jdbc.Driver").newInstance();
		} catch (Exception e) {
			Log.record(Log.ERR, ConnManagement.class.getSimpleName(), e);
			System.exit(0);
		}
	}

	public Session connect() throws SQLException {
		Log.record(Log.DEBUG, getClass(), "Lock ConnManagement");
		MyConnection conn = getConn();
		return conn.connect();
	}

	public Session connect(Boolean trans) throws SQLException {
		if (true == trans) {
			return connect();
		}
		Log.record(Log.DEBUG, getClass(), "Lock ConnManagement");

		MyConnection conn = getConn(trans);
		return conn.connect();
	}

	private MyConnection getConn() throws SQLException {
		MyConnection conn = null;
		synchronized (this) {
			Iterator<MyConnection> it = normal.iterator();
			while (it.hasNext()) {
				MyConnection _conn = it.next();
				if (!_conn.isBusy()) {
					conn = _conn;
					break;
				}
			}
			if (null == conn) {
				conn = MyConnection
						.getConnection(dbUrl, dbUsername, dbPassword);
				normal.addLast(conn);
			}
		}
		return conn;
	}

	private MyConnection getConn(Boolean trans) throws SQLException {
		if (true == trans) {
			return getConn();
		}
		MyConnection conn = null;
		synchronized (this) {
			Iterator<MyConnection> it = normal.iterator();
			while (it.hasNext()) {
				MyConnection _conn = it.next();
				if (0 == _conn.size()) {
					conn = _conn;
					it.remove();
					break;
				}
			}
			if (null == conn) {
				conn = MyConnection
						.getConnection(dbUrl, dbUsername, dbPassword);
				conn.setAutoCommit(false);
			}
			transaction.put(conn.getId(), conn);
		}
		return conn;
	}

	/**
	 * Free transaction
	 * 
	 * @param id
	 */
	public void free(Integer id) {
		synchronized (this) {
			Log.record(Log.DEBUG, getClass(), "Commit Connection: " + id);
			MyConnection conn = transaction.get(id);
			if (null != conn) {
				try {
					conn.rollback();
					conn.freeAll();
					conn.setAutoCommit(true);
				} catch (Exception e) {
					Log.record(Log.ERR, getClass(), e);
				}
				transaction.remove(id);
				normal.addLast(conn);
			}
		}
	}

	public void showdown() {
		synchronized (this) {
			Iterator<Entry<Integer, MyConnection>> it = transaction.entrySet()
					.iterator();
			while (it.hasNext()) {
				Entry<Integer, MyConnection> item = it.next();
				MyConnection conn = item.getValue();
				try {
					conn.close();
				} catch (Exception e) {
					Log.record(Log.ERR, getClass(), e);
				}
			}
		}
		synchronized (this) {
			Iterator<MyConnection> it = normal.iterator();
			while (it.hasNext()) {
				MyConnection conn = it.next();
				try {
					conn.close();
				} catch (Exception e) {
					Log.record(Log.ERR, getClass(), e);
				}
			}
		}
	}

	public MyConnection getConnection(Integer id) {
		if (transaction.containsKey(id)) {
			return transaction.get(id);
		}
		Iterator<MyConnection> it = normal.iterator();
		while (it.hasNext()) {
			MyConnection conn = it.next();
			if (conn.getId() == id) {
				return conn;
			}
		}
		return null;
	}
}
