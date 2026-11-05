package bo.edu.uagrm.credenciales.storage;

public interface DocumentStoragePort {
    String store(String objectKey, byte[] content, String contentType);
    byte[] load(String objectKey);
}
