package com.yan.compiler.config;

import static org.junit.Assert.*;

import java.nio.file.Path;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ConfigTest {

	private Config config;

	private Integer id;

	@Before
	public void setUp() throws Exception {
		config = Config.factory(true);
		id = 12;
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testGetBackupDir() {
		String targetBetaDir = "E:\\home\\webdata\\htdocs\\publish\\backup\\beta\\c2\\0ad4d76fe97759aa27a0c99bff6710";
		String dir = config.getBackupDir(id, Env.BETA);
		assertEquals(targetBetaDir, dir);

		String targetPreviewDir = "E:\\home\\webdata\\htdocs\\publish\\backup\\preview\\c2\\0ad4d76fe97759aa27a0c99bff6710";
		dir = config.getBackupDir(id, Env.PREVIEW);
		assertEquals(targetPreviewDir, dir);
		assertNotEquals(targetBetaDir, dir);

		String targetOnlineDir = "E:\\home\\webdata\\htdocs\\publish\\backup\\online\\c2\\0ad4d76fe97759aa27a0c99bff6710";
		dir = config.getBackupDir(id, Env.ONLINE);
		assertEquals(targetOnlineDir, dir);
	}

	@Test
	public void testGetCacheDir() {
		String cacheDir = "E:\\home\\webdata\\htdocs\\publish\\cache\\c2\\0ad4d76fe97759aa27a0c99bff6710";
		String dir = config.getCacheDir(id);
		assertEquals(cacheDir, dir);
	}

	@Test
	public void testGetBackupTplPath() {
		Integer id = 990;
		Path path = config.getBackupTplPath(id);
		System.out.println(path.toString());
		System.out.println(path.getFileName().toString());
		assertNotEquals(path, null);
	}

	@Test
	public void testGetDeployConfig() {
		DeployConfig myCfg = new DeployConfig();
		myCfg.host = "10.202.241.19";
		myCfg.path = "/home/webdata/htdocs/www";
		myCfg.tmp = "/tmp/%s/htdocs/www";

		DeployConfig cfg = config.getDeployConfig("view/www", Env.BETA);
		assertEquals(myCfg.path, cfg.path);
	}
}
