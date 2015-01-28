package com.yan.compiler.receiver;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;
import com.yan.compiler.config.Env;

public class PackageFactoryTest {

	String msg;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		Gson gson = new Gson();
		FileBasePackage fbp = new FileBasePackage();
		fbp.setAction(Action.PUSH);
		fbp.setEnv(Env.BETA);
		fbp.setGroupId(12);
		fbp.setProject("MAIN_SITE");

		String data = gson.toJson(fbp);

		PackageFactory pf = new PackageFactory();
		pf.setData(data);
		pf.setType(ProjectType.FILE);

		msg = gson.toJson(pf);
		System.out.println(msg);
	}

	@Test
	public void test() {
		AbstractBasePackage bp = PackageFactory.factory(msg);
		assertEquals(bp.getClass().getSimpleName(),
				FileBasePackage.class.getSimpleName());

	}

}
