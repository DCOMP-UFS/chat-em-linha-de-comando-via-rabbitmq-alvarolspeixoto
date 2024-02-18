package br.ufs.dcomp.ChatRabbitMQ;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

public class Utils {
    
    public static String getFormattedDate() {
        String zone = "America/Sao_Paulo";
        ZonedDateTime dateTime = ZonedDateTime.now(java.time.ZoneId.of(zone));

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String date = dateTime.format(dateFormatter);

        return date;
    }

    public static String getFormattedTime() {
        String zone = "America/Sao_Paulo";
        ZonedDateTime dateTime = ZonedDateTime.now(java.time.ZoneId.of(zone));
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        String time = dateTime.format(timeFormatter);

        return time;

    }

    public static Properties getProps() throws IOException {
		Properties props = new Properties();
		FileInputStream file = new FileInputStream(
				"./properties/config.properties");
		props.load(file);
		return props;

	}
}
