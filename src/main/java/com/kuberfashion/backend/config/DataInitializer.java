package com.kuberfashion.backend.config;

import com.kuberfashion.backend.entity.User;
import com.kuberfashion.backend.entity.Product;
import com.kuberfashion.backend.entity.Category;
import com.kuberfashion.backend.repository.UserRepository;
import com.kuberfashion.backend.repository.ProductRepository;
import com.kuberfashion.backend.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Create admin user if it doesn't exist
        if (!userRepository.existsByEmail("admin@kuberfashion.com")) {
            User admin = new User();
            admin.setFirstName("Admin");
            admin.setLastName("User");
            admin.setEmail("admin@kuberfashion.com");
            admin.setPhone("1234567890");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole(User.Role.ADMIN);
            admin.setEnabled(true);
            
            userRepository.save(admin);
            System.out.println("Admin user created: admin@kuberfashion.com / admin123");
        }
        
        // Create test user if it doesn't exist
        if (!userRepository.existsByEmail("test@kuberfashion.com")) {
            User testUser = new User();
            testUser.setFirstName("Test");
            testUser.setLastName("User");
            testUser.setEmail("test@kuberfashion.com");
            testUser.setPhone("9876543210");
            testUser.setPassword(passwordEncoder.encode("test123"));
            testUser.setRole(User.Role.USER);
            testUser.setEnabled(true);
            
            userRepository.save(testUser);
            System.out.println("Test user created: test@kuberfashion.com / test123");
        }

        // Initialize categories
        initializeCategories();
        
        // Initialize sample products
        initializeSampleProducts();
    }

    private void initializeCategories() {
        String[] categoryData = {
            "mens-fashion,Men's Fashion",
            "womens-fashion,Women's Fashion", 
            "kids-fashion,Kids Fashion",
            "footwear,Footwear",
            "accessories,Accessories",
            "sports-active,Sports & Active"
        };

        for (String data : categoryData) {
            String[] parts = data.split(",");
            String slug = parts[0];
            String name = parts[1];
            
            if (!categoryRepository.existsBySlug(slug)) {
                Category category = new Category();
                category.setSlug(slug);
                category.setName(name);
                category.setDescription("Premium " + name.toLowerCase() + " collection");
                category.setActive(true);
                categoryRepository.save(category);
                System.out.println("Category created: " + name);
            }
        }
    }

    private void initializeSampleProducts() {
        try {
            if (productRepository.count() > 0) {
                System.out.println("Products already exist, skipping initialization");
                return; // Products already exist
            }

            System.out.println("Initializing sample products...");

            Category mensCategory = categoryRepository.findBySlug("mens-fashion").orElse(null);
            Category womensCategory = categoryRepository.findBySlug("womens-fashion").orElse(null);
            Category footwearCategory = categoryRepository.findBySlug("footwear").orElse(null);

            if (mensCategory == null || womensCategory == null || footwearCategory == null) {
                System.err.println("ERROR: Categories not found! Cannot create products.");
                return;
            }

        if (mensCategory != null) {
            createProduct("Classic Cotton T-Shirt", "classic-cotton-tshirt", 
                new BigDecimal("29.99"), new BigDecimal("39.99"), 25, mensCategory,
                "https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80",
                Arrays.asList("https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80"),
                "Premium quality cotton t-shirt with comfortable fit. Perfect for casual wear and everyday comfort.",
                Arrays.asList("S", "M", "L", "XL", "XXL"),
                Arrays.asList("White", "Black", "Navy", "Gray"),
                true, true, 50);

            createProduct("Denim Jacket", "denim-jacket",
                new BigDecimal("69.99"), new BigDecimal("89.99"), 22, mensCategory,
                "https://images.unsplash.com/photo-1551698618-1dfe5d97d256?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80",
                Arrays.asList("https://images.unsplash.com/photo-1551698618-1dfe5d97d256?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80"),
                "Classic denim jacket with vintage wash. Timeless style that goes with everything.",
                Arrays.asList("S", "M", "L", "XL", "XXL"),
                Arrays.asList("Light Blue", "Dark Blue", "Black"),
                true, false, 30);
        }

        if (womensCategory != null) {
            createProduct("Elegant Summer Dress", "elegant-summer-dress",
                new BigDecimal("79.99"), new BigDecimal("99.99"), 20, womensCategory,
                "https://images.unsplash.com/photo-1515372039744-b8f02a3ae446?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80",
                Arrays.asList("https://images.unsplash.com/photo-1515372039744-b8f02a3ae446?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80"),
                "Flowing summer dress perfect for warm weather. Lightweight fabric with beautiful floral patterns.",
                Arrays.asList("XS", "S", "M", "L", "XL"),
                Arrays.asList("Floral Blue", "Floral Pink", "Solid White"),
                true, true, 25);

            createProduct("Silk Blouse", "silk-blouse",
                new BigDecimal("95.99"), new BigDecimal("129.99"), 26, womensCategory,
                "https://images.unsplash.com/photo-1564257577-4d4c6b5b8b8a?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80",
                Arrays.asList("https://images.unsplash.com/photo-1564257577-4d4c6b5b8b8a?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80"),
                "Luxurious silk blouse perfect for professional and formal occasions. Elegant and comfortable.",
                Arrays.asList("XS", "S", "M", "L", "XL"),
                Arrays.asList("White", "Cream", "Navy", "Black"),
                true, true, 15);
        }

        if (footwearCategory != null) {
            createProduct("Premium Sneakers", "premium-sneakers",
                new BigDecimal("129.99"), new BigDecimal("159.99"), 19, footwearCategory,
                "https://images.unsplash.com/photo-1549298916-b41d501d3772?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80",
                Arrays.asList("https://images.unsplash.com/photo-1549298916-b41d501d3772?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80"),
                "High-quality sneakers with superior comfort and style. Perfect for both casual and athletic wear.",
                Arrays.asList("7", "8", "9", "10", "11", "12"),
                Arrays.asList("White", "Black", "Gray", "Navy"),
                true, true, 40);
        }

            System.out.println("Sample products created successfully!");
        } catch (Exception e) {
            System.err.println("ERROR initializing sample products: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createProduct(String name, String slug, BigDecimal price, BigDecimal originalPrice, 
                             int discount, Category category, String image, List<String> images,
                             String description, List<String> sizes, List<String> colors,
                             boolean inStock, boolean featured, int stockQuantity) {
        Product product = new Product();
        product.setName(name);
        product.setSlug(slug);
        product.setPrice(price);
        product.setOriginalPrice(originalPrice);
        product.setDiscount(discount);
        product.setCategory(category);
        product.setImage(image);
        product.setImages(images);
        product.setRating(new BigDecimal("4.5"));
        product.setReviews(100);
        product.setDescription(description);
        product.setSizes(sizes);
        product.setColors(colors);
        product.setInStock(inStock);
        product.setFeatured(featured);
        product.setStockQuantity(stockQuantity);
        product.setActive(true);
        
        productRepository.save(product);
    }
}
