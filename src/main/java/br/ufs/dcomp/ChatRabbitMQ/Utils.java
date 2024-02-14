package br.ufs.dcomp.ChatRabbitMQ;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

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
}
