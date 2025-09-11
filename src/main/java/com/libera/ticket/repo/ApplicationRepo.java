package com.libera.ticket.repo;

import com.libera.ticket.domain.Application;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface ApplicationRepo extends JpaRepository<Application, UUID> {
}
