package bo.edu.uagrm.credenciales.storage;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.GetObjectArgs;
import io.minio.PutObjectArgs;
import java.io.ByteArrayInputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MinioDocumentStorageAdapter implements DocumentStoragePort {
    private final MinioClient client;
    private final String bucket;

    public MinioDocumentStorageAdapter(MinioClient client, @Value("${app.storage.bucket}") String bucket) {
        this.client = client;
        this.bucket = bucket;
    }

    @Override
    public String store(String objectKey, byte[] content, String contentType) {
        try {
            if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
            client.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(new ByteArrayInputStream(content), content.length, -1)
                    .contentType(contentType)
                    .build());
            return objectKey;
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo almacenar el documento", exception);
        }
    }

    @Override
    public byte[] load(String objectKey) {
        try (var stream = client.getObject(GetObjectArgs.builder().bucket(bucket).object(objectKey).build())) {
            return stream.readAllBytes();
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo recuperar el documento", exception);
        }
    }
}
