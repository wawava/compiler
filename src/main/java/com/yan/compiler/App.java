package com.yan.compiler;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import com.yan.compiler.db.ConnManagement;
import com.yan.compiler.db.Session;
import com.yan.compiler.receiver.Receiver;

/**
 * Hello world!
 * 
 */
public class App {
	public static void main(String[] args) throws SQLException,
			NoSuchAlgorithmException, IOException {
		Log.init();
		Receiver r = Receiver.factory();
		r.start();
		// Session session = ConnManagement.factory().connect();
		// session.query("Select * from svn_test_group order by id desc limit 3");
		// LinkedList<Map<String, Object>> result = session.getResult();
		// Iterator<Map<String, Object>> it = result.iterator();
		// while (it.hasNext()) {
		// Map<String, Object> map = it.next();
		// System.out.println(map);
		// }
	}
}
