package com.yan.compiler.db;

import java.sql.Connection;
import java.sql.DriverManager;
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

	private static Integer idGen = 0;

	private String dbAddr;
	private Integer dbPort;
	private String dbUsername;
	private String dbPassword;
	private String dbName;

	private LinkedList<Session> idle;
	private Map<String, Session> busy;

	private Connection connect = null;

	private ConnManagement() {
		Config config = Config.factory();
		dbAddr = config.get("dbAddr");
		dbPort = Integer.valueOf(config.get("dbPort"));
		dbUsername = config.get("dbUsername");
		dbPassword = config.get("dbPassword");
		dbName = config.get("dbName");

		idle = new LinkedList<Session>();
		busy = new HashMap<String, Session>();

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
		if (null == connect || connect.isClosed()) {
			String str = String.format(connTpl, dbAddr, dbPort, dbName);
			Log.record(Log.DEBUG, getClass(),
					"Connect to database with Connection-String: " + str);
			// setup the connection with the DB.
			connect = DriverManager.getConnection(str, dbUsername, dbPassword);
		}

		Session session;
		Log.record(Log.DEBUG, getClass(), "Lock idle session queue.");
		synchronized (idGen) {
			if (idle.size() > 0) {
				session = idle.removeFirst();
				session.setStatement(connect.createStatement());
			} else {
				session = new Session(String.valueOf(idGen++),
						connect.createStatement());
			}
			busy.put(session.getId(), session);
		}
		Log.record(Log.DEBUG, getClass(), "Unlock idle session queue.");
		return session;
	}

	void free(String id) {
		synchronized (idGen) {
			Log.record(Log.DEBUG, getClass(), "Free Session: " + id);
			Session session = busy.get(id);
			if (null != session) {
				idle.addLast(session);
			}
		}
	}

	public void showdown() {
		Iterator<Entry<String, Session>> it = busy.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, Session> item = it.next();
			Session session = item.getValue();
			try {
				session.showdown();
			} catch (SQLException e) {
				Log.record(Log.ERR, getClass(), e.getMessage());
			}
			idle.add(session);
		}
	}
}
