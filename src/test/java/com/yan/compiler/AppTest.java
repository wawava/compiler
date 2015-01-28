package com.yan.compiler;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

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
		try {
			BufferedReader br = new BufferedReader(new FileReader(
					Config.USER_DIR + "/config/server_config.json"));
			StringBuilder sbd = new StringBuilder();
			String str;
			while (null != (str = br.readLine())) {
				sbd.append(str);
			}
			br.close();
			String json = sbd.toString();

			JsonElement jElement = new JsonParser().parse(json);
			JsonObject jObject = jElement.getAsJsonObject();
			JsonObject cfg = jObject.getAsJsonObject("view/www");
			JsonObject val = cfg.getAsJsonObject("SERVER_BETA");

			Gson gson = new Gson();
			DeployConfig dc = gson.fromJson(val, DeployConfig.class);
			System.out.println(dc.host);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void testScanDir() {
		String path = "E:\\compile\\cache\\2b\\331b7dce72e5d85f8cecfcdbcb5c36";
		List<String> list = App.scanDir(path);
		assertEquals(list.size(), 7);
	}
}
