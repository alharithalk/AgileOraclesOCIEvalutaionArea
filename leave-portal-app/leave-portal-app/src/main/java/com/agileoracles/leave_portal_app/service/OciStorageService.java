package com.agileoracles.leave_portal_app.service;

import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.requests.GetObjectRequest;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import com.oracle.bmc.objectstorage.responses.GetObjectResponse;
import com.oracle.bmc.objectstorage.responses.PutObjectResponse;
import com.oracle.bmc.Region;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class OciStorageService {

    private final String region;
    private final String namespace;
    private final String bucketName;
    private final String configFile;
    private final String profile;

    public OciStorageService(
            @Value("${oci.region}") String region,
            @Value("${oci.namespace}") String namespace,
            @Value("${oci.bucket-name}") String bucketName,
            @Value("${oci.config-file}") String configFile,
            @Value("${oci.profile}") String profile) {
        this.region = region;
        this.namespace = namespace;
        this.bucketName = bucketName;
        this.configFile = expandHome(configFile);
        this.profile = profile;
    }

    public Map<String, String> uploadFile(byte[] content, String fileName) {
        Map<String, String> result = new HashMap<>();
        try {
            ObjectStorage client = buildClient();

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .namespaceName(namespace)
                    .bucketName(bucketName)
                    .objectName(fileName)
                    .contentLength((long) content.length)
                    .putObjectBody(new ByteArrayInputStream(content))
                    .build();

            PutObjectResponse response = client.putObject(putObjectRequest);

            result.put("objectName", fileName);
            result.put("objectId", buildObjectId(fileName));
            result.put("bucketName", bucketName);
        } catch (Exception e) {
            log.error("Failed to upload file to OCI Object Storage", e);
            String cause = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            throw new RuntimeException("Failed to upload file to OCI Object Storage: " + cause, e);
        }
        return result;
    }

    public byte[] downloadFile(String objectName) {
        try {
            ObjectStorage client = buildClient();

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .namespaceName(namespace)
                    .bucketName(bucketName)
                    .objectName(objectName)
                    .build();

            GetObjectResponse response = client.getObject(getObjectRequest);
            return readAll(response.getInputStream());
        } catch (Exception e) {
            log.error("Failed to download file from OCI Object Storage", e);
            String cause = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            throw new RuntimeException("Failed to download file from OCI Object Storage: " + cause, e);
        }
    }

    private ObjectStorage buildClient() throws IOException {
        ConfigFileAuthenticationDetailsProvider provider =
                new ConfigFileAuthenticationDetailsProvider(configFile, profile);
        return ObjectStorageClient.builder()
                .region(Region.fromRegionId(region))
                .build(provider);
    }

    private byte[] readAll(InputStream inputStream) throws IOException {
        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            byte[] chunk = new byte[4096];
            int read;
            while ((read = inputStream.read(chunk)) != -1) {
                buffer.write(chunk, 0, read);
            }
            return buffer.toByteArray();
        }
    }

    private String expandHome(String path) {
        if (path != null && path.startsWith("~/")) {
            return System.getProperty("user.home") + path.substring(1);
        }
        return path;
    }

    private String buildObjectId(String fileName) {
        return "ocid1.object.oc1." + region + "." + namespace + "/" + bucketName + "/" + fileName;
    }
}
