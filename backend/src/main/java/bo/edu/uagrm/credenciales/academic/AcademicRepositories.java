package bo.edu.uagrm.credenciales.academic;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
interface AcademicProgramRepository extends JpaRepository<AcademicProgram,UUID>{
 List<AcademicProgram> findAllByTenantIdOrderByCreatedAtDesc(UUID tenant);
 Optional<AcademicProgram> findByIdAndTenantId(UUID id,UUID tenant);
}
interface AcademicRecordRepository extends JpaRepository<AcademicRecord,UUID>{
 Optional<AcademicRecord> findByIdAndTenantId(UUID id,UUID tenant);
 List<AcademicRecord> findAllByStudentIdOrderByCreatedAtDesc(UUID student);
 @Query("select r from AcademicRecord r where r.tenantId=:tenant and (:student is null or r.studentId=:student) and exists(select c.id from StudentConsent c where c.student.id=r.studentId and c.tenant.id=:tenant and c.usedAt is not null and c.revokedAt is null) and exists(select s.id from StudentAccount s where s.id=r.studentId and s.active=true)")
 Page<AcademicRecord> list(@Param("tenant") UUID tenant,@Param("student") UUID student,Pageable page);
}
