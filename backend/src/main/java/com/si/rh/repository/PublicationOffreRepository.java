package com.si.rh.repository;

import com.si.rh.model.PublicationOffre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.si.rh.model.enums.StatutPublication;
import java.util.List;
import java.util.Optional;

@Repository
public interface PublicationOffreRepository extends JpaRepository<PublicationOffre, Integer> {
    List<PublicationOffre> findByStatut(StatutPublication statut);
}
