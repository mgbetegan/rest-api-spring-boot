package fr.univlr.info.AppointmentAPIV1.controller;

import fr.univlr.info.AppointmentAPIV1.model.Appointment;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
public class AppointmentDateValidator
        implements ConstraintValidator<AppointmentDateConstraint, Appointment> {
    @Override
    public void initialize(AppointmentDateConstraint dateCst) {
    }

    @Override
    public boolean isValid(Appointment app,
                           ConstraintValidatorContext ctxt) {
        if(app.getStartDate()!= null) {
            return app.getStartDate().before(app.getEndDate());
        } else{
            return false ;
        }
    }

}
