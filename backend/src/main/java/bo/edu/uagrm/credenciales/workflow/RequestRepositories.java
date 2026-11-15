package bo.edu.uagrm.credenciales.workflow;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
interface RequestEventRepository extends JpaRepository<RequestEvent,UUID>{List<RequestEvent> findAllByRequestIdOrderByOccurredAtAsc(UUID id);}
interface RequestAttachmentRepository extends JpaRepository<RequestAttachment,UUID>{List<RequestAttachment> findAllByRequestIdOrderByCreatedAtAsc(UUID id);Optional<RequestAttachment> findByIdAndRequestId(UUID id,UUID request);long countByRequestId(UUID id);}
