package fr.univlr.info.AppointmentAPIV1.controller;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import fr.univlr.info.AppointmentAPIV1.model.Appointment;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;
import java.util.Date;

@Component
class AppointmentModelAssembler implements RepresentationModelAssembler<Appointment, EntityModel<Appointment>>{
    @Override
    public EntityModel<Appointment> toModel(Appointment appointment) {

        EntityModel<Appointment> appointmentModel = EntityModel.of(appointment, //
                linkTo(methodOn(AppointmentController.class).one(null,appointment.getId())).withSelfRel(),

                linkTo(methodOn(AppointmentController.class).all(null,null)).withRel("appointments")
                );
        if(appointment.getStartDate().after(new Date())){
            appointmentModel.add(linkTo(methodOn(AppointmentController.class).cancel(null,appointment.getId())).withRel("cancel"));
            appointmentModel.add(linkTo(methodOn(AppointmentController.class).newAppointment(appointment)).withRel("update"));
        }
        return appointmentModel;
    }

    public EntityModel<Appointment> toModel2(Appointment appointment) {

        return new EntityModel(appointment, //
                //linkTo(methodOn(AppointmentController.class).one(null,appointment.getId())).withSelfRel(),

                //linkTo(methodOn(AppointmentController.class).all(null,null)).withRel("appointments"),
                linkTo(methodOn(DoctorController.class).getAppointments(null,appointment.getDoctorObj().getName())).withRel("appointments")
        );
    }





}







