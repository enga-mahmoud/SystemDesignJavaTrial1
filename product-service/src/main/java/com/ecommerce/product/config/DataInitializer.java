package com.ecommerce.product.config;

import com.ecommerce.product.dto.ProductRequest;
import com.ecommerce.product.repository.ProductRepository;
import com.ecommerce.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final ProductRepository productRepository;
    private final ProductService productService;

    private static final UUID VENDOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Override
    public void run(ApplicationArguments args) {
        if (productRepository.count() > 0) {
            // Products already exist — re-queue outbox events so Elasticsearch stays in sync.
            // Elasticsearch upserts by product ID are idempotent, so this is safe on every restart.
            productService.reindexAll();
            return;
        }

        List<ProductRequest> requests = List.of(
            request("Wireless Noise-Cancelling Headphones",
                "Premium over-ear headphones with active noise cancellation, 30-hour battery life, and foldable design.",
                "249.99"),
            request("Mechanical Keyboard",
                "TKL mechanical keyboard with Cherry MX switches, RGB backlighting, and aluminium frame.",
                "129.99"),
            request("4K USB-C Monitor",
                "27-inch 4K IPS display with USB-C 90W power delivery, HDR400, and 99% sRGB coverage.",
                "449.99"),
            request("Ergonomic Office Chair",
                "Fully adjustable mesh chair with lumbar support, headrest, and breathable backrest.",
                "379.00"),
            request("Standing Desk",
                "Electric height-adjustable desk with memory presets, anti-collision sensor, and cable management tray.",
                "599.00"),
            request("Webcam 1080p",
                "Full HD webcam with built-in stereo microphone, auto-focus, and low-light correction.",
                "79.99"),
            request("Portable SSD 1TB",
                "USB 3.2 Gen 2 external SSD with up to 1050 MB/s read speed and shock-resistant casing.",
                "109.99"),
            request("Smart LED Desk Lamp",
                "Touch-controlled lamp with wireless charging pad, five colour temperatures, and USB-A charging port.",
                "49.99"),
            request("USB-C Hub 7-in-1",
                "Compact hub with 4K HDMI, 100W PD, SD/microSD readers, and three USB-A 3.0 ports.",
                "39.99"),
            request("Laptop Stand",
                "Aluminium laptop stand with six adjustable height levels and foldable design for portability.",
                "34.99"),
            request("Wireless Mouse",
                "Silent wireless mouse with 2.4 GHz receiver, 90-day battery life, and three-level DPI switch.",
                "29.99"),
            request("Noise-Cancelling Earbuds",
                "True wireless earbuds with hybrid ANC, 8-hour playtime, IPX4 water resistance, and fast-charge case.",
                "149.99")
        );

        for (ProductRequest req : requests) {
            productService.createProduct(req, VENDOR_ID);
        }
        log.info("Seeded {} sample products with outbox events", requests.size());
    }

    private ProductRequest request(String name, String description, String price) {
        ProductRequest req = new ProductRequest();
        req.setName(name);
        req.setDescription(description);
        req.setPrice(new BigDecimal(price));
        return req;
    }
}
