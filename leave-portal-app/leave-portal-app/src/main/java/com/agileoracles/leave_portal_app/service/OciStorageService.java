package com.agileoracles.leave_portal_app.service;

import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import com.oracle.bmc.objectstorage.responses.PutObjectResponse;
import com.oracle.bmc.Region;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
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
        this.configFile = configFile;
        this.profile = profile;
    }

    public Map<String, String> uploadFile(byte[] content, String fileName) {
        Map<String, String> result = new HashMap<>();
        try {
            ConfigFileAuthenticationDetailsProvider provider =
                    new ConfigFileAuthenticationDetailsProvider(configFile, profile);

            ObjectStorage client = ObjectStorageClient.builder()
                    .region(Region.fromRegionId(region))
                    .build(provider);

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
        } catch (Exception e) {
            log.error("Failed to upload file to OCI Object Storage", e);
            String cause = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            throw new RuntimeException("Failed to upload file to OCI Object Storage: " + cause, e);
        }
        return result;
    }

    private String buildObjectId(String fileName) {
        return "ocid1.object.oc1." + region + "." + namespace + "/" + bucketName + "/" + fileName;
    }
}
