package bo.edu.uagrm.credenciales.identity;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface StudentAccountRepository extends JpaRepository<StudentAccount, UUID> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    Optional<StudentAccount> findBySearchCode(String searchCode);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select s from StudentAccount s where s.id = :id")
    Optional<StudentAccount> findLockedById(@org.springframework.data.repository.query.Param("id") UUID id);
    Optional<StudentAccount> findByEmail(String email);
    boolean existsByCountryAndDocumentTypeAndDocumentNumber(String country, String type, String number);
}
