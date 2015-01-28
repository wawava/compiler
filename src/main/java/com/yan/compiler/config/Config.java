package com.yan.compiler.config;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
	public static final String CFG_FILE_NAME = "config";
	/**
	 * 默认配置文件父目录名
	 */
	public static final String CFG_FILE_PARENT_DIR_NAME = "config";

	public static Config factory(boolean isDebug) {
		if (null == Config.obj) {
			try {
				Log.record(Log.DEBUG, Config.class, "factory Config");
				Config.obj = new Config(isDebug);
			} catch (Exception e) {
				Log.record(Log.ERR, Config.class.getName(), e);
				System.exit(0);
			}
		}
		return Config.obj;
	}

	/**
	 * 获取本类的引用。如果出现异常，则打印异常并直接退出。
	 * 
	 * @return
	 */
	public static Config factory() {
		return factory(false);
	}

	private MessageDigest md;

	private Map<String, String> cfg = new HashMap<String, String>();
	private String addr;
	private Integer port;
	private HashMap<String, String> projects;
	private String compileDir;
	private String cacheDir;
	private String backupDir;
	private Boolean sshTunnel = false;
	private JsonObject serverConfig;
	private JsonObject projectConfig;

	private String[] envp;

	private Boolean debug = false;

	private Gson gson = new Gson();

	/**
	 * Set {@code val} to the field {@code name}
	 * 
	 * @param name
	 *            The name of the field. Contain the char "." if wants to
	 *            indicate a key in a map.
	 * @param val
	 *            The value of the field or the value of a key in an map.
	 */
	@SuppressWarnings("unchecked")
	private void assign(String name, Object val) {
		String[] names = name.split("\\.");
		try {
			String _name = names[0];
			Field field = getClass().getDeclaredField(_name);
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

	/**
	 * A private constructor
	 * 
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 */
	private Config() throws IllegalArgumentException, IllegalAccessException,
			NoSuchAlgorithmException, IOException {
		this(false);
	}

	/**
	 * A private constructor
	 * 
	 * @param isDebug
	 *            true if working in debug mode.
	 * @throws IOException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws NoSuchAlgorithmException
	 */
	private Config(boolean isDebug) throws IOException,
			IllegalArgumentException, IllegalAccessException,
			NoSuchAlgorithmException {
		debug = isDebug;
		assignField();
		initJvmEnv();
		initServerConfig();
		initProjectConfig();
	}

	/**
	 * Read the config file and assign to config instance.
	 * 
	 * @throws IOException
	 */
	private void assignField() throws IOException {
		Iterator<Entry<Object, Object>> it = getProperties().entrySet()
				.iterator();
		while (it.hasNext()) {
			Entry<Object, Object> entry = it.next();
			String name = (String) entry.getKey();
			Object val = entry.getValue();
			Log.record(
					Log.DEBUG,
					Config.class,
					String.format("Read config: [%s = %s]", name,
							val.toString()));
			assign(name, val);
		}
	}

	private void initServerConfig() {
		Path configPath = Paths.get(USER_DIR, CFG_FILE_PARENT_DIR_NAME,
				"server_config.json");
		serverConfig = readJson(configPath);
	}

	private void initProjectConfig() {
		Path configPath = Paths.get(USER_DIR, CFG_FILE_PARENT_DIR_NAME,
				"project_config.json");
		projectConfig = readJson(configPath);
	}

	private JsonObject readJson(Path path) {
		try {
			BufferedReader br = new BufferedReader(
					new FileReader(path.toFile()));
			StringBuilder sbd = new StringBuilder();
			String str;
			while (null != (str = br.readLine())) {
				sbd.append(str);
			}
			br.close();
			String json = sbd.toString();

			JsonElement jElement = new JsonParser().parse(json);
			return jElement.getAsJsonObject();
		} catch (FileNotFoundException e) {
			Log.record(Log.ERR, getClass(), e);
		} catch (IOException e) {
			Log.record(Log.ERR, getClass(), e);
		}
		return null;
	}

	/**
	 * Read config field in a {@link Properties} instance.
	 * 
	 * @return A Properties instance contain config.
	 * @throws IOException
	 */
	private Properties getProperties() throws IOException {
		String fileName;
		if (debug) {
			fileName = CFG_FILE_NAME + ".debug.properties";
		} else {
			fileName = CFG_FILE_NAME + ".properties";
		}
		Path configPath = Paths.get(USER_DIR, CFG_FILE_PARENT_DIR_NAME,
				fileName);
		configPath = configPath.toAbsolutePath();
		File file = configPath.toFile();
		Log.record(Log.DEBUG, Config.class,
				"Read config from file: " + file.getPath());

		Properties prop = new Properties();
		InputStream in = new BufferedInputStream(new FileInputStream(file));
		prop.load(in);
		return prop;
	}

	/**
	 * Read the env param
	 * 
	 * @throws NoSuchAlgorithmException
	 */
	private void initJvmEnv() throws NoSuchAlgorithmException {
		md = MessageDigest.getInstance("MD5");

		Map<String, String> env = System.getenv();
		String jdkHome = env.get("JDK_HOME");
		List<String> envList = new LinkedList<String>();
		for (Entry<String, String> entry : env.entrySet()) {
			String key = entry.getKey();
			String val;
			if (key.equals("JAVA_HOME") && null != jdkHome) {
				val = jdkHome;
			} else {
				val = entry.getValue();
			}
			String _env = String.format("%s=%s", key, val);
			Log.record(Log.DEBUG, getClass(), "Get env: " + _env);
			envList.add(_env);
		}
		envp = envList.toArray(new String[0]);
	}

	/**
	 * Get Compile directive.
	 * 
	 * @param project
	 * @param env
	 * @return
	 */
	public String getCompileDir(String project, String env) {
		project = projects.get(project);
		Path path = Paths.get(compileDir, project, env);
		return path.toAbsolutePath().normalize().toString();
	}

	/**
	 * Get backup directive.
	 * 
	 * @param name
	 * @param env
	 * @return
	 */
	public String getBackupDir(String name, String env) {
		StringBuilder sbd = md5(name);
		Path path = Paths.get(backupDir, env, sbd.substring(0, 2),
				sbd.substring(2));
		return path.toAbsolutePath().normalize().toString();
	}

	/**
	 * Get backup directive.
	 * 
	 * @param id
	 * @param env
	 * @return
	 */
	public String getBackupDir(Integer id, Env env) {
		return getBackupDir(String.valueOf(id), env.toString());
	}

	/**
	 * Get cache directive.
	 * 
	 * @param name
	 * @return
	 */
	public String getCacheDir(String name) {
		StringBuilder sbd = md5(name);
		Path path = Paths.get(cacheDir, sbd.substring(0, 2), sbd.substring(2));
		return path.toAbsolutePath().normalize().toString();
	}

	/**
	 * Get cache directive.
	 * 
	 * @param id
	 * @return
	 */
	public String getCacheDir(Integer id) {
		return getCacheDir(String.valueOf(id));
	}

	/**
	 * Get a template dir.
	 * 
	 * @param id
	 * @return
	 */
	public Path getBackupTplPath(Integer id) {
		String prefix = md5(id.toString()).toString();
		try {
			Path tmp = Files.createTempDirectory(prefix);
			return tmp.toAbsolutePath().normalize();
		} catch (IOException e) {
			Log.record(Log.ERR, getClass(), e);
			return null;
		}
	}

	/**
	 * 
	 */
	public StringBuilder md5(String name) {
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

	/**
	 * return whether using ssh tunnel.
	 * 
	 * @return
	 */
	public Boolean usingSSHTunnel() {
		return sshTunnel;
	}

	public Boolean isDebug() {
		return debug;
	}

	public String[] getEnvp() {
		return envp;
	}

	/**
	 * 
	 * @param project
	 * @param env
	 * @return
	 */
	public DeployConfig getDeployConfig(String project, Env env) {
		JsonObject cfg = serverConfig.getAsJsonObject(project);
		JsonObject val = cfg.getAsJsonObject(env.toString());

		DeployConfig dc = gson.fromJson(val, DeployConfig.class);
		return dc;
	}

	/**
	 * 
	 * @param project
	 * @return
	 */
	public String getProjectConfig(String project) {
		JsonObject cfg = projectConfig.getAsJsonObject(project);
		return cfg.toString();
	}
}
