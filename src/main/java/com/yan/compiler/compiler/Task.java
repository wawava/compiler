package com.yan.compiler.compiler;

public interface Task extends Runnable {
	/**
	 * @return The thread name.
	 */
	String getName();
}
