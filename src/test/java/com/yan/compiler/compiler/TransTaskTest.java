package com.yan.compiler.compiler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonObject;
import com.yan.compiler.config.Config;
import com.yan.compiler.config.Env;
import com.yan.compiler.entities.PushLogEntity;
import com.yan.compiler.receiver.Action;

public class TransTaskTest {

	Config config;

	@Before
	public void setUp() throws Exception {
		config = Config.factory(true);
	}

	@Test
	public void testBackup() throws NoSuchFieldException, SecurityException,
			IllegalArgumentException, IllegalAccessException {
		// Class<TaskManagement> clazz = TaskManagement.class;
		// Field instance = clazz.getDeclaredField("instance");
		// instance.setAccessible(true);
		//
		// TaskManagement mockManage = Mockito.mock(TaskManagement.class);
		// Mockito.doAnswer(new Answer<Future<Boolean>>() {
		//
		// @Override
		// public Future<Boolean> answer(InvocationOnMock invocation)
		// throws Throwable {
		// Future<Boolean> future = new Future<Boolean>() {
		//
		// @Override
		// public boolean isDone() {
		// return true;
		// }
		//
		// @Override
		// public boolean isCancelled() {
		// return false;
		// }
		//
		// @Override
		// public Boolean get(long timeout, TimeUnit unit)
		// throws InterruptedException, ExecutionException,
		// TimeoutException {
		// return true;
		// }
		//
		// @Override
		// public Boolean get() throws InterruptedException,
		// ExecutionException {
		// return true;
		// }
		//
		// @Override
		// public boolean cancel(boolean mayInterruptIfRunning) {
		// return true;
		// }
		// };
		// return future;
		// }
		//
		// }).when(mockManage).submit(Mockito.any(Callable.class));
		//
		// instance.set(null, mockManage);

		// TransTask trans = new TransTask(990, Env.BETA, Action.PUSH);
		// trans.run();
	}

	@Test
	public void testLog() {
		TransTask task = new TransTask(990, Env.BETA, Action.PUSH, 6);
		Class<TransTask> clazz = TransTask.class;
		try {
			Method method = clazz.getDeclaredMethod("init");
			method.setAccessible(true);
			method.invoke(task);

			method = clazz.getDeclaredMethod("actionLog", Integer.class,
					JsonObject.class, Integer.class, Integer.class, Boolean.class);
			method.setAccessible(true);
			method.invoke(task, 1, new JsonObject(), 2,
					PushLogEntity.PUSH_SPECIES_BACKUP, true);
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
