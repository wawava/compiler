package com.yan.compiler.compiler;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.yan.compiler.config.Env;
import com.yan.compiler.receiver.Action;
import com.yan.compiler.receiver.FileBasePackage;

public class TaskManagementTest {

	TaskManagement manage;

	@Before
	public void setUp() throws Exception {
		manage = TaskManagement.factory();
	}

	@Test
	public void testCreateTask() {
//		FileBasePackage pkg = new FileBasePackage();
//		pkg.setAction(Action.PUSH);
//		pkg.setEnv(Env.BETA);
//		pkg.setGroupId(990);
//		pkg.setProject("MAIN_SITE");
//
//		pkg.deliver();
//
//		assertEquals(manage.contains(pkg.getGroupId()), true);
//		try {
//			Thread.sleep(3000);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		assertEquals(manage.contains(pkg.getGroupId()), false);
	}

}
