package com.yan.compiler.receiver;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.yan.compiler.compiler.TaskManagement;

public class FileBasePackageTest {

	@Before
	public void setUp() {
		TaskManagement.factory().clear();
	}

	@Test
	public void test() {
		final FileBasePackage pkg = Mockito.mock(FileBasePackage.class);
		Mockito.when(pkg.getTaskId()).thenReturn(990);
		Mockito.doCallRealMethod().when(pkg).chkPackage();

		assertEquals(pkg.chkPackage(), true);
	}

}
