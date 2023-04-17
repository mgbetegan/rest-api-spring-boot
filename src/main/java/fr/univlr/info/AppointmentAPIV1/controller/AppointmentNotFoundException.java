package fr.univlr.info.AppointmentAPIV1.controller;

public class AppointmentNotFoundException extends RuntimeException {
    public AppointmentNotFoundException(Long id) {
        super("Could not find appointment " + id);
    }
}
