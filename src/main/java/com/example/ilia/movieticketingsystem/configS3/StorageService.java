package com.example.ilia.movieticketingsystem.configS3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.util.IOUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class StorageService {

    @Autowired
    private AmazonS3 amazonS3Client;


    @Value("${application.bucket.name}")
    private String bucket;


    public String uploadFile(MultipartFile file){

        File fileUpload = null;
        try {
            fileUpload = convertMultipartFileToFile(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String filename = System.currentTimeMillis()+"_"+file.getOriginalFilename();
        amazonS3Client.putObject(new PutObjectRequest(bucket, filename, fileUpload));
        fileUpload.delete();
        return converToURLEncoded(filename);
    }

    private String converToURLEncoded(String s) {
        String url = URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
        return "https://marketplace-api-storage.s3.us-east-2.amazonaws.com/" + url;
    }

    public byte[] donwloadFile(String fileName) {
        S3Object s3Object = amazonS3Client.getObject(bucket, fileName);
        S3ObjectInputStream inputStream = s3Object.getObjectContent();
        try {
            byte[] content = IOUtils.toByteArray(inputStream);
            return content;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public String deleteFile(String fileName){
        amazonS3Client.deleteObject(bucket,fileName);
        return fileName + " was removed";
    }

    private File convertMultipartFileToFile(MultipartFile multipartFile) throws IOException {
        File file = new File(Objects.requireNonNull(multipartFile.getOriginalFilename()));
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(multipartFile.getBytes());
        } catch(IOException e) {
            log.error("Error converting multipartFile to file", e);
        }
        return file;
    }

    private boolean isImageFile(MultipartFile file){
        List<String> acceptableContentTypes = Arrays.asList("image/jpeg", "image/png", "image/gif");
        return acceptableContentTypes.contains(file.getContentType());
    }
}
