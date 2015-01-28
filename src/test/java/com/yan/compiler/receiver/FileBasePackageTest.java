package com.yan.compiler.receiver;

import static org.junit.Assert.*;

import org.junit.Test;
import org.mockito.Mockito;

public class FileBasePackageTest {

	@Test
	public void test() {
		final FileBasePackage pkg = Mockito.mock(FileBasePackage.class);
		Mockito.when(pkg.getTaskId()).thenReturn(990);
		Mockito.doCallRealMethod().when(pkg).chkPackage();

		assertEquals(pkg.chkPackage(), true);
	}

}
