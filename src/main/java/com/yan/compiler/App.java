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
		Receiver r = Receiver.factory();
		r.start();
	}
}
