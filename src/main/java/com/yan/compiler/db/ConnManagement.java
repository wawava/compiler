package com.yan.compiler.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import com.yan.compiler.Log;
import com.yan.compiler.config.Config;

public class ConnManagement {
	private static String connTpl = "jdbc:mysql://%s:%s/%s";

	private static ConnManagement obj;

	public static ConnManagement factory() {
		if (null == obj) {
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

		// this will load the MySQL driver, each DB has its own driver
		try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
		} catch (Exception e) {
			Log.record(Log.ERR, ConnManagement.class.getSimpleName(), e);
		}
	}

	public Session connect() throws SQLException {
		if (null == connect || connect.isClosed()) {
			String str = String.format(connTpl, dbAddr, dbPort, dbName);
			// setup the connection with the DB.
			connect = DriverManager.getConnection(str, dbUsername, dbPassword);
		}

		Session session;
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
		return session;
	}

	void free(String id) {
		synchronized (busy) {
			Session session = busy.get(id);
			if (null != session) {
				idle.addLast(session);
			}
		}
	}
}
