package com.yan.compiler;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
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
