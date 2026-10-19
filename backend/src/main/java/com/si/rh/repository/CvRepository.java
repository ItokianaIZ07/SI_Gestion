package com.sigrh.repository;

import com.sigrh.model.Cv;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CvRepository extends JpaRepository<Cv, Integer> {
}
