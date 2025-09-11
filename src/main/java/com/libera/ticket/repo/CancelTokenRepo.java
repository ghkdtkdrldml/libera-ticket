package com.libera.ticket.repo;

import com.libera.ticket.domain.CancelToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface CancelTokenRepo extends JpaRepository<CancelToken, UUID> {}
