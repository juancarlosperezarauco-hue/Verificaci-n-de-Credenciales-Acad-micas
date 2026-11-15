package bo.edu.uagrm.credenciales.academic;
import bo.edu.uagrm.credenciales.audit.*;
import bo.edu.uagrm.credenciales.shared.*;
import bo.edu.uagrm.credenciales.security.UserPrincipal;
import bo.edu.uagrm.credenciales.tenant.TenantRepository;
import java.time.Clock;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @Transactional
public class AcademicService {
 private final AcademicProgramRepository programs;private final AcademicRecordRepository records;private final TenantAccess access;private final TenantRepository tenants;private final Clock clock;private final AuditTrail audit;
 public AcademicService(AcademicProgramRepository p,AcademicRecordRepository r,TenantAccess a,TenantRepository t,Clock c,AuditTrail audit){programs=p;records=r;access=a;tenants=t;clock=c;this.audit=audit;}
 @Transactional(readOnly=true) public List<AcademicDtos.Program> programs(UserPrincipal p){return programs.findAllByTenantIdOrderByCreatedAtDesc(access.require(p,"ACADEMIC_VIEW")).stream().map(this::view).toList();}
 public AcademicDtos.Program saveProgram(UserPrincipal p,UUID id,AcademicDtos.ProgramInput input){UUID tenant=access.require(p,"ACADEMIC_MANAGE");var row=id==null?new AcademicProgram():programs.findByIdAndTenantId(id,tenant).orElseThrow(TenantAccess::missing);if(id==null){row.id=UUID.randomUUID();row.tenantId=tenant;row.createdAt=clock.instant();}row.name=input.name().strip();row.kind=input.kind().strip();row.period=input.period().strip();row.requirements=input.requirements();row.active=input.active();programs.save(row);audit.record(AuditAction.ACADEMIC_PROGRAM_UPDATED,tenant,AuditTrail.Resource.ACADEMIC,row.id);return view(row);}
 public AcademicDtos.Record enroll(UserPrincipal p,AcademicDtos.Enroll input){UUID tenant=access.require(p,"ACADEMIC_MANAGE");access.linked(input.studentId(),tenant,true);var program=programs.findByIdAndTenantId(input.programId(),tenant).filter(x->x.active).orElseThrow(TenantAccess::missing);var row=new AcademicRecord();row.id=UUID.randomUUID();row.tenantId=tenant;row.studentId=input.studentId();row.programId=program.id;row.registrationCode=input.registrationCode().strip();row.enrolledOn=input.enrolledOn();row.status=AcademicRecord.Status.ENROLLED;row.createdAt=clock.instant();records.saveAndFlush(row);audit.record(AuditAction.ACADEMIC_RECORD_UPDATED,tenant,AuditTrail.Resource.ACADEMIC,row.id);return view(row);}
 public AcademicDtos.Record update(UserPrincipal p,UUID id,AcademicDtos.Update input){UUID tenant=access.require(p,"ACADEMIC_MANAGE");var row=owned(id,tenant);access.linked(row.studentId,tenant,true);if(input.completedOn()!=null&&input.completedOn().isBefore(row.enrolledOn))throw new IllegalArgumentException("La finalización no puede ser anterior a la matrícula.");if(input.status()==AcademicRecord.Status.COMPLETED&&input.completedOn()==null)throw new IllegalArgumentException("Indica la fecha de finalización.");row.status=input.status();row.completedOn=input.completedOn();row.result=input.result();row.notes=input.notes();audit.record(AuditAction.ACADEMIC_RECORD_UPDATED,tenant,AuditTrail.Resource.ACADEMIC,id);return view(row);}
 @Transactional(readOnly=true) public ResultPage<AcademicDtos.Record> list(UserPrincipal p,UUID student,int page,int size){ResultPage.validate(page,size);var result=records.list(access.require(p,"ACADEMIC_VIEW"),student,PageRequest.of(page,size,Sort.by(Sort.Direction.DESC,"createdAt")));return new ResultPage<>(result.stream().map(this::view).toList(),result.getTotalElements(),page,size);}
 @Transactional(readOnly=true) public List<AcademicDtos.Record> own(UUID student){access.own(student,false);return records.findAllByStudentIdOrderByCreatedAtDesc(student).stream().map(this::view).toList();}
 public AcademicRecord require(UUID id,UUID tenant,UUID student,boolean lock){var row=owned(id,tenant);if(!row.studentId.equals(student))throw TenantAccess.missing();access.linked(student,tenant,lock);return row;}
 public AcademicRecord snapshot(UUID id,UUID tenant,UUID student){var row=owned(id,tenant);if(!row.studentId.equals(student))throw TenantAccess.missing();return row;}
 public String programName(AcademicRecord record){return programs.findById(record.programId).orElseThrow(TenantAccess::missing).name;}
 private AcademicRecord owned(UUID id,UUID tenant){return records.findByIdAndTenantId(id,tenant).orElseThrow(TenantAccess::missing);}
 private AcademicDtos.Program view(AcademicProgram p){return new AcademicDtos.Program(p.id,p.name,p.kind,p.period,p.requirements,p.active);}
 private AcademicDtos.Record view(AcademicRecord r){var program=programs.findById(r.programId).orElseThrow();var student=access.own(r.studentId,false);return new AcademicDtos.Record(r.id,r.tenantId,tenants.findById(r.tenantId).orElseThrow().getName(),r.studentId,student.getFullName(),program.id,program.name,program.period,program.requirements,r.registrationCode,r.status,r.enrolledOn,r.completedOn,r.result,r.notes,r.createdAt);}
}
