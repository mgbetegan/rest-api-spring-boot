package fr.univlr.info.AppointmentAPIV1.store;
import fr.univlr.info.AppointmentAPIV1.model.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DoctorRepository extends JpaRepository<Doctor,Long> {
    Doctor findByName(String name);
}
