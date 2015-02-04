package com.yan.compiler.entities;

import java.util.Date;

import com.yan.compiler.config.Config;

public class TestGroupEntity {
	public static final String TABLE_NAME = "svn_test_group";

	/**
	 * 提测id
	 */
	public static final String COLUMN_ID = "id";

	/**
	 * 提测名称
	 */
	public static final String COLUMN_NAME = "name";

	/**
	 * 发起提测的uid
	 */
	public static final String COLUMN_UID = "uid";

	/**
	 * 下一步处理人
	 */
	public static final String COLUMN_NEXT = "next";

	/**
	 * 发起提测的时间
	 */
	public static final String COLUMN_TIME = "time";

	/**
	 * 提测状态 0 等待提测 1 已提测 2 已发布 3 已关闭
	 */
	public static final String COLUMN_STATUS = "status";

	/**
	 * 发起提测时的版本库最新版本号
	 */
	public static final String COLUMN_VERSION = "version";

	/**
	 * 测试单冲突计数
	 */
	public static final String COLUMN_CONFLICT = "conflict";

	/**
	 * branch表的外键
	 */
	public static final String COLUMN_BRANCH_ID = "branch_id";

	/**
	 * 所属分支名
	 */
	public static final String COLUMN_BRANCH_NAME = "branch_name";

	/**
	 * 主项目
	 */
	public static final String COLUMN_PROJECT = "project";
	/**
	 * 最后修改时间
	 */
	public static final String COLUMN_LAST_MODIFY_TIME = "last_modify_time";
	/**
	 * 最后修改用户
	 */
	public static final String COLUMN_LAST_MODIFY_USER = "last_modify_user";
	/**
	 * 最后修改类型
	 */
	public static final String COLUMN_LAST_MODIFY_TYPE = "last_modify_type";

	// --------------------------
	// 提测状态
	public static final Integer STATUS_PRE_TEST = 0; // 等待提测
	public static final Integer STATUS_TESTING = 1; // 已提测
	public static final Integer STATUS_RELEASE = -2; // 已发布
	public static final Integer STATUS_CLOSED = -3; // 已关闭
	public static final Integer STATUS_CONFLICT_CLOSE = -1; // 冲突关闭

	// -----------------------------
	// 部署状态
	public static final Integer PUSH_STATUS_NORMAL = 0; // 0正常
	public static final Integer PUSH_STATUS_ROLLBACK = 1; // 1已被回滚到初始（提示重新提测）
	public static final Integer PUSH_STATUS_TRUNK = 2; // 2发布流程因正式环境出bug而终止（提示覆盖提测）
	public static final Integer PUSH_STATUS_RELEASE = 3; // 已部署到正式环境

	/**
	 * 制作更新sql
	 * 
	 * @param type
	 * @param uid
	 * @param tid
	 * @return
	 */
	public static String mkUpdateSql(Integer type, Integer uid, Integer tid) {
		String sqlTpl = "UPDATE %s.%s SET %s = %s, %s = %s, %s = %s WHERE %s = %s";
		String sql = String.format(sqlTpl, Config.factory().get("dbName"),
				TABLE_NAME, COLUMN_LAST_MODIFY_TIME,
				Integer.valueOf((int) ((new Date()).getTime() / 1000)),
				COLUMN_LAST_MODIFY_TYPE, type, COLUMN_LAST_MODIFY_USER, uid,
				COLUMN_ID, tid);
		return sql;
	}
}
