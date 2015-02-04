package com.yan.compiler.db;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public class ConnManagementTest {
	ConnManagement manage;

	@Before
	public void setUp() throws Exception {
		manage = ConnManagement.factory();
	}

	@Test
	public void testConnect() {
		try {
			Session session = manage.connect();
			Class<ConnManagement> clazz = ConnManagement.class;
			Field field = clazz.getDeclaredField("normal");
			field.setAccessible(true);
			@SuppressWarnings("unchecked")
			LinkedList<MyConnection> normal = (LinkedList<MyConnection>) field
					.get(manage);
			assertEquals(normal.size(), 1);

			session.query("select * from svn_test_group where id = 990");
			Map<String, Object> result = session.find();
			Integer id = Integer.valueOf(String.valueOf(result.get("id")));
			assertEquals(id, Integer.valueOf(990));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testFree() {
		// fail("Not yet implemented");
	}

	@Test
	public void testShowdown() {
		// fail("Not yet implemented");
	}

	@Test
	public void testGetConnection() {
		// fail("Not yet implemented");
	}

}
