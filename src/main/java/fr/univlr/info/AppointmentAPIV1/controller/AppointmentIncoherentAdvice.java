package fr.univlr.info.AppointmentAPIV1.controller;

import org.springframework.http.HttpStatus;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;

@ControllerAdvice
public class AppointmentIncoherentAdvice {
    @ResponseBody
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
    String appointmentIncoherentHandler(MethodArgumentNotValidException ex) {
        //return ex.getBindingResult().getAllErrors().get(1).unwrap(ConstraintViolation.class).getMessageTemplate();
        List<ObjectError> errorList = ex.getBindingResult().getAllErrors();
        return errorList.get(errorList.size()-1).getDefaultMessage();
    }
}
