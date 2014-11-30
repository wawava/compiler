package com.yan.compiler.config;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import com.yan.compiler.Log;

public class Config {
	/**
	 * 系统默认换行符
	 */
	public static final String LINE_SEPARATOR = System
			.getProperty("line.separator");
	/**
	 * 单例类实例引用。
	 */
	private static Config obj = null;
	/**
	 * 程序根目录
	 */
	public static final String USER_DIR = System.getProperty("user.dir");
	/**
	 * 默认配置文件名
	 */
	public static final String CFG_FILE_NAME = "config.properties";
	/**
	 * 默认配置文件父目录名
	 */
	public static final String CFG_FILE_PARENT_DIR_NAME = "config";
	/**
	 * 默认的配置文件路径，默认值为 <b>运行目录/config/config.xml</b>
	 */
	public static String CFG_FILE_PATH = null;
	static {
		StringBuilder sbd = new StringBuilder(50);
		sbd.append(USER_DIR);
		sbd.append(File.separator);
		sbd.append(CFG_FILE_PARENT_DIR_NAME);
		sbd.append(File.separator);
		sbd.append(CFG_FILE_NAME);
		CFG_FILE_PATH = sbd.toString();
	};

	/**
	 * 获取本类的引用。如果出现异常，则打印异常并直接退出。
	 * 
	 * @return
	 */
	public static Config factory() {
		if (null == Config.obj) {
			try {
				Config.obj = new Config();
			} catch (Exception e) {
				Log.record(Log.ERR, Config.class.getName(), e);
				System.exit(0);
			}
		}
		return Config.obj;
	}

	private MessageDigest md;

	private Map<String, String> cfg = new HashMap<String, String>();
	private String addr;
	private Integer port;
	private HashMap<String, String> projects;
	private String compileDir;
	private String cacheDir;
	private String backupDir;

	@SuppressWarnings("unchecked")
	private void assign(Class<? extends Config> clazz, String name, Object val) {
		String[] names = name.split("\\.");
		try {
			String _name = names[0];
			Field field = clazz.getDeclaredField(_name);
			Class<?> type = field.getType();
			try {
				String value;
				if (names.length == 2) {
					Map<String, String> map = (Map<String, String>) field
							.get(this);
					if (null == map) {
						Object obj = type.newInstance();
						field.set(this, obj);
						map = (Map<String, String>) obj;
					}
					String key = names[1];
					map.put(key, val.toString());
				} else {
					if (!type.getSimpleName().equals("String")) {
						value = String.valueOf(val);
						Method valueOf = type.getDeclaredMethod("valueOf",
								String.class);
						field.set(this, valueOf.invoke(null, value));
					} else {
						value = (String) val;
						field.set(this, value);
					}
				}

			} catch (Exception e) {
				Log.record(Log.ERR, Config.class.getName(), e);
			}
		} catch (Exception e) {
			cfg.put(name, val.toString());
		}
	}

	private Config() throws IOException, IllegalArgumentException,
			IllegalAccessException, NoSuchAlgorithmException {
		File file = new File(CFG_FILE_PATH);
		Properties prop = new Properties();
		InputStream in = new BufferedInputStream(new FileInputStream(file));
		prop.load(in);
		Class<? extends Config> clazz = getClass();

		Iterator<Entry<Object, Object>> it = prop.entrySet().iterator();
		while (it.hasNext()) {
			Entry<Object, Object> entry = it.next();
			String name = (String) entry.getKey();
			Object val = entry.getValue();
			assign(clazz, name, val);
		}

		md = MessageDigest.getInstance("MD5");
	}

	public String getCompileDir(String project, String env) {
		project = projects.get(project);
		Path path = Paths.get(compileDir, project, env);
		return path.toAbsolutePath().normalize().toString();
	}

	public String getBackupDir(String name, String env) {
		StringBuilder sbd = md5(name);
		Path path = Paths.get(backupDir, env, sbd.substring(0, 2),
				sbd.substring(2));
		return path.toAbsolutePath().normalize().toString();
	}

	public String getCacheDir(String name) {
		StringBuilder sbd = md5(name);
		Path path = Paths.get(cacheDir, sbd.substring(0, 2), sbd.substring(2));
		return path.toAbsolutePath().normalize().toString();
	}

	private StringBuilder md5(String name) {
		md.update(name.getBytes());
		byte[] digest = md.digest();
		StringBuilder sb = new StringBuilder();
		for (byte b : digest) {
			sb.append(String.format("%02x", b & 0xff));
		}
		return sb;
	}

	/**
	 * @return the addr
	 */
	public String getAddr() {
		return addr;
	}

	/**
	 * @param addr
	 *            the addr to set
	 */
	public void setAddr(String addr) {
		this.addr = addr;
	}

	/**
	 * @return the port
	 */
	public Integer getPort() {
		return port;
	}

	/**
	 * @param port
	 *            the port to set
	 */
	public void setPort(Integer port) {
		this.port = port;
	}

	/**
	 * @return the projects
	 */
	public HashMap<String, String> getProjects() {
		return projects;
	}

	/**
	 * @param projects
	 *            the projects to set
	 */
	public void setProjects(HashMap<String, String> projects) {
		this.projects = projects;
	}

	public String get(String key) {
		return cfg.get(key);
	}
}
