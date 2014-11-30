package com.yan.compiler;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.yan.compiler.compiler.CompilerTask;
import com.yan.compiler.config.Config;
import com.yan.compiler.db.ConnManagement;
import com.yan.compiler.db.Session;
import com.yan.compiler.receiver.BasePackage;
import com.yan.compiler.receiver.Receiver;

/**
 * Hello world!
 * 
 */
public class App {
	public static void main(String[] args) throws SQLException,
			NoSuchAlgorithmException, IOException {
		// Receiver r = Receiver.factory();
		// r.start();

		// BasePackage bp = new BasePackage();
		// bp.setEnv("bate");
		// bp.setProject("java");
		// bp.setName("test");
		// Gson gson = new Gson();
		// String str = gson.toJson(bp);
		// System.out.println(str);
		// Config config = Config.factory();
		// System.out.println(config.getAddr());
		// ConnManagement m = ConnManagement.factory();
		// Session s = m.connect();
		// try {
		// s.query("select * from po_posts limit 3");
		// LinkedList<Map<String, Object>> result = s.getResult();
		// s.close();
		// System.out.println(result);
		// } catch (SQLException e) {
		// e.printStackTrace();
		// }

		BasePackage bp = new BasePackage();
		bp.setProject("java_search_management");
		bp.setEnv("beta");
		bp.setName("test");
		bp.setId(536);
		
		CompilerTask task = new CompilerTask(bp);
		task.deployFile();
		task.compile();
		System.out.println(task);
	}
}
