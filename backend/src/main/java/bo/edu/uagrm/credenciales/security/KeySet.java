package bo.edu.uagrm.credenciales.security;

import java.util.*;

/** Server-configured key identifiers; never accepts key material from requests. */
public final class KeySet {
 private final String currentId;private final Map<String,String> keys;
 public KeySet(String currentId,String current,String previous){
  validateId(currentId);this.currentId=currentId;Map<String,String> values=new LinkedHashMap<>();values.put(currentId,current);
  if(previous!=null&&!previous.isBlank())for(String entry:previous.split(";")){
   String[] pair=entry.split("=",2);if(pair.length!=2)throw new IllegalArgumentException("Formato de claves anteriores inválido");validateId(pair[0]);
   if(values.putIfAbsent(pair[0],pair[1])!=null)throw new IllegalArgumentException("Identificador de clave repetido");
  }
  if(values.size()>10)throw new IllegalArgumentException("Demasiadas claves configuradas");keys=Collections.unmodifiableMap(values);
 }
 public static void validateId(String id){if(id==null||!id.matches("[A-Za-z0-9-]{1,40}"))throw new IllegalArgumentException("Identificador de clave inválido");}
 public String currentId(){return currentId;}
 public String value(String id){String value=keys.get(id);if(value==null)throw new IllegalArgumentException("Clave no disponible");return value;}
 public Set<String> ids(){return keys.keySet();}
 @Override public String toString(){return "KeySet[redacted]";}
}
