package com.app.MailService.Service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Service
public class S3Service {
    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Autowired
    private AmazonS3 amazonS3;

    public String uploadFile(String fileName, byte[] inputBytes) {
        InputStream inputStream = new ByteArrayInputStream(inputBytes);

        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, fileName, inputStream, new ObjectMetadata())
                .withCannedAcl(CannedAccessControlList.PublicRead);

        amazonS3.putObject(putObjectRequest);
        return amazonS3.getUrl(bucketName, fileName).toString();
    }
}
