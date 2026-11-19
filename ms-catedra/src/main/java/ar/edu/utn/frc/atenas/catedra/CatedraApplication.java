package ar.edu.utn.frc.atenas.catedra;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication(scanBasePackages = {
        "ar.edu.utn.frc.atenas.catedra",
        "ar.edu.utn.frc.atenas.commons"
})
public class CatedraApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(CatedraApplication.class, args);
    }
}
