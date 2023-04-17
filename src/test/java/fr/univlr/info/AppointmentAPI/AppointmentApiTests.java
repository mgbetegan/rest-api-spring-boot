package fr.univlr.info.AppointmentAPI;

import fr.univlr.info.AppointmentAPIV1.AppointmentApiApplication;
import fr.univlr.info.AppointmentAPIV1.model.Appointment;
import fr.univlr.info.AppointmentAPIV1.model.Doctor;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.client.Traverson;
import org.springframework.hateoas.server.core.TypeReferences;
import org.springframework.http.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DateTimeException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.hateoas.client.Hop.rel;

@SpringBootTest(classes = AppointmentApiApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AppointmentApiTests {
    @LocalServerPort
    private int port;

    // static because for each test one object (AppointmentApiTests) is created...
    private static Appointment createdAppt = null;

    @Autowired
    private RestTemplate restTemplate;

    // utility method...
    public static Date parseDate(String date) throws DateTimeException {
        try {
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm").parse(date);
        } catch (ParseException e) {
            throw new DateTimeException("Wrong date: " + date);
        }
    }

    // start and end dates for the first appointment created
    private static final String currentYear = String.valueOf((new GregorianCalendar()).get(Calendar.YEAR));
    private static final Date startDate = parseDate(currentYear + "-07-09T9:30");
    private static final Date endDate = parseDate(currentYear + "-07-09T10:15");

    @Test
    @Order(1)
    public void testGetAllAppointment1() {
        ResponseEntity<Appointment[]> response =
                restTemplate.getForEntity("http://localhost:" + port +
                        "/api/appointments", Appointment[].class);
        assertSame(response.getStatusCode(),HttpStatus.OK);
        Appointment[] apptArray = response.getBody();
        if (apptArray != null) {
            assertEquals(apptArray.length, 0);
        } else {
            Assertions.fail("Null array for no appointment");
        }
    }


    @Test
    @Order(2)
    public void testAddAppointment1() {
        // WARNING: for the test to work, the date of the appointment must be later than the current date
        if (startDate.before(new Date())) {
            Assertions.fail("The start date of this appointment must be later than the current date");
        }
        if (endDate.before(new Date())) {
            Assertions.fail("The end date of this appointment must be later than the current date");
        }

        Appointment appt1 = new Appointment("mjones", startDate, endDate, "patient1");
        // Not needed...
        //  HttpHeaders headers = new HttpHeaders();
        //  headers.set("Accept", "application/json");
        // @TODO : add CORS
        HttpEntity<Appointment> request = new HttpEntity<>(appt1, null);
        ResponseEntity<Appointment> response =
                restTemplate.postForEntity("http://localhost:" + port +
                        "/api/appointments", request, Appointment.class);
        assertSame(response.getStatusCode(),HttpStatus.CREATED);
        // the response must contain resource location
        String newResourceURL = response.getHeaders().getFirst("Location");
        if (newResourceURL != null) {
            try {
                restTemplate.getForObject(newResourceURL, Appointment.class);
            } catch (HttpStatusCodeException e) {
                Assertions.fail("Invalid location: " + newResourceURL);
            }
        } else {
            Assertions.fail("The header of HTTP response must contains the new resource location");
        }
    }

    @Test
    @Order(3)
    public void testGetAllAppointment2() {
        ResponseEntity<Appointment[]> response =
                restTemplate.getForEntity("http://localhost:" + port +
                        "/api/appointments", Appointment[].class);
        assertSame(response.getStatusCode(),HttpStatus.OK);
        Appointment[] apptArray = response.getBody();
        if (apptArray != null) {
            assertSame(apptArray.length, 1); // one appointment created at test #2
            createdAppt = apptArray[0]; // used by other tests
            assertEquals(createdAppt.getDoctor(), "mjones");
            assertEquals(createdAppt.getStartDate(), startDate);
            assertEquals(createdAppt.getEndDate(), endDate);
            assertEquals(createdAppt.getPatient(), "patient1");
        } else {
            Assertions.fail("Appointments not found.");
        }
    }

    @Test
    @Order(4)
    public void testGetOneAppointment() {
        Appointment appt = restTemplate.getForObject("http://localhost:" + port +
                "/api/appointments/" + createdAppt.getId(), Appointment.class);
        if (appt != null) {
            assertTrue(appt != null && appt.getId().equals(createdAppt.getId()));
            // check the response
            assertEquals(appt.getDoctor(), "mjones");
            assertEquals(appt.getStartDate(), startDate);
            assertEquals(appt.getEndDate(), endDate);
            assertEquals(appt.getPatient(), "patient1");
        } else {
            Assertions.fail("Appointment with id #" + createdAppt.getId() + " not found.");
        }
    }

    @Test
    @Order(5)
    public void testUpdateAppointment1() {
        // modification of an existing FUTURE appointment
        // the doctor's name is changed
        String originalDoctor = createdAppt.getDoctor();
        createdAppt.setDoctor("jsmith");
        Long originalId = createdAppt.getId();
        // attempt to modify the appointment id but this isn't allowed
        // and it must be ignored...
        createdAppt.setId(123L);
        this.restTemplate.put("http://localhost:" + port +
                "/api/appointments/" + originalId, createdAppt);
        Appointment updAppt = restTemplate.getForObject("http://localhost:" + port +
                "/api/appointments/" + originalId, Appointment.class);
        if (updAppt != null) {
            // originalId must not have been modified
            assertSame(updAppt.getId(), originalId);
            // but doctor must have been modified
            assertEquals(updAppt.getDoctor(), "jsmith");
        } else {
            Assertions.fail("Updated appointment not found.");
        }
        // restore createdAppt object
        createdAppt.setId(originalId);
        createdAppt.setDoctor(originalDoctor);
    }

    @Test
    @Order(6)
    public void testAddIncoherentAppointment() {
        // start date > end date -> incoherent appointment
        Appointment appt = new Appointment("mjones",
                parseDate("2023-01-13T10:15"), parseDate("2023-01-13T9:30"),"patient2");
        try {
            Appointment newAppt = this.restTemplate.postForObject("http://localhost:" + port +
                    "/api/appointments", appt, Appointment.class);
            Assertions.fail("Incoherent appointment not detected : " + newAppt);
        } catch (HttpStatusCodeException e) {
            assertSame(e.getStatusCode(),HttpStatus.NOT_ACCEPTABLE);
        }
    }

    @Test
    @Order(7)
    public void testAddInvalidAppointment() {
        // start date is null -> invalid appointment
        Appointment appt = new Appointment("mjones",
                null, parseDate(currentYear + "-07-13T9:30"),"patient3");
        try {
            Appointment newAppt = this.restTemplate.postForObject("http://localhost:" + port +
                    "/api/appointments", appt, Appointment.class);
            Assertions.fail("Incoherent appointment not detected : " + newAppt);
        } catch (HttpStatusCodeException e) {
            assertSame(e.getStatusCode(),HttpStatus.NOT_ACCEPTABLE);
        }
    }

    @Test
    @Order(8)
    public void testAddAppointment2() {
        Appointment newAppt = new Appointment("mjones",
                parseDate(currentYear + "-07-25T14:00"),
                parseDate(currentYear + "-07-25T14:50"), "patient4");
        ResponseEntity<Appointment> responsePost =
                this.restTemplate.postForEntity("http://localhost:" + port + "/api/appointments",
                        newAppt, Appointment.class);
        assertSame(responsePost.getStatusCode(),HttpStatus.CREATED);
        ResponseEntity<Appointment[]> responseGet =
                restTemplate.getForEntity("http://localhost:" + port +
                        "/api/appointments", Appointment[].class);
        assertSame(responseGet.getStatusCode(),HttpStatus.OK);
        Appointment[] apptArray = responseGet.getBody();
        if (apptArray != null) {
            assertEquals(apptArray.length, 2);
        } else {
            Assertions.fail("Appointments not found.");
        }
    }

    @Test
    @Order(9)
    public void testDeleteAppointment() {
        ResponseEntity<Void> response = this.restTemplate.exchange(
                "http://localhost:" + port + "/api/appointments/" + createdAppt.getId(),
                HttpMethod.DELETE, null, Void.class);
        assertSame(response.getStatusCode(),HttpStatus.OK);
        try {
            Appointment appt = restTemplate.getForObject("http://localhost:" + port +
                    "/api/appointments/" + createdAppt.getId(), Appointment.class);
            Assertions.fail("Appointment not deleted : " + appt);
        } catch (HttpStatusCodeException e) {
            // if the appointment is deleted then we should not be able to find it...
            assertSame(e.getStatusCode(),HttpStatus.NOT_FOUND);
        }
    }

    @Test
    @Order(10)
    public void testDeletetAppointmentWithInvalidId() {
        try {
            restTemplate.delete("http://localhost:" + port + "/api/appointments/3333333");
            Assertions.fail("Incoherent appointment id not detected");
        } catch (HttpStatusCodeException e) {
            assertSame(e.getStatusCode(),HttpStatus.NOT_FOUND);
        }
    }

    @Test
    @Order(11)
    public void testDeleteAppointmentAll() {
        try {
            String urlStr = "http://localhost:" + port + "/api/appointments";
            restTemplate.delete(urlStr); // delete all appointments
            ResponseEntity<Appointment[]> responseEntity = restTemplate.getForEntity(urlStr, Appointment[].class);
            Appointment[] apptArray = responseEntity.getBody();
            if (apptArray != null) {
                assertEquals(apptArray.length, 0);
            } else {
                Assertions.fail("Null array for no appointment");
            }
        } catch (HttpStatusCodeException e) {
            Assertions.fail("Delete all appointments failed, error = " + e.getStatusCode());
        }
    }

    // Question 3 : updated API **********************************************
    // Uncomment instantiation of doctors in LoadDatabase class.
    // We start with no appointments (see test 11)

    @Test
    @Order(12)
    public void testGetAllDoctors1() {
        ResponseEntity<Doctor[]> response =
                restTemplate.getForEntity("http://localhost:" + port + "/api/doctors", Doctor[].class);
        assertSame(response.getStatusCode(),HttpStatus.OK);
        Doctor[] docArray = response.getBody();
        if (docArray != null) {
            assertEquals(docArray.length,3);
        } else {
            Assertions.fail("Doctors not found.");
        }
    }

    @Test
    @Order(13)
    public void testGetOneDoctor() {
        Doctor doctor = restTemplate.getForObject("http://localhost:" + port + "/api/doctors/mjones",
                Doctor.class);
        if (doctor != null) {
            assertEquals(doctor.getName(), "mjones");
        } else {
            Assertions.fail("Doctor not found.");
        }
    }

    @Test
    @Order(14)
    public void testAddAppointmentsforOneDoctor() {
        // new appointment for doctor mjones
        Appointment appt1 = new Appointment("mjones",
                parseDate(currentYear + "-09-25T14:00"),
                parseDate(currentYear + "-09-25T14:50"), "patient5");
        this.restTemplate.postForObject("http://localhost:" + port +
                "/api/appointments", appt1, Appointment.class);
        Appointment appt2 = new Appointment("mjones",
                parseDate(currentYear + "-10-25T16:00"),
                parseDate(currentYear + "-10-25T16:30"), "patient6");
        this.restTemplate.postForObject("http://localhost:" + port +
                "/api/appointments", appt2, Appointment.class);

        ResponseEntity<Appointment[]> response = restTemplate.getForEntity("http://localhost:" + port +
                "/api/doctors/mjones/appointments", Appointment[].class);
        assertSame(response.getStatusCode(),HttpStatus.OK);
        Appointment[] apptArray = response.getBody();
        if (apptArray != null) {
            assertEquals(apptArray.length, 2);
        } else {
            Assertions.fail("Appointments not found.");
        }
    }

    @Test
    @Order(15)
    public void testAddAlreadyBookedAppointment() {
        // this new appointment overlaps (conflicts) a previous one
        Appointment appt1 = new Appointment("mjones",
                parseDate(currentYear + "-09-25T14:20"),
                parseDate(currentYear + "-09-25T15:00"), "patient3");
        try {
            Appointment newAppt = this.restTemplate.postForObject("http://localhost:" + port +
                    "/api/appointments", appt1, Appointment.class);
            Assertions.fail("Appointment conflict not detected: " + newAppt);
        } catch (HttpStatusCodeException e) {
            // error detected -> OK
            assertSame(e.getStatusCode(),HttpStatus.CONFLICT);
        }
    }

    @Test
    @Order(16)
    public void testDeleteDoctorFail() {
        // test integrity constraint violation : we could not delete a doctor with appointments
        try {
            restTemplate.delete("http://localhost:" + port + "/api/doctors/mjones");
            Assertions.fail("Integrity constraint violation not detected.");
        } catch (HttpStatusCodeException e) {
            assertSame(e.getStatusCode(),HttpStatus.CONFLICT);
        }
    }

    @Test
    @Order(17)
    public void testDeleteDoctorOK() {
        try {
            restTemplate.delete("http://localhost:" + port + "/api/doctors/jsmith");
        } catch (HttpStatusCodeException e) {
            Assertions.fail("Doctor jsmith not deleted.");
        }
        try {
            restTemplate.getForObject("http://localhost:" + port + "/api/doctors/jsmith", Doctor.class);
            Assertions.fail("Doctor jsmith still exists.");
        } catch (HttpStatusCodeException e) {
            assertSame(e.getStatusCode(),HttpStatus.NOT_FOUND);
        }
    }

    // Question 4 : templated request ****************************************

    @Test
    @Order(18)
    public void testGetAllAppointmentsAfterADate() {
        String date = currentYear + "-09-26T10:00";
        ResponseEntity<Appointment[]> response =
                restTemplate.getForEntity("http://localhost:" + port +
                        "/api/appointments?date=" + date, Appointment[].class);
        assertSame(response.getStatusCode(),HttpStatus.OK);
        Appointment[] apptArray = response.getBody();
        if (apptArray != null) {
            assertEquals(apptArray.length, 1);
            Appointment appt = apptArray[0];
            assertEquals(appt.getDoctor(), "mjones");
            assertEquals(appt.getStartDate(), parseDate(currentYear + "-10-25T16:00"));
            assertEquals(appt.getEndDate(), parseDate(currentYear + "-10-25T16:30"));
            assertEquals(appt.getPatient(), "patient6");
        } else {
            Assertions.fail("Appointments not found.");
        }
    }

    @Test
    @Order(19)
    public void testGetAllAppointmentsWithInvalidDate() {
        String date = "2021-20-27T17:34";
        try {
            restTemplate.getForEntity("http://localhost:" + port +
                    "/api/appointments?date=" + date, Appointment[].class);
            Assertions.fail("Invalid date not detected: " + date);
        } catch (HttpStatusCodeException e) {
            assertSame(e.getStatusCode(),HttpStatus.BAD_REQUEST);
        }
    }

    // Question 5 : HAL feature **********************************************

    @Test
    @Order(20)
    public void testGetAllAppointmentHALWithRestTemplate() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/hal+json"); // get JSON/HAL
        HttpEntity<String> request = new HttpEntity<>("", headers);
        ResponseEntity<CollectionModel<EntityModel<Appointment>>> response = this.restTemplate.exchange(
                "http://localhost:" + port + "/api/appointments",
                HttpMethod.GET, request, new TypeReferences.CollectionModelType<EntityModel<Appointment>>() {
                });
        assertSame(response.getStatusCode(), HttpStatus.OK);
        CollectionModel<EntityModel<Appointment>> apptEntities = response.getBody();
        if (apptEntities != null) {
            List<EntityModel<Appointment>> appts = new ArrayList<>(apptEntities.getContent());
            assertEquals(appts.size(), 2);
            for (EntityModel<Appointment> apptEntity : appts) {
                assertTrue(apptEntity.hasLink("self"));
                assertTrue(apptEntity.hasLink("appointments"));
            }
            createdAppt = appts.get(0).getContent(); // for the next tests
        } else {
            Assertions.fail("HAL appointments not found.");
        }
    }

    @Test
    @Order(21)
    public void testGetOneAppointmentHALWithRestTemplate() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/hal+json"); // get JSON/HAL
        HttpEntity<String> request = new HttpEntity<>("", headers);
        ResponseEntity<EntityModel<Appointment>> response = this.restTemplate.exchange(
                "http://localhost:" + port + "/api/appointments/" + createdAppt.getId(),
                HttpMethod.GET, request, new TypeReferences.EntityModelType<Appointment>() {
                });
        assertSame(response.getStatusCode(),HttpStatus.OK);
        EntityModel<Appointment> apptEntity = response.getBody();
        if (apptEntity != null) {
            assertTrue(apptEntity.hasLink("self"));
            assertTrue(apptEntity.hasLink("appointments"));
            Appointment appt = apptEntity.getContent();
            if (appt != null) {
                assertEquals(appt.getPatient(), createdAppt.getPatient());
            } else {
                Assertions.fail("Appointment not found.");
            }
        } else {
            Assertions.fail("HAL appointment not found.");
        }
    }

    @Test
    @Order(22)
    public void testGetOneAppointmentHAL() {
        Traverson client = new Traverson(URI.create("http://localhost:" + port +
                "/api/appointments/" + createdAppt.getId()), MediaTypes.HAL_JSON);
        EntityModel<Appointment> apptEntity = client //
                .follow("self") //
                .toObject(new TypeReferences.EntityModelType<Appointment>() {
                });
        if (apptEntity != null) {
            Appointment appt = apptEntity.getContent();
            if (appt != null) {
                assertEquals(appt.getPatient(), createdAppt.getPatient());
            } else {
                Assertions.fail("Appointment not found.");
            }
        } else {
            Assertions.fail("HAL appointment not found.");
        }
    }

    @Test
    @Order(23)
    public void testGetAllDoctorsHALWithRestTemplate() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/hal+json"); // get HAL/JSON
        HttpEntity<String> request = new HttpEntity<>("", headers);
        ResponseEntity<CollectionModel<EntityModel<Doctor>>> response =
                this.restTemplate.exchange(
                        "http://localhost:" + port + "/api/doctors",
                        HttpMethod.GET, request, new TypeReferences.CollectionModelType<EntityModel<Doctor>>() {
                        });
        assertSame(response.getStatusCode(),HttpStatus.OK);
        CollectionModel<EntityModel<Doctor>> cmEMDoctors = response.getBody();
        if (cmEMDoctors != null) {
            Collection<EntityModel<Doctor>> cEMDoctors = cmEMDoctors.getContent();
            assertEquals(cEMDoctors.size(), 2);
            List<String> docNames = new ArrayList<>();
            for (EntityModel<Doctor> doctorEntity : cEMDoctors) {
                assertTrue(doctorEntity.hasLink("self"));
                assertTrue(doctorEntity.hasLink("doctors"));
                Doctor doc = doctorEntity.getContent();
                if (doc != null) {
                    docNames.add(doc.getName());
                } else {
                    Assertions.fail("Doctor not found.");
                }
            }
            // no assumption on the doctor order
            List<String> nameTest = Arrays.asList("mjones","jdoe");
            Assertions.assertIterableEquals(docNames, nameTest);
        } else {
            Assertions.fail("Doctor entities not found.");
        }
    }

    @Test
    @Order(24)
    public void testGetOneDoctorHALWithRestTemplate() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/hal+json"); // get JSON/HAL
        HttpEntity<String> request = new HttpEntity<>("", headers);
        ResponseEntity<EntityModel<Doctor>> response = this.restTemplate.exchange(
                "http://localhost:" + port + "/api/doctors/mjones",
                HttpMethod.GET, request, new ParameterizedTypeReference<EntityModel<Doctor>>() {});
        assertSame(response.getStatusCode(),HttpStatus.OK);
        EntityModel<Doctor> docEntity = response.getBody();
        if (docEntity != null) {
            assertTrue(docEntity.hasLink("self"));
            assertTrue(docEntity.hasLink("doctors"));
            Doctor doctor = docEntity.getContent();
            if (doctor != null) {
                assertEquals(doctor.getName(), "mjones");
            } else {
                Assertions.fail("Doctor not found.");
            }
        } else {
            Assertions.fail("Doctor entity not found.");
        }
    }

    @Test
    @Order(25)
    public void testGetOneDoctorHAL() {
        Traverson client = new Traverson(URI.create("http://localhost:" + port +
                "/api/doctors/mjones"), MediaTypes.HAL_JSON);
        EntityModel<Doctor> doctorEntity = client //
                .follow("self") //
                .toObject(new TypeReferences.EntityModelType<Doctor>() {});
        if (doctorEntity != null) {
            Doctor doctor = doctorEntity.getContent();
            if (doctor != null) {
                assertEquals(doctor.getName(), "mjones");
            } else {
                Assertions.fail("Doctor not found.");
            }
        } else {
            Assertions.fail("Doctor entity not found.");
        }
    }

    @Test
    @Order(26)
    public void testGetAllDoctorsWithLinkHAL() {
        Traverson client = new Traverson(URI.create("http://localhost:" + port +
                "/api/doctors/mjones"), MediaTypes.HAL_JSON);
        // get the link for all doctor resources
        Traverson.TraversalBuilder builder = client.follow(rel("doctors"));
        CollectionModel<EntityModel<Doctor>> cmEMDoctors =
                builder.toObject(new TypeReferences.CollectionModelType<EntityModel<Doctor>>() {});
        if (cmEMDoctors != null) {
            assertEquals(cmEMDoctors.getContent().size(), 2);
        } else {
            Assertions.fail("Doctor entities not found.");
        }
    }

    @Test
    @Order(27)
    public void testGetAppointmentForOneDoctorHAL() {
        Traverson client = new Traverson(URI.create("http://localhost:" + port +
                "/api/doctors/mjones"), MediaTypes.HAL_JSON);
        // get the link for all appointment resources for doctor mjones
        Traverson.TraversalBuilder builder = client.follow(rel("appointments"));
        CollectionModel<EntityModel<Appointment>> apptForMJones =
                builder.toObject(new TypeReferences.CollectionModelType<EntityModel<Appointment>>() {});
        if (apptForMJones != null) {
            List<EntityModel<Appointment>> apptEntities = new ArrayList<>(apptForMJones.getContent());
            assertEquals(apptEntities.size(), 2);
        } else {
            Assertions.fail("Appointment entities for doctor Jones not found.");
        }
    }

    // Question 6 : action feature **********************************************

    @Test
    @Order(28)
    public void testCancelOneAppointmentFailWithRestTemplate() {
        // creates an appointment in the past so cannot be changed
        // new appointment for doctor mjones
        Appointment appt = new Appointment("mjones",
                parseDate("2021-01-25T14:00"), parseDate("2021-01-25T14:50"), "patient5");

        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/hal+json"); // get JSON
        HttpEntity<Appointment> request = new HttpEntity<>(appt, headers);
        ResponseEntity<EntityModel<Appointment>> response =
                this.restTemplate.exchange(
                        "http://localhost:" + port + "/api/appointments",
                        HttpMethod.POST, request, new TypeReferences.EntityModelType<Appointment>() {
                        });

        EntityModel<Appointment> apptEM = response.getBody();
        if (apptEM != null) {
            Appointment apptCreated = apptEM.getContent();
            if (apptCreated != null) {
                try {
                    // try to cancel the appointment
                    this.restTemplate.delete("http://localhost:" + port + "/api/appointments/" +
                            apptCreated.getId() + "/cancel");
                    Assertions.fail("A past appointment cannot be cancelled.");
                } catch (HttpStatusCodeException e) {
                    assertSame(e.getStatusCode(),HttpStatus.CONFLICT);
                    // now we really delete the appointment
                    restTemplate.delete("http://localhost:" + port + "/api/appointments/" + apptCreated.getId());
                }
            } else {
                Assertions.fail("Problem about appointment for doctor Jones.");
            }
        } else {
            Assertions.fail("Problem about appointment entity for doctor Jones.");
        }
    }

    @Test
    @Order(29)
    public void testExistingActionsForOneFutureAppointment() {
        // WARNING: for the test to work, the date of the appointment must be later than the current date
        Date sDate = parseDate("2023-06-25T14:00");
        if (sDate.before(new Date())) {
            Assertions.fail("The start date of this appointment must be later than the current date");
        }
        Date eDate = parseDate("2023-06-25T14:50");
        if (eDate.before(new Date())) {
            Assertions.fail("The end date of this appointment must be later than the current date");
        }
        Appointment appt = new Appointment("mjones", sDate, eDate, "patient8");

        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/hal+json"); // get JSON
        HttpEntity<Appointment> request = new HttpEntity<>(appt, headers);
        ResponseEntity<EntityModel<Appointment>> response =
                this.restTemplate.exchange(
                        "http://localhost:" + port + "/api/appointments",
                        HttpMethod.POST, request, new TypeReferences.EntityModelType<Appointment>() {
                        });
        assertSame(response.getStatusCode(), HttpStatus.CREATED);
        EntityModel<Appointment> apptEntity = response.getBody();
        if (apptEntity != null) {
            // for a future appointment, we must have these two action links
            assertTrue(apptEntity.hasLink("cancel"));
            assertTrue(apptEntity.hasLink("update"));
            Appointment apptCreated = apptEntity.getContent();
            if (apptCreated != null) {
                // get the appointment id
                Long apptId = apptCreated.getId();
                // get the action link
                Link cancelLnk = apptEntity.getRequiredLink("cancel");

                try {
                    // we use the cancel link
                    this.restTemplate.delete(cancelLnk.getHref());
                    try {
                        // test the cancellation
                        this.restTemplate.exchange(
                                "http://localhost:" + port + "/api/appointments/" + apptId,
                                HttpMethod.GET, request, new ParameterizedTypeReference<EntityModel<Appointment>>() {
                                });
                        Assertions.fail("Appointment not cancelled.");
                    } catch (HttpStatusCodeException e) {
                        // ok appointment is cancelled
                        assertSame(e.getStatusCode(), HttpStatus.NOT_FOUND);
                    }
                } catch (HttpStatusCodeException e) {
                    Assertions.fail("Invalid cancel action.");
                }
            } else {
                Assertions.fail("Problem about appointment (patient8) for doctor Jones.");
            }
        } else {
            Assertions.fail("Problem about appointment entity (patient8) for doctor Jones.");
        }
    }

    @Test
    @Order(30)
    public void testNotExistingActionsForOnePastAppointment() {
        Appointment appt = new Appointment("mjones",
                parseDate("2020-06-25T14:00"), parseDate("2020-06-25T14:50"), "patient8");

        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/hal+json"); // get JSON
        HttpEntity<Appointment> request = new HttpEntity<>(appt, headers);
        ResponseEntity<EntityModel<Appointment>> response =
                this.restTemplate.exchange(
                        "http://localhost:" + port + "/api/appointments",
                        HttpMethod.POST, request, new TypeReferences.EntityModelType<Appointment>() {
                        });
        assertSame(response.getStatusCode(),HttpStatus.CREATED);
        EntityModel<Appointment> apptEntity = response.getBody();
        if (apptEntity != null) {
            // no action link for a past appointment
            assertFalse(apptEntity.hasLink("cancel"));
            assertFalse(apptEntity.hasLink("update"));
        }
    }
}