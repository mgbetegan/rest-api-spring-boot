package fr.univlr.info.AppointmentAPIV1.model;

import javax.persistence.*;
import java.util.List;

@Entity
public class Doctor {
    @Id
    @GeneratedValue
    private Long id;
    private String name;
    @Transient
    @OneToMany(mappedBy = "DoctorObj")
    private List<Appointment> appointments;
    public Doctor(String name) {
        this.name=name;
    }

    public Doctor() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Appointment> getAppointments() {
        return appointments;
    }

    public void setAppointments(List<Appointment> appointments) {
        this.appointments = appointments;
    }

}

