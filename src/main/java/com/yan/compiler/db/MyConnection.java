package com.yan.compiler.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import com.yan.compiler.Log;

public class MyConnection {
	private static Integer idGen = 0;

	private Integer id;
	private String url;
	private String user;
	private String password;

	private Connection connect = null;

	private LinkedList<Session> idle;
	private Map<String, Session> busy;

	/**
	 * 
	 * @param id
	 * @param url
	 * @param user
	 * @param password
	 */
	private MyConnection(Integer id, String url, String user, String password) {
		this.id = id;
		this.url = url;
		this.user = user;
		this.password = password;

		idle = new LinkedList<Session>();
		busy = new HashMap<String, Session>();
	}

	/**
	 * @return the id
	 */
	public Integer getId() {
		return id;
	}

	/**
	 * Make a connection.
	 * 
	 * @param url
	 * @param user
	 * @param password
	 * @return
	 * @throws SQLException
	 */
	public static MyConnection getConnection(String url, String user,
			String password) throws SQLException {
		MyConnection conn;
		synchronized (idGen) {
			conn = new MyConnection(idGen++, url, user, password);
		}
		conn.setConnect();
		return conn;
	}

	/**
	 * @param autoCommit
	 * @throws SQLException
	 * @see {@link Connection#setAutoCommit(boolean)}
	 */
	public void setAutoCommit(Boolean autoCommit) throws SQLException {
		connect.setAutoCommit(autoCommit);
	}

	/**
	 * @return the connect
	 */
	public Connection getConnect() {
		return connect;
	}

	/**
	 * @param connect
	 *            the connect to set
	 * @throws SQLException
	 */
	private void setConnect() throws SQLException {
		Log.record(Log.DEBUG, getClass(),
				"Connect to database with Connection-String: " + url);
		this.connect = DriverManager.getConnection(url, user, password);
	}

	/**
	 * Connect and get a session.
	 * 
	 * @return
	 * @throws SQLException
	 */
	public Session connect() throws SQLException {
		if (null == connect || connect.isClosed()) {
			setConnect();
		}

		Session session;
		Log.record(Log.DEBUG, getClass(), String.format(
				"Lock connection: [%s] idle session queue.", getId()));

		synchronized (idGen) {
			if (idle.size() > 0) {
				session = idle.removeFirst();
				session.setStatement(connect.createStatement());
			} else {
				session = new Session(String.valueOf(idGen++), getId(),
						connect.createStatement());
				busy.put(session.getId(), session);
			}
		}
		Log.record(Log.DEBUG, getClass(), "Unlock idle session queue.");
		return session;
	}

	/**
	 * Free sessions
	 * 
	 * @param id
	 */
	void free(String id) {
		Log.record(Log.DEBUG, getClass(), "Free Session: " + id);
		synchronized (idGen) {
			Session session = busy.get(id);
			if (null != session) {
				idle.addLast(session);
			}
		}
	}

	public void freeAll() {
		synchronized (idGen) {
			Iterator<Entry<String, Session>> it = busy.entrySet().iterator();
			while (it.hasNext()) {
				Entry<String, Session> item = it.next();
				Session session = item.getValue();
				try {
					session.showdown();
				} catch (SQLException e) {
					Log.record(Log.ERR, getClass(), e);
				}
				idle.addLast(session);
				it.remove();
			}
		}
	}

	/**
	 * @see Connection#commit()
	 * @throws SQLException
	 */
	public void commit() throws SQLException {
		connect.commit();
	}

	/**
	 * @see Connection#rollback()
	 * @throws SQLException
	 */
	public void rollback() throws SQLException {
		connect.rollback();
	}

	/**
	 * close this connection.
	 */
	public void close() {
		Log.record(Log.DEBUG, getClass(), "Free Connection: " + getId());
		try {
			connect.rollback();
		} catch (SQLException e) {
			Log.record(Log.ERR, getClass(), e);
		}
		freeAll();
		try {
			connect.close();
		} catch (SQLException e) {
			Log.record(Log.ERR, getClass(), e);
		}
	}

	public boolean isBusy() {
		if (idle.size() > 0) {
			return false;
		}
		if (busy.size() > 10) {
			return true;
		}
		return false;
	}

	public int size() {
		if (busy.size() > 0) {
			return busy.size();
		}
		return 0;
	}
}
