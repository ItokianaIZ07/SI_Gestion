package com.sigrh.repository;

import com.sigrh.model.PublicationOffre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PublicationOffreRepository extends JpaRepository<PublicationOffre, Integer> {
}
