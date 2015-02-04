package com.yan.compiler.entities;

import com.yan.compiler.config.Config;

public class PushEntity {
	public static final String TABLE_NAME = "svn_push";

	/**
	 * 流程ID
	 */
	public static final String COLUMN_ID = "id";

	/**
	 * 测试ID
	 */
	public static final String COLUMN_TID = "tid";

	/**
	 * 流程进度 0 已部署到开发机 1 已部署到测试机 2 已部署到beta机 3 已部署到preview机 4 已部署到正式环境
	 */
	public static final String COLUMN_PROCESS = "process";

	/**
	 * 流程状态 0 未执行 1 已执行 2 已回滚
	 */
	public static final String COLUMN_STATUS = "status";

	/**
	 * 生效 （失效后的版本号将在回滚中被跳过） 1 生效 0 失效
	 */
	public static final String COLUMN_OFFICIAL = "official";

	/**
	 * 生效版本号 用于回滚和记录
	 */
	public static final String COLUMN_VERSION = "version";

	/**
	 * 操作时间
	 */
	public static final String COLUMN_TIME = "time";

	// ----------------------------
	// 流程进度
	public static final Integer PROCESS_TO_SVN = 0;
	public static final Integer PROCESS_TO_DEV = 1;
	public static final Integer PROCESS_TO_TEST = -9;
	public static final Integer PROCESS_TO_ALPHA = 5;
	public static final Integer PROCESS_TO_BETA = 6;
	public static final Integer PROCESS_TO_PREVIEW = 8;
	public static final Integer PROCESS_TO_ONLINE = 10;
	public static final Integer PROCESS_TO_EMPTY = -10;
	// ----------------------------
	// 流程状态
	public static final Integer STATUS_NO_EXECUTE = 0;
	public static final Integer STATUS_ALREADY_EXECUTE = 1;
	public static final Integer STATUS_ALREADY_ROLLBACK = 2;
	public static final Integer STATUS_FINISHED = 3;
	public static final Integer STATUS_READY_TO_EXECUTE = 6;
	public static final Integer STATUS_RECEIVED = 7;
	public static final Integer STATUS_BACKUP = 8;
	public static final Integer STATUS_TRANS = 9;
	public static final Integer STATUS_SWITCH = 10;
	// ----------------------------
	// 生效状态
	public static final Integer OFFICIAL_ALREADY_OFFICIAL = 1;
	public static final Integer OFFICIAL_NO_OFFICIAL = 0;

	/**
	 * @param groupId
	 * @param status
	 * @return
	 */
	public static String updateStatus(Integer groupId, Integer status) {
		return String.format("update %s.%s set %s = %s where %s = %s", Config
				.factory().get("dbName"), TABLE_NAME, COLUMN_STATUS, status,
				COLUMN_TID, groupId);
	}
	
	
}
