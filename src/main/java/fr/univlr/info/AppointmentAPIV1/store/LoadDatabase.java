package fr.univlr.info.AppointmentAPIV1.store;

import fr.univlr.info.AppointmentAPIV1.model.Appointment;
import fr.univlr.info.AppointmentAPIV1.model.Doctor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.hateoas.config.HypermediaRestTemplateConfigurer;
import org.springframework.web.client.RestTemplate;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Configuration
@EnableHypermediaSupport(type = EnableHypermediaSupport.HypermediaType.HAL)

class LoadDatabase {

    private static final Logger log = LoggerFactory.getLogger(LoadDatabase.class);

    public static Date parseDate(String date) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm").parse(date);
        } catch (ParseException e) {
            return null;
        }
    }

    @Bean
    CommandLineRunner initDatabase(AppointmentRepository appointmentRepository,
                                   DoctorRepository docRepository) {
        return args -> {
            // V2 version : doctors mjones and jsmith must exist

              Doctor mjones = docRepository.save(new Doctor("mjones"));
              Doctor jsmith = docRepository.save(new Doctor("jsmith"));
              Doctor jdoe = docRepository.save(new Doctor("jdoe"));


            // uncomment to populate the database

            /*Appointment appt1 = new Appointment("mjones",
                    parseDate("2021-01-13T9:30"), parseDate("2021-01-13T10:15"), "jdoe");
            appointmentRepository.save(appt1);
            log.info("Preloading " + appt1);
            log.info("Preloading " + appointmentRepository.save(new Appointment("mjones",
                    parseDate("2021-01-25T14:00"), parseDate("2021-01-25T14:50"), "jsmith")));*/

        };
    }

    @Bean
    RestTemplate hypermediaRestTemplate(HypermediaRestTemplateConfigurer configurer) {
        return configurer.registerHypermediaTypes(new RestTemplate());
    }
}
