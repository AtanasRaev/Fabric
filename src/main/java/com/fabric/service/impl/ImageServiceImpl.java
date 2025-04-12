package com.fabric.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.fabric.database.entity.Clothing;
import com.fabric.database.entity.Image;
import com.fabric.database.entity.enums.Type;
import com.fabric.database.repository.ImageRepository;
import com.fabric.exceptions.ImageUploadException;
import com.fabric.exceptions.UnsupportedImageFormatException;
import com.fabric.service.ImageService;
import com.luciad.imageio.webp.WebPImageWriterSpi;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class ImageServiceImpl implements ImageService {
    private final ImageRepository imageRepository;
    private final Cloudinary cloudinary;

    @Value("${app.ftpServer}")
    private String ftpServer;

    @Value("${app.ftpPort}")
    private Integer ftpPort;

    @Value("${app.ftpUser}")
    private String ftpUser;

    @Value("${app.ftpPassword}")
    private String ftpPassword;

    public ImageServiceImpl(ImageRepository imageRepository,
                            Cloudinary cloudinary) {
        this.imageRepository = imageRepository;
        this.cloudinary = cloudinary;
    }

    @Override
    public void saveAll(List<Image> extractedImages) {
        extractedImages.forEach(image -> {
            if (image != null && this.imageRepository.findByPublicId(image.getPublicId()).isEmpty()) {
                this.imageRepository.save(image);
            }
        });
    }

    @Override
    public void deleteAll(List<String> publicIds) {
        List<Image> allByPublicIds = this.imageRepository.findAllByPublicIds(publicIds);

        List<CompletableFuture<Void>> deleteFutures = allByPublicIds.stream()
                .map(image -> {
                    CompletableFuture<Void> cloudDelete = CompletableFuture.runAsync(() ->
                            deleteImageFromCloudinary(image.getPublicId())
                    );

                    CompletableFuture<Void> hostDelete = CompletableFuture.runAsync(() ->
                            deleteImageFromHost(image.getPublicId())
                    );

                    return CompletableFuture.allOf(cloudDelete, hostDelete);
                })
                .toList();

        CompletableFuture.allOf(deleteFutures.toArray(new CompletableFuture[0])).join();

        this.imageRepository.deleteAll(allByPublicIds);
    }

    @Override
    public void saveImageInCloud(MultipartFile file, Clothing clothing, String side) {
        if (file == null || file.isEmpty()) {
            throw new UnsupportedImageFormatException("The image format is not supported");
        }

        String uniqueImageId = getUniqueImageId(clothing, side);
        uploadImageInCloud(file, uniqueImageId);
    }

    @Override
    public void saveImageInHost(MultipartFile file, Clothing clothing, String side) {
        if (file == null || file.isEmpty()) {
            throw new UnsupportedImageFormatException("The image format is not supported");
        }

        String uniqueImageId = getUniqueImageId(clothing, side);
        uploadImageInHost(file, uniqueImageId);
    }

    @Override
    public Image findByPublicId(String publicId) {
        return this.imageRepository.findByPublicId(publicId)
                .orElse(null);
    }

    @Override
    public void deleteImage(Image byPath) {
        deleteImageFromCloudinary(byPath.getPublicId());
        this.imageRepository.delete(byPath);
    }

    @Override
    public String getUniqueImageId(Clothing clothing, String side) {
        return clothing.getModel() + clothing.getCategory() + "_" + getModelType(clothing.getType()) + "_" + side;
    }

    protected void uploadImageInCloud(MultipartFile multipartFile, String publicId) {
        try {
            this.cloudinary.uploader().upload(multipartFile.getBytes(), ObjectUtils.asMap(
                    "public_id", publicId)
            );
        } catch (IOException e) {
            System.err.println("Image upload failed: " + e.getMessage());
            throw new RuntimeException("Failed to upload image", e);
        }
    }

    public void uploadImageInHost(MultipartFile file, String uniqueImageId) {
        FTPClient ftpClient = new FTPClient();
        try {
            ByteArrayInputStream webpInputStream = processImage(file);
            String remoteFileName = uniqueImageId + ".webp";

            uploadFileToFTP(ftpClient, webpInputStream, remoteFileName);

            System.out.println("File uploaded successfully as: " + remoteFileName);
        } finally {
            if (ftpClient.isConnected()) {
                try {
                    ftpClient.disconnect();
                } catch (IOException ex) {
                    System.err.println("Error disconnecting from FTP server: " + ex.getMessage());
                }
            }
        }
    }

    private ByteArrayInputStream processImage(MultipartFile file) {
        try {
            IIORegistry.getDefaultInstance().registerServiceProvider(new WebPImageWriterSpi());

            BufferedImage originalImage = ImageIO.read(file.getInputStream());
            if (originalImage == null) {
                throw new ImageUploadException("Could not read image from the provided file.");
            }

            BufferedImage resizedImage = resizeImage(originalImage, 1048, 1292);

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            boolean writeSuccess = ImageIO.write(resizedImage, "webp", os);
            if (!writeSuccess) {
                throw new ImageUploadException("Failed to write the image in WebP format. Check the WebP plugin setup.");
            }

            return new ByteArrayInputStream(os.toByteArray());
        } catch (IOException e) {
            throw new ImageUploadException("Error processing image: " + e.getMessage(), e);
        }
    }

    private BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) {
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = resizedImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        g2d.dispose();
        return resizedImage;
    }

    private void uploadFileToFTP(FTPClient ftpClient, InputStream fileStream, String remoteFileName) {
        try {
            connectAndLogin(ftpClient);
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

            boolean uploaded = ftpClient.storeFile(remoteFileName, fileStream);
            if (!uploaded) {
                throw new ImageUploadException("Failed to upload the file to the FTP server.");
            }

            ftpClient.logout();
        } catch (IOException e) {
            throw new ImageUploadException("Error during FTP upload: " + e.getMessage(), e);
        }
    }

    private String getModelType(Type type) {
        switch (type) {
            case SHORTS -> {
                return "K";
            }
            case SWEATSHIRT -> {
                return "SW";
            }
            case LONG_T_SHIRT -> {
                return "D";
            }
            case KIT -> {
                return "KT";
            }
            case TOWELS -> {
                return "T";
            }
            case BANDANAS -> {
                return "B";
            }
            default -> {
                return "";
            }
        }
    }

    private void deleteImageFromCloudinary(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("invalidate", true));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteImageFromHost(String uniqueImageId) {
        FTPClient ftpClient = new FTPClient();
        String remoteFileName = uniqueImageId + ".webp";
        try {
            connectAndLogin(ftpClient);

            boolean deleted = ftpClient.deleteFile(remoteFileName);
            if (!deleted) {
                throw new ImageUploadException("Failed to delete file: " + remoteFileName + " from the FTP server.");
            }

            System.out.println("File " + remoteFileName + " deleted successfully.");

            ftpClient.logout();
        } catch (IOException e) {
            throw new ImageUploadException("Error during FTP deletion: " + e.getMessage(), e);
        } finally {
            if (ftpClient.isConnected()) {
                try {
                    ftpClient.disconnect();
                } catch (IOException ex) {
                    System.err.println("Error disconnecting from FTP server: " + ex.getMessage());
                }
            }
        }
    }

    private void connectAndLogin(FTPClient ftpClient) throws IOException {
        ftpClient.connect(this.ftpServer, this.ftpPort);
        int reply = ftpClient.getReplyCode();
        if (!FTPReply.isPositiveCompletion(reply)) {
            throw new ImageUploadException("FTP server refused connection. Reply code: " + reply);
        }

        if (!ftpClient.login(this.ftpUser, this.ftpPassword)) {
            throw new ImageUploadException("Could not login to the FTP server with provided credentials.");
        }

        ftpClient.enterLocalPassiveMode();
    }
}
