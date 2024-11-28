package com.mindup.core.services.IMPL;

import com.mindup.core.dtos.Appointment.*;
import com.mindup.core.entities.AppointmentEntity;
import com.mindup.core.entities.User;
import com.mindup.core.enums.AppointmentStatus;
import com.mindup.core.enums.Role;
import com.mindup.core.exceptions.AppointmentConflictException;
import com.mindup.core.exceptions.ResourceNotFoundException;
import com.mindup.core.exceptions.RoleMismatchException;
import com.mindup.core.exceptions.UserNotFoundException;
import com.mindup.core.mappers.AppointmentMapper;
import com.mindup.core.repositories.IAppointmentRepository;
import com.mindup.core.repositories.UserRepository;
import com.mindup.core.services.IAppointmentService;
import com.mindup.core.validations.UserValidation;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class AppointmentServiceImpl implements IAppointmentService {
    private final IAppointmentRepository appointmentRepository;
    private final AppointmentMapper appointmentMapper;
    private final UserRepository userRepository;
    private final UserValidation userValidation;

    @Override
    public Set<ResponseAppointmentDto> getPatientReservedAppointments(String id) {
        // Cheking if patient exists
        User patient = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException("Patient not found"));

        // Checking patient role
        if (patient.getRole() != Role.PATIENT)
            throw new IllegalArgumentException("User must be a patient");

        List<AppointmentEntity> appointmentEntityList = appointmentRepository.findAll();
        Set<AppointmentEntity> acceptedList = appointmentEntityList.stream()
                .filter(appointmentEntity -> appointmentEntity.getStatus() == AppointmentStatus.ACCEPTED &&
                        appointmentEntity.getPatient().getUserId() == patient.getUserId() &&
                        appointmentEntity.getSoftDelete() == null)
                .collect(Collectors.toSet());
        return appointmentMapper.toResponseDtoSet(acceptedList);
    }

    @Override
    public Set<ResponseAppointmentDto> getPshychologistReservedAppointment(String id) {
        // Cheking if patient exists
        User psychologist = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Psychologist not found"));

        // Checking psychologist role
        if (psychologist.getRole() != Role.PSYCHOLOGIST)
            throw new RoleMismatchException("User must be a psychologist");

        List<AppointmentEntity> appointmentEntityList = appointmentRepository.findAll();
        Set<AppointmentEntity> acceptedList = appointmentEntityList.stream()
                .filter(appointmentEntity -> appointmentEntity.getStatus() == AppointmentStatus.ACCEPTED &&
                        appointmentEntity.getPsychologist().getUserId() == psychologist.getUserId() &&
                        appointmentEntity.getSoftDelete() == null)
                .collect(Collectors.toSet());
        return appointmentMapper.toResponseDtoSet(acceptedList);
    }
    // #######################################################################/*

    @Override
    public Set<ResponseAppointmentDto> getAppointmentsByPatient(String id) {
        // Checking if patient exists
        User patient = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException("Patient not found"));

        if (patient.getRole() != Role.PATIENT)
            throw new RoleMismatchException("Bad argument, user isn't a patient");

        Set<AppointmentEntity> appointments = appointmentRepository.getAppointmentsByPatient(patient);
        return appointmentMapper.toResponseDtoSet(appointments);
    }

    @Override
    public Set<ResponseAppointmentDto> getAppointmentsByPsychologist(String id) {
        // Checking if psycologist exists
        User psychologist = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Patient not found"));

        if (psychologist.getRole() != Role.PSYCHOLOGIST)
            throw new RoleMismatchException("Bad argument, user isn't a psychologist");

        Set<AppointmentEntity> appointments = appointmentRepository.getAppointmentsByPsychologist(psychologist);
        return appointmentMapper.toResponseDtoSet(appointments);
    }

    @Override
    public Set<ResponseAppointmentDto> getAppointmentsPending() {
        List<AppointmentEntity> appointmentEntityList = appointmentRepository.findAll();
        Set<AppointmentEntity> pendingList = appointmentEntityList.stream()
                .filter(appointmentEntity -> appointmentEntity.getStatus() == AppointmentStatus.PENDING &&
                        appointmentEntity.getSoftDelete() == null)
                .collect(Collectors.toSet());
        return appointmentMapper.toResponseDtoSet(pendingList);
    }

    @Override
    public Set<ResponseAppointmentDto> getAppointmentsAccepted() {
        List<AppointmentEntity> appointmentEntityList = appointmentRepository.findAll();
        Set<AppointmentEntity> acceptedList = appointmentEntityList.stream()
                .filter(appointmentEntity -> appointmentEntity.getStatus() == AppointmentStatus.ACCEPTED &&
                        appointmentEntity.getSoftDelete() == null)
                .collect(Collectors.toSet());
        return appointmentMapper.toResponseDtoSet(acceptedList);
    }

    @Override
    public Set<ResponseAppointmentDto> getAppointmetsCanceled() {
        List<AppointmentEntity> appointmentEntityList = appointmentRepository.findAll();
        Set<AppointmentEntity> canceledList = appointmentEntityList.stream()
                .filter(appointmentEntity -> appointmentEntity.getStatus() == AppointmentStatus.CANCELED &&
                        appointmentEntity.getSoftDelete() == null)
                .collect(Collectors.toSet());
        return appointmentMapper.toResponseDtoSet(canceledList);
    }

    // agregar checkear si el que lo crea es el de la sesion// posible, no se si es necesario

    @Override
    public ResponseCreateAppointmentDto add(RequestCreateAppointmentDto requestDto) {
        // Checking if patient and psychologist exists
        User patient = userRepository.findById(requestDto.patientId())
                .orElseThrow(() -> new UserNotFoundException("Patient not found"));
        User psychologist = userRepository.findById(requestDto.psychologistId())
                .orElseThrow(() -> new UserNotFoundException("Psychologist not found"));


        // Checking if patient already has an appointment that day
        LocalDateTime startOfDay = requestDto.date().toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        long patientAppointmentsCount = appointmentRepository.countByPatientAndDateBetween(
                patient, startOfDay, endOfDay);

        if (patientAppointmentsCount > 0) {
            throw new AppointmentConflictException("Patient already has an appointment on this day");
        }
        // Preguntar al front que se dedique a esto 

        // Verify psychologist appointments 30 minutes before and after
        LocalDateTime beforeAppointment = requestDto.date().minusMinutes(30);

        long psychologistAppointmentsCount = appointmentRepository.countByPsychologistAndDateBefore(
                psychologist, beforeAppointment);

        if (psychologistAppointmentsCount > 0) {
            throw new AppointmentConflictException("Psychologist is not available at this time");
        }

        // scheduling an appointment/*
        AppointmentEntity appointmen = AppointmentEntity.builder()
                .patient(patient)
                .psychologist(psychologist)
                .date(requestDto.date())
                .status(AppointmentStatus.PENDING)
                .build();

        AppointmentEntity savedAppointment = appointmentRepository.save(appointmen);
        return appointmentMapper.appointmentToResponseCreateAppointmentDto(savedAppointment);
    }

    @Override
    public ResponseAppointmentDto aceptAppointment(String appointmentId){
        AppointmentEntity appointment = appointmentRepository.findById(appointmentId)
        .orElseThrow(() -> new ResourceNotFoundException("Appointment doesn't exist"));

        appointment.setStatus(AppointmentStatus.ACCEPTED);
        appointmentRepository.save(appointment);

        return appointmentMapper.toResponseDto(appointment);
    }


    @Override
    public ResponseAppointmentDto cancelAppointment(String appointmentId){

        AppointmentEntity appointment = appointmentRepository.findById(appointmentId)
        .orElseThrow(() -> new ResourceNotFoundException("Appointment doesn't exist"));

        appointment.setStatus(AppointmentStatus.CANCELED);
        appointmentRepository.save(appointment);

        return appointmentMapper.toResponseDto(appointment);
    };
    
    // preguntar al front que se dedique a esto
    @Override
    public ResponseAppointmentDto update(RequestUpdateAppointmentDto requestUpdateAppointmentDto) {
        // Checking if patient and psychologist exists
        User patient = userRepository.findById(requestUpdateAppointmentDto.patientId())
                .orElseThrow(() -> new UserNotFoundException("Patient not found"));
        User psychologist = userRepository.findById(requestUpdateAppointmentDto.psychologistId())
                .orElseThrow(() -> new UserNotFoundException("Psychologist not found"));

        // Checking Roles
        if (patient.getRole() != Role.PATIENT)
            throw new RoleMismatchException("User must be a patient to schedule an appointment");
        if (psychologist.getRole() == Role.PSYCHOLOGIST)
            throw new RoleMismatchException("User must be a psychologist to schedule an appointment");

        if(!userValidation.isPatient()) {
            throw new RoleMismatchException("Only psychologists can cancel appointments");
        }
            
        // Checking if appointment exists
        AppointmentEntity updatedEntity = appointmentRepository
                .findById(requestUpdateAppointmentDto.appointmenId())
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        // Verify patient has no other appointments on the same day
        LocalDateTime startOfDay = requestUpdateAppointmentDto.date().toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        long patientAppointmentsCount = appointmentRepository.countByPatientAndDateBetweenAndIdNot(
                patient, startOfDay, endOfDay, updatedEntity.getId());

        if (patientAppointmentsCount > 0) {
            throw new AppointmentConflictException("Patient already has an appointment on this day");
        }

        // Verify psychologist availability (10 minutes before and after)
        LocalDateTime beforeAppointment = requestUpdateAppointmentDto.date().minusMinutes(10);
        LocalDateTime afterAppointment = requestUpdateAppointmentDto.date().plusMinutes(10);

        long psychologistAppointmentsCount = appointmentRepository.countByPsychologistAndDateBetweenAndIdNot(
                psychologist, beforeAppointment, afterAppointment, updatedEntity.getId());

        if (psychologistAppointmentsCount > 0) {
            throw new AppointmentConflictException("Psychologist is not available at this time");
        }

        // Update appointment
        updatedEntity.setPatient(patient);
        updatedEntity.setPsychologist(psychologist);
        updatedEntity.setDate(requestUpdateAppointmentDto.date());

        AppointmentEntity updatedAppointment = appointmentRepository.save(updatedEntity);
        return appointmentMapper.toResponseDto(updatedAppointment);
    }

    @Override
    public ResponseDeleteAppointmentDto delete(String appointmentId) {
        AppointmentEntity appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment doesn't exist"));

        ZoneId zoneId = ZoneId.of("America/Argentina/Buenos_Aires");
        appointment.setSoftDelete(ZonedDateTime.now(zoneId).toLocalDateTime());

        AppointmentEntity deletedAppointment = appointmentRepository.save(appointment);

        return ResponseDeleteAppointmentDto.builder()
                .appointmentId(deletedAppointment.getId())
                .dateSoftDelete(deletedAppointment.getSoftDelete())
                .status(deletedAppointment.getStatus())
                .build();
    }

    @Override
    public ResponseReactivateAppointmentDto reactivateAppointment(String appointmentId) {
        AppointmentEntity appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment doesn't exist"));

        appointment.setSoftDelete(null);
        AppointmentEntity reactivatedAppointment = appointmentRepository.save(appointment);

        return appointmentMapper.appointmentToResponseReactivateAppointmentDto(reactivatedAppointment);
    }

}