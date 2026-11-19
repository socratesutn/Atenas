package ar.edu.utn.frc.atenas.seguimiento;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication(scanBasePackages = {
        "ar.edu.utn.frc.atenas.seguimiento",
        "ar.edu.utn.frc.atenas.commons"
})
public class SeguimientoApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(SeguimientoApplication.class, args);
    }
}
