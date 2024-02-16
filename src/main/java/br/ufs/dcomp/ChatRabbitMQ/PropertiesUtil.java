package br.ufs.dcomp.ChatRabbitMQ;

import java.io.IOException;
import java.util.Properties;
import java.io.FileInputStream;

public class PropertiesUtil {

	public static Properties getProp() throws IOException {
		Properties props = new Properties();
		FileInputStream file = new FileInputStream(
				"./properties/config.properties");
		props.load(file);
		return props;

	}
}
