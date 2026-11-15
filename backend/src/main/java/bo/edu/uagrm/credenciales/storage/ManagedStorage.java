package bo.edu.uagrm.credenciales.storage;
import bo.edu.uagrm.credenciales.security.AeadKeyRing;
import bo.edu.uagrm.credenciales.identityreview.IdentityImages;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.*;
@Service
public class ManagedStorage {
 private final DocumentStoragePort raw;private final ManagedObjectRegistry registry;private final JdbcTemplate jdbc;private final AeadKeyRing keys;
 public ManagedStorage(DocumentStoragePort raw,ManagedObjectRegistry registry,JdbcTemplate jdbc,@Value("${app.evidence.encryption-key:}") String secret,@Value("${app.evidence.key-id:legacy}") String keyId,@Value("${app.evidence.previous-keys:}") String previous){this.raw=raw;this.registry=registry;this.jdbc=jdbc;this.keys=secret.isBlank()?null:new AeadKeyRing(keyId,secret,previous);}
 public boolean configured(){return keys!=null;}public String keyId(){return keys==null?"UNCONFIGURED":keys.currentId();}
 public String store(UUID tenant,UUID owner,String purpose,byte[] bytes,String contentType){
  if(keys==null)throw new IllegalStateException("El almacenamiento privado requiere configurar su clave de cifrado.");
  if(!TransactionSynchronizationManager.isActualTransactionActive())throw new IllegalStateException("La escritura requiere una transacción.");
  if(!purpose.matches("[A-Z_]{1,30}"))throw new IllegalArgumentException("Categoría de archivo inválida.");
  String key="managed/"+purpose.toLowerCase(Locale.ROOT)+"/"+owner+"/"+UUID.randomUUID()+".bin";
  registry.reserve(key,owner,tenant,purpose,IdentityImages.hash(bytes),bytes.length,keys.currentId());
  TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void beforeCommit(boolean readOnly){registry.activate(key);}@Override public void afterCompletion(int status){registry.finish(key,status==STATUS_COMMITTED);}});
  try{raw.store(key,keys.encrypt(aad(key),bytes).getBytes(StandardCharsets.UTF_8),"application/octet-stream");return key;}
  catch(RuntimeException failed){throw new IllegalStateException("No se pudo guardar el archivo privado.");}
 }
 public byte[] load(String key){
  var rows=jdbc.queryForList("select state,content_hash,key_id,purpose from managed_objects where object_key=?",key);
  if(rows.isEmpty())return raw.load(key); // historical objects are never silently rewritten
  if("REMOVED".equals(rows.getFirst().get("state")))throw new IllegalArgumentException("El archivo ya fue retirado según la política de conservación.");
  if(keys==null&&!"LEGACY_IDENTITY".equals(rows.getFirst().get("purpose")))throw new IllegalStateException("No está disponible la clave del archivo privado.");
  byte[] bytes="LEGACY_IDENTITY".equals(rows.getFirst().get("purpose"))?raw.load(key):keys.decrypt(aad(key),new String(raw.load(key),StandardCharsets.UTF_8));
  if(!IdentityImages.hash(bytes).equals(rows.getFirst().get("content_hash")))throw new IllegalStateException("La integridad del archivo no coincide.");
  return bytes;
 }
 public void requestDeletion(String key){if(key!=null)jdbc.update("update managed_objects set delete_requested_at=CURRENT_TIMESTAMP where object_key=? and state<>'REMOVED'",key);}
 public void retireIdentityObject(String key,UUID owner,UUID tenant,String hash){
  if(key==null)return;if(key.startsWith("managed/")){requestDeletion(key);return;}
  if(!key.matches("private/identity/[0-9a-f-]{36}/"+owner+"/(front|back|photo)\\.jpg"))return;
  jdbc.update("insert into managed_objects(id,object_key,owner_id,tenant_id,purpose,content_hash,size_bytes,key_id,state,created_at,safe_after,delete_requested_at) select ?,?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP where not exists(select 1 from managed_objects where object_key=?)",UUID.randomUUID(),key,owner,tenant,"LEGACY_IDENTITY",hash,0,"PLAIN","BOUND",key);
 }
 public byte[] recipher(String key,byte[] encrypted){if(keys==null)throw new IllegalStateException("Cifrado no configurado.");return keys.encrypt(aad(key),keys.decrypt(aad(key),new String(encrypted,StandardCharsets.UTF_8))).getBytes(StandardCharsets.UTF_8);}
 private byte[] aad(String key){return ("credential-private-v1:"+key).getBytes(StandardCharsets.UTF_8);}
}
