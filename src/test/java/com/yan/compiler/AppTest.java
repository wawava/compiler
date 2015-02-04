package com.yan.compiler;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.yan.compiler.config.Config;
import com.yan.compiler.config.DeployConfig;

/**
 * Unit test for simple App.
 */
public class AppTest {

	@Test
	public void testProjectConfig() {
		// try {
		// BufferedReader br = new BufferedReader(new FileReader(
		// Config.USER_DIR + "/config/server_config.json"));
		// StringBuilder sbd = new StringBuilder();
		// String str;
		// while (null != (str = br.readLine())) {
		// sbd.append(str);
		// }
		// br.close();
		// String json = sbd.toString();
		//
		// JsonElement jElement = new JsonParser().parse(json);
		// JsonObject jObject = jElement.getAsJsonObject();
		// JsonObject cfg = jObject.getAsJsonObject("view/www");
		// JsonObject val = cfg.getAsJsonObject("beta");
		//
		// Gson gson = new Gson();
		// DeployConfig dc = gson.fromJson(val, DeployConfig.class);
		// System.out.println(dc.host);
		// } catch (FileNotFoundException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// } catch (IOException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
	}

	@Test
	public void testScanDir() {
		// String path =
		// "E:\\compile\\cache\\2b\\331b7dce72e5d85f8cecfcdbcb5c36";
		// List<String> list = App.scanDir(path);
		// assertEquals(list.size(), 7);
		String str = "3aefb729b6fa96f5f24440d44dd66126  /home/webdata/lang/view/VU/zh-tw/pdalogin/index_lang_tmp.php";
		String[] arr = str.split("\\s+");
		System.out.println(arr.length);

		String str1 = "/home/webdata/view/shop";
		Path path1 = Paths.get(str1);
		String str2 = "/home/webdata/view/member";
		Path path2 = Paths.get(str2);

		System.out.println(path1.);
	}

	@Test
	public void testDiff() throws IOException {
		Config config = Config.factory();
		Path file = Paths.get(Config.USER_DIR, Config.CFG_FILE_PARENT_DIR_NAME,
				"diff_result");
		BufferedReader br = new BufferedReader(new FileReader(file.toFile()));
		String str = null;
		Pattern pattern = Pattern.compile(
				"[\\d\\.]+:(.*)\\s+and\\s+(.*)\\s+differ",
				Pattern.CASE_INSENSITIVE);
		while (null != (str = br.readLine())) {
			Matcher matcher = pattern.matcher(str);
			if (matcher.find()) {
				System.out.println(matcher.group(0));
				System.out.println(matcher.group(1));
				System.out.println(matcher.group(2));
			}
		}
	}
}
