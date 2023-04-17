package fr.univlr.info.AppointmentAPIV1.controller;
import fr.univlr.info.AppointmentAPIV1.model.Doctor;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class DoctorModelAssembler implements RepresentationModelAssembler<Doctor, EntityModel<Doctor>> {
    @Override
    public EntityModel<Doctor> toModel(Doctor doctor) {

        return EntityModel.of(doctor, //
                linkTo(methodOn(DoctorController.class).getDoctor(null,doctor.getName())).withSelfRel(),
                //linkTo(methodOn(DoctorController.class).getAppointments(null,doctor.getName())).withRel("appointments"),
                linkTo(methodOn(DoctorController.class).all(null)).withRel("appointments"),
                linkTo(methodOn(DoctorController.class).all(null)).withRel("doctors"));
    }
}
