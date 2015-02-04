package com.yan.compiler.entities;

import java.util.Date;

import com.google.gson.JsonObject;
import com.yan.compiler.config.Config;

public class PushLogEntity {
	public static final String COLUMN_INFO_PREV = "prev";
	public static final String COLUMN_INFO_NEXT = "next";
	public static final String COLUMN_INFO_RELATIVE_FILES = "relativeFiles";
	public static final String COLUMN_INFO_LAST_TIME = "lastTime";
	public static final String COLUMN_INFO_FILE_COUNT = "count";
	public static final String COLUMN_INFO_NEW_FILE = "newFile";
	public static final String COLUMN_INFO_IS_SUCCESS = "success";

	/**
	 * 发布
	 */
	public static final Integer PUSH_SPECIES_PUBLISH = 1;
	/**
	 * 回滚
	 */
	public static final Integer PUSH_SPECIES_ROLLBACK = 2;
	/**
	 * 重新提测
	 */
	public static final Integer PUSH_SPECIES_RETEST = 3;
	/**
	 * 添加文件
	 */
	public static final Integer PUSH_SPECIES_ADD_FILE = 4;
	/**
	 * 关闭提测
	 */
	public static final Integer PUSH_SPECIES_CLOSE = 5;
	/**
	 * 重新打开提测
	 */
	public static final Integer PUSH_SPECIES_REOPEN = 6;
	/**
	 * 创建提测
	 */
	public static final Integer PUSH_SPECIES_CREATE_GROUP = 7;
	/**
	 * 打开测试
	 */
	public static final Integer PUSH_SPECIES_OPEN_TEST = 8;
	/**
	 * 强制回滚
	 */
	public static final Integer PUSH_SPECIES_FORCE_RESET = 9;
	/**
	 * 更改处理人
	 */
	public static final Integer PUSH_SPECIES_CHANGE_OPERATOR = 10;
	/**
	 * 备份
	 */
	public static final Integer PUSH_SPECIES_BACKUP = 11;
	/**
	 * 部署
	 */
	public static final Integer PUSH_SPECIES_DEPLOY = 12;
	/**
	 * 传输
	 */
	public static final Integer PUSH_SPECIES_TRANS = 13;
	/**
	 * 删除文件
	 */
	public static final Integer PUSH_SPECIES_DEL_FILE = 14;

	/**
	 * 表名
	 */
	public static final String TABLE_NAME = "svn_push_log";

	/**
	 *
	 */
	public static final String COLUMN_ID = "id";

	/**
	 * 关联的测试ID
	 */
	public static final String COLUMN_TID = "tid";

	/**
	 * 执行操作的用户的ID
	 */
	public static final String COLUMN_UID = "uid";

	/**
	 * 执行操作的时间戳
	 */
	public static final String COLUMN_TIME = "time";

	/**
	 * 1：发布，2：回滚
	 */
	public static final String COLUMN_TYPE = "type";

	/**
	 * 执行此次操作时测试单记录的版本号
	 */
	public static final String COLUMN_REVERSION = "reversion";

	/**
	 * json格式存储的发布日志信息，一般会有prev: 发布前状态，next:发布后状态，relativeFiles:涉及的文件列表
	 */
	public static final String COLUMN_INFO = "info";

	public static JsonObject mkInfo(Integer prev, Integer next, Integer last,
			Integer fileCount, JsonObject files, Boolean success) {
		JsonObject info = new JsonObject();
		info.addProperty(COLUMN_INFO_FILE_COUNT, fileCount);
		info.addProperty(COLUMN_INFO_PREV, prev);
		info.addProperty(COLUMN_INFO_NEXT, next);
		info.addProperty(COLUMN_INFO_LAST_TIME, last);
		info.add(COLUMN_INFO_RELATIVE_FILES, files);
		info.addProperty(COLUMN_INFO_IS_SUCCESS, success);

		return info;
	}

	/**
	 * 获得日志sql
	 * 
	 * @param type
	 *            部署类型
	 * @param tid
	 *            日志指向的测试单号
	 * @param uid
	 *            操作用户ID
	 * @param version
	 *            记录日志时test所在版本号
	 * @param info
	 *            要记录的日志信息
	 * @return sql
	 */
	public static String mkInsertSql(Integer type, Integer tid, Integer uid,
			Integer version, JsonObject info) {
		String sqlTpl = "INSERT INTO %s.%s (%s, %s, %s, %s, %s, %s) VALUES (%s, %s, %s, %s, %s, '%s')";

		String sql = String.format(sqlTpl, Config.factory().get("dbName"),
				TABLE_NAME, COLUMN_TYPE, COLUMN_TIME, COLUMN_REVERSION,
				COLUMN_TID, COLUMN_UID, COLUMN_INFO, type,
				Integer.valueOf((int) ((new Date()).getTime() / 1000)),
				version, tid, uid, info.toString());
		return sql;
	}

}
