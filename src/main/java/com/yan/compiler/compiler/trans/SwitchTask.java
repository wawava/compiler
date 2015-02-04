package com.yan.compiler.compiler.trans;

import java.util.concurrent.CountDownLatch;

import com.yan.compiler.compiler.ModuleInfo;

public class SwitchTask extends AbstractTransTask {

	public SwitchTask(CountDownLatch doneSignal, CountDownLatch startSignal,
			ModuleInfo moduleInfo) {
		super(doneSignal, startSignal, moduleInfo);
	}

	@Override
	public TaskResult call() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

}
