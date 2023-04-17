package fr.univlr.info.AppointmentAPIV1.controller;

import fr.univlr.info.AppointmentAPIV1.model.Appointment;
import fr.univlr.info.AppointmentAPIV1.store.AppointmentRepository;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;


@RestController
@RequestMapping(path = "/api")
public class AppointmentController {
    private final AppointmentRepository apptRepository;
    private final AppointmentModelAssembler apptAssembler;

    public AppointmentController(AppointmentRepository apptRepository, AppointmentModelAssembler apptAssembler) {
        this.apptRepository = apptRepository;
        this.apptAssembler = apptAssembler;
    }

    @GetMapping("/appointments")
    ResponseEntity<?> all(@RequestHeader(value="Accept", required=false) String halContent ,@RequestParam(required = false) String date) {
        Date targetDate = date != null ? formatAndParseDate(date) : null;
        if (date != null && targetDate == null) return ResponseEntity.badRequest().build();
        List<Appointment> appts;
        if (date != null) {
            appts = this.apptRepository.retrieveAppointmentsByDate(targetDate);
        } else {
            appts = this.apptRepository.findAll();
        }

        if(halContent !=null && MediaTypes.HAL_JSON_VALUE.equals(halContent)) {
            List<EntityModel<Appointment>> appointmentsWithHallTemplate = this.apptRepository.findAll().stream().map(apptAssembler::toModel).collect(Collectors.toList());
            return ResponseEntity.ok(CollectionModel.of(appointmentsWithHallTemplate));
        }
        return new ResponseEntity<>(appts, HttpStatus.OK);
    }

    @PostMapping("/appointments")
    ResponseEntity<?> newAppointment(@Valid @RequestBody Appointment appt) {
        // here i used a custom repository method that i have created to get appointments between two dates
        List<Appointment> alreadyExistsAppointmentsBetweenStartAndEndDate = this.apptRepository.findAppointmentByDate(appt.getStartDate(), appt.getEndDate());
        // Now i check if there is an appointment between the start and the end date that was send in the payload; If yes i return an http conflict
        if (alreadyExistsAppointmentsBetweenStartAndEndDate.size() > 0)
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        EntityModel<Appointment> entityModel = this.apptAssembler.toModel(this.apptRepository.save(appt));
        return ResponseEntity.created(entityModel.getRequiredLink(IanaLinkRelations.SELF).toUri()).body(entityModel);

    }

    @GetMapping("/appointments/{id}")
    EntityModel<Appointment> one(@RequestHeader(value="Accept", required=false) String halContent, @PathVariable Long id) {

        Appointment appointment = this.apptRepository.findById(id) //
                .orElseThrow(() -> new AppointmentNotFoundException(id));

        return this.apptAssembler.toModel(appointment);
    }

    @PutMapping("/appointments/{id}")
    ResponseEntity<?> updateAppoint(@RequestBody Appointment apptToBeUpdated, @PathVariable Long id) {
        Appointment updatedEmployee = this.apptRepository.findById(id) //
                .map(appointment -> {
                    appointment.setDoctor(apptToBeUpdated.getDoctor());
                    appointment.setPatient(apptToBeUpdated.getPatient());
                    appointment.setStartDate(apptToBeUpdated.getStartDate());
                    appointment.setEndDate(apptToBeUpdated.getEndDate());
                    return this.apptRepository.save(appointment);
                }) //
                .orElseGet(() -> {
                    apptToBeUpdated.setId(id);
                    return this.apptRepository.save(apptToBeUpdated);
                });
        EntityModel<Appointment> entityModel = this.apptAssembler.toModel(updatedEmployee);
        return ResponseEntity.created(entityModel.getRequiredLink(IanaLinkRelations.SELF).toUri()).body(entityModel);
    }

    @DeleteMapping("/appointments/{id}/cancel")
    ResponseEntity<?>cancel(@RequestHeader(value="Accept", required=false) String halContent,@PathVariable Long id) {

        Appointment appointment = apptRepository.findById(id).orElseThrow(() -> new AppointmentNotFoundException(id));
       if(appointment.getStartDate().before(new Date())){
           return ResponseEntity.status(HttpStatus.CONFLICT).build();
       }
        this.apptRepository.deleteById(id);
        return ResponseEntity.status(HttpStatus.OK).build();

    }

    @DeleteMapping("/appointments/{id}")
    void deleteAppointment(@PathVariable Long id) {
        // first i verify if the target appointment which is tried to delete exist, if not i return a not found execption
        this.apptRepository.findById(id).orElseThrow(() -> new AppointmentNotFoundException(id));
        this.apptRepository.deleteById(id);
    }

    @DeleteMapping("/appointments")
    void deleteAllAppointments() {
        this.apptRepository.deleteAll();
    }

    public static java.util.Date formatAndParseDate(String dateToParse) {
        try {
            DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
            formatter.setLenient(false);
            return formatter.parse(dateToParse);
        } catch (ParseException e) {
            return null;
        }
    }


}
