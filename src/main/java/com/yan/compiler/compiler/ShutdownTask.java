package com.yan.compiler.compiler;

import com.yan.compiler.Log;
import com.yan.compiler.db.ConnManagement;
import com.yan.compiler.receiver.Receiver;

public class ShutdownTask extends Thread {

	private static Integer counter = 0;

	public ShutdownTask() {
		super("Shutdown Task - " + nextNum());

	}

	private static Integer nextNum() {
		synchronized (counter) {
			counter++;
			return counter;
		}
	}

	@Override
	public void run() {
		Log.record(Log.INFO, getClass(), "shutdown compiler task");
		CompilerManagement.factory().stop();
		Log.record(Log.INFO, getClass(), "shutdown Receiver and Deliver");
		Receiver.factory().stop();
		Log.record(Log.INFO, getClass(), "Close db connect");
		ConnManagement.factory().showdown();
	}
}
