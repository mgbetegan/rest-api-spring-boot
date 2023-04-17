package fr.univlr.info.AppointmentAPIV1.controller;

import fr.univlr.info.AppointmentAPIV1.model.Appointment;
import fr.univlr.info.AppointmentAPIV1.model.Doctor;
import fr.univlr.info.AppointmentAPIV1.store.AppointmentRepository;
import fr.univlr.info.AppointmentAPIV1.store.DoctorRepository;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = "/api")
public class DoctorController {
    private final DoctorRepository doctorRepository;
    private final DoctorModelAssembler doctorAssembler;
    private final AppointmentRepository  appointmentRepository;
    private final AppointmentModelAssembler appointmentAssembler;
    public DoctorController(DoctorRepository doctorRepository, DoctorModelAssembler doctorAssembler,AppointmentRepository appointmentRepository,AppointmentModelAssembler appointmentAssembler) {
        this.doctorRepository = doctorRepository;
        this.doctorAssembler = doctorAssembler;
        this.appointmentRepository = appointmentRepository;
        this.appointmentAssembler = appointmentAssembler;
    }

    @GetMapping("/doctors")
    ResponseEntity<?> all(@RequestHeader(value="Accept", required=false) String halContent) {
        List<Doctor> doctors = doctorRepository.findAll();
        if(halContent !=null && MediaTypes.HAL_JSON_VALUE.equals(halContent)) {
            List<EntityModel<Doctor>> doctorsWithHallTemplate = this.doctorRepository.findAll().stream().map(doctorAssembler::toModel).collect(Collectors.toList());
            return ResponseEntity.ok(CollectionModel.of(doctorsWithHallTemplate));
        }
        return new ResponseEntity<>(doctors, HttpStatus.OK);
    }
    @GetMapping("/doctors/{name}")
    ResponseEntity<?> getDoctor(@RequestHeader(value="Accept", required=false) String halContent,@PathVariable String name) {
        Doctor doctor = doctorRepository.findByName(name);
        if (doctor==null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        if(halContent !=null && MediaTypes.HAL_JSON_VALUE.equals(halContent)){
            return ResponseEntity.ok(this.doctorAssembler.toModel(doctor));
        }
        return ResponseEntity.ok(doctor);
    }
    @GetMapping("/doctors/{name}/appointments")
    ResponseEntity<?> getAppointments(@RequestHeader(value="Accept", required=false) String halContent, @PathVariable String name) {
        List<Appointment> appointments= appointmentRepository.findByDoctorContains(name);
        if(halContent !=null && MediaTypes.HAL_JSON_VALUE.equals(halContent)){
            List<EntityModel<Appointment>> doctorAppointmentsWithHallTemplate = appointments.stream().map(appointmentAssembler::toModel2).collect(Collectors.toList());

            //appointments.stream().map(appointmentAssembler::toModel2).collect(Collectors.toList());
            /*return ResponseEntity.ok(CollectionModel.of(doctorAppointmentsWithHallTemplate,
                    linkTo(DoctorController.class).slash("doctor/").withRel("appointments") ));*/
        //}
            System.out.println("--------------hello hello--------------- ");
            System.out.println(CollectionModel.of(doctorAppointmentsWithHallTemplate).getContent().size());
           return ResponseEntity.ok(CollectionModel.of(doctorAppointmentsWithHallTemplate));

        }
        return new ResponseEntity<>(appointments, HttpStatus.OK);
    }
    @DeleteMapping("/doctors/{name}")
    ResponseEntity<Long> deleteDoctor(@PathVariable String name) {
        // first i verify if the target doctor which is tried to delete exist, if not i return a not found exception
        Doctor doctor = this.doctorRepository.findByName(name); //
        System.out.println(doctor);

        if(doctor == null) return  ResponseEntity.notFound().build();

        List<Appointment> theDoctorAppointments = this.appointmentRepository.findByDoctorContains(name);
        if (theDoctorAppointments.size() >0) return ResponseEntity.status(HttpStatus.CONFLICT).build();

        doctorRepository.delete(doctor);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

}
