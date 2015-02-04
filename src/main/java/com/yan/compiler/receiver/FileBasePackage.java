package com.yan.compiler.receiver;

import com.yan.compiler.compiler.Task;
import com.yan.compiler.compiler.TaskManagement;
import com.yan.compiler.compiler.TransTask;
import com.yan.compiler.config.Env;

public class FileBasePackage extends AbstractBasePackage {
	private Action action;
	private Integer groupId;
	private Env env;
	private Integer uid;

	public Action getAction() {
		return action;
	}

	public void setAction(Action action) {
		this.action = action;
	}

	public Integer getGroupId() {
		return groupId;
	}

	public void setGroupId(Integer groupId) {
		this.groupId = groupId;
	}

	public Env getEnv() {
		return env;
	}

	public void setEnv(Env env) {
		this.env = env;
	}

	@Override
	public Integer getTaskId() {
		return groupId;
	}

	@Override
	public boolean chkPackage() {
		Integer id = getTaskId();
		TaskManagement manage = TaskManagement.factory();
		return !manage.contains(id);
	}

	@Override
	public void deliver() {
		// create a task
		TaskManagement mangage = TaskManagement.factory();
		Task task = mangage.createTask(this);
		// execute the task
		mangage.execute(task);
	}

	@Override
	public Task createTask() {
		Task task = new TransTask(getGroupId(), getEnv(), getAction(), getUid());
		return task;
	}

	/**
	 * @return the uid
	 */
	public Integer getUid() {
		return uid;
	}

	/**
	 * @param uid
	 *            the uid to set
	 */
	public void setUid(Integer uid) {
		this.uid = uid;
	}
}
