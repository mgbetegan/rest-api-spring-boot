package fr.univlr.info.AppointmentAPIV1.store;

import fr.univlr.info.AppointmentAPIV1.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.Date;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment,Long> {
    @Query("SELECT appointment from Appointment appointment WHERE  (:startDate <= appointment.endDate  and :startDate >= appointment.startDate) or (:endDate >= appointment.startDate  and :endDate <= appointment.endDate) ")
    List<Appointment> findAppointmentByDate(Date startDate, Date endDate);
    List<Appointment> findByDoctorContains(@PathVariable("name") String name);
    @Query("SELECT a from Appointment a where a.startDate > :date ")
    List<Appointment> retrieveAppointmentsByDate(Date date);
   }
