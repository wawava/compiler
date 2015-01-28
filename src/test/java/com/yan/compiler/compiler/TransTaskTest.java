package com.yan.compiler.compiler;

import static org.junit.Assert.*;

import java.nio.file.Paths;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.yan.compiler.compiler.TransTask.BackupTask;
import com.yan.compiler.config.Config;
import com.yan.compiler.config.DeployConfig;
import com.yan.compiler.config.Env;
import com.yan.compiler.receiver.Action;

public class TransTaskTest {

	Config config;

	@Before
	public void setUp() throws Exception {
		config = Config.factory(true);
	}

	@Test
	public void testBackup() {
		TransTask trans = new TransTask(990, Env.BETA, Action.PUSH);
		trans.run();
	}

}
