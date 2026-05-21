package com.fctnow.backend.asignaciones.externas;

import com.fctnow.backend.asignaciones.AsignacionEstado;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface AsignacionFctExternaRepository extends JpaRepository<AsignacionFctExterna, Long> {

  boolean existsBySolicitudId(Long solicitudId);

  boolean existsByAlumnoIdAndEstado(Long alumnoId, AsignacionEstado estado);

  @Modifying
  @Transactional
  @Query("delete from AsignacionFctExterna a where a.alumno.id = :alumnoId")
  int deleteByAlumnoId(@Param("alumnoId") Long alumnoId);

  @Query("""
      SELECT a
      FROM AsignacionFctExterna a
      JOIN FETCH a.solicitud s
      JOIN FETCH a.alumno
      ORDER BY a.fechaAsignacion DESC""")
  List<AsignacionFctExterna> findAllWithDetails();

  @Query("""
      SELECT a
      FROM AsignacionFctExterna a
      JOIN FETCH a.solicitud s
      JOIN FETCH a.alumno
      WHERE a.alumno.id IN :alumnoIds
      ORDER BY a.fechaAsignacion DESC""")
  List<AsignacionFctExterna> findByAlumnoIdInWithDetails(
      @Param("alumnoIds") Collection<Long> alumnoIds);

  @Query("""
      SELECT a
      FROM AsignacionFctExterna a
      JOIN FETCH a.solicitud s
      JOIN FETCH a.alumno
      WHERE a.alumno.id = :alumnoId
        AND a.estado = :estado
      ORDER BY a.fechaAsignacion DESC, a.id DESC""")
  Optional<AsignacionFctExterna> findFirstByAlumnoIdAndEstadoWithDetails(
      @Param("alumnoId") Long alumnoId,
      @Param("estado") AsignacionEstado estado);
}
