package ar.edu.utn.frc.atenas.documentacion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication(scanBasePackages = {
        "ar.edu.utn.frc.atenas.documentacion",
        "ar.edu.utn.frc.atenas.commons"
})
public class DocumentacionApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(DocumentacionApplication.class, args);
    }
}
