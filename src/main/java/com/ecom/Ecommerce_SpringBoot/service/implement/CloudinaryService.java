package com.ecom.Ecommerce_SpringBoot.service.implement;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Wraps Cloudinary image upload with a "no-op when not configured" guard.
 *
 * <p>If {@code cloudinary.cloud-name} / {@code cloudinary.api-key} /
 * {@code cloudinary.api-secret} are missing, blank, or the documented
 * placeholder values that ship in {@code .env.production.example}, every
 * call to {@link #uploadImage} returns a public fallback image URL instead
 * of contacting Cloudinary. The app keeps working — products / categories
 * just render the placeholder picture until real credentials are wired up.
 *
 * <p>Without this guard, Cloudinary's SDK throws a generic
 * {@code RuntimeException("Slow down. Too many concurrent requests!")} when
 * the API key is invalid, which the controller's {@code catch (IOException)}
 * misses and turns into a 500 Whitelabel error.
 */
@Service
public class CloudinaryService {

    private static final Logger log = LoggerFactory.getLogger(CloudinaryService.class);

    /** Generic image used when Cloudinary isn't configured or an upload fails. */
    public static final String FALLBACK_IMAGE_URL =
            "https://res.cloudinary.com/demo/image/upload/v1/default.jpg";

    private final Cloudinary cloudinary;

    @Value("${cloudinary.cloud-name:}")
    private String cloudName;

    @Value("${cloudinary.api-key:}")
    private String apiKey;

    @Value("${cloudinary.api-secret:}")
    private String apiSecret;

    /** Notice logged once on first skipped upload, then stays quiet. */
    private final AtomicBoolean disabledNoticeLogged = new AtomicBoolean(false);

    public CloudinaryService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    public boolean isConfigured() {
        if (cloudName == null || cloudName.isBlank() || "dummy".equalsIgnoreCase(cloudName)
                || "test-cloud".equalsIgnoreCase(cloudName) || "your-cloud".equalsIgnoreCase(cloudName)) {
            return false;
        }
        if (apiKey == null || apiKey.isBlank() || "000000000000000".equals(apiKey)) {
            return false;
        }
        if (apiSecret == null || apiSecret.isBlank()
                || "dummy_secret".equalsIgnoreCase(apiSecret)
                || "test-secret".equalsIgnoreCase(apiSecret)
                || "your-secret".equalsIgnoreCase(apiSecret)) {
            return false;
        }
        return true;
    }

    public String uploadImage(MultipartFile file, String folder) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        if (!isConfigured()) {
            if (disabledNoticeLogged.compareAndSet(false, true)) {
                log.info("Cloudinary not configured (cloudinary.cloud-name/api-key/api-secret missing " +
                        "or placeholder). Skipping uploads; returning fallback image URL.");
            }
            return FALLBACK_IMAGE_URL;
        }

        Map<String, Object> options = ObjectUtils.asMap(
                "folder", folder,
                "overwrite", true,
                "resource_type", "image",
                "invalidate", true
        );

        try {
            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), options);
            return result.get("secure_url").toString();
        } catch (RuntimeException ex) {
            // Cloudinary's SDK wraps API errors in RuntimeException, which the
            // controller's `catch (IOException)` would otherwise miss → 500.
            // Rethrow as IOException so existing controller error handling kicks in.
            throw new IOException("Cloudinary upload failed: " + ex.getMessage(), ex);
        }
    }
}
