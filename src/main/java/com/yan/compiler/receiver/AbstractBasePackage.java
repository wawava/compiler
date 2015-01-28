package com.yan.compiler.receiver;

import com.yan.compiler.compiler.Task;

public abstract class AbstractBasePackage {

	protected String project;

	/**
	 * Check if this package can be deliver to working queue.
	 * 
	 * @return
	 */
	public abstract boolean chkPackage();

	/**
	 * Deliver this package to the specific queue.
	 */
	public abstract void deliver();

	public Integer getTaskId() {
		return 0;
	}

	/**
	 * @return the project
	 */
	public String getProject() {
		return project;
	}

	/**
	 * @param project
	 *            the project to set
	 */
	public void setProject(String project) {
		this.project = project;
	}

	public Task createTask() {
		return new Task() {

			@Override
			public void run() {
			}

			@Override
			public String getName() {
				return "Untitle Task";
			}
		};
	}
}