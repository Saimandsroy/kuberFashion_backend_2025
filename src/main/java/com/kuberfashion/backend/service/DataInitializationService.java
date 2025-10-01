package com.kuberfashion.backend.service;

import com.kuberfashion.backend.entity.Category;
import com.kuberfashion.backend.entity.Product;
import com.kuberfashion.backend.entity.User;
import com.kuberfashion.backend.repository.CategoryRepository;
import com.kuberfashion.backend.repository.ProductRepository;
import com.kuberfashion.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Service
public class DataInitializationService implements CommandLineRunner {
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Override
    public void run(String... args) throws Exception {
        if (categoryRepository.count() == 0) {
            initializeCategories();
        }
        if (productRepository.count() == 0) {
            initializeProducts();
        }
        if (userRepository.count() == 0) {
            initializeUsers();
        }
    }
    
    private void initializeCategories() {
        List<Category> categories = Arrays.asList(
            new Category("Men's Fashion", "mens-fashion", 
                "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80",
                "Trendy clothing for modern men"),
            new Category("Women's Fashion", "womens-fashion",
                "./ayo-ogunseinde-6W4F62sN_yI-unsplash.jpg",
                "Elegant styles for every occasion"),
            new Category("Footwear", "footwear",
                "https://images.unsplash.com/photo-1549298916-b41d501d3772?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80",
                "Comfortable and stylish shoes"),
            new Category("Accessories", "accessories",
                "https://images.unsplash.com/photo-1553062407-98eeb64c6a62?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80",
                "Complete your look with accessories"),
            new Category("Kids Fashion", "kids-fashion",
                "https://images.unsplash.com/photo-1503919545889-aef636e10ad4?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80",
                "Cute and comfortable kids wear"),
            new Category("Sports & Active", "sports-active",
                "https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80",
                "Athletic wear for active lifestyle")
        );
        
        categories.get(0).setProductCount(245);
        categories.get(1).setProductCount(312);
        categories.get(2).setProductCount(189);
        categories.get(3).setProductCount(156);
        categories.get(4).setProductCount(98);
        categories.get(5).setProductCount(134);
        
        categoryRepository.saveAll(categories);
    }
    
    private void initializeProducts() {
        Category mensCategory = categoryRepository.findBySlug("mens-fashion").orElse(null);
        Category womensCategory = categoryRepository.findBySlug("womens-fashion").orElse(null);
        Category footwearCategory = categoryRepository.findBySlug("footwear").orElse(null);
        Category accessoriesCategory = categoryRepository.findBySlug("accessories").orElse(null);
        Category kidsCategory = categoryRepository.findBySlug("kids-fashion").orElse(null);
        Category sportsCategory = categoryRepository.findBySlug("sports-active").orElse(null);
        
        List<Product> products = Arrays.asList(
            createProduct(1L, "Classic Cotton T-Shirt", "classic-cotton-tshirt", 
                new BigDecimal("29.99"), new BigDecimal("39.99"), 25, mensCategory,
                "https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80",
                Arrays.asList("https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80",
                           "https://images.unsplash.com/photo-1583743814966-8936f37f4678?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80"),
                new BigDecimal("4.5"), 128, "Premium quality cotton t-shirt with comfortable fit. Perfect for casual wear and everyday comfort.",
                Arrays.asList("S", "M", "L", "XL", "XXL"), Arrays.asList("White", "Black", "Navy", "Gray"), true, true),
            
            createProduct(2L, "Elegant Summer Dress", "elegant-summer-dress",
                new BigDecimal("79.99"), new BigDecimal("99.99"), 20, womensCategory,
                "https://images.unsplash.com/photo-1515372039744-b8f02a3ae446?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80",
                Arrays.asList("https://images.unsplash.com/photo-1515372039744-b8f02a3ae446?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80",
                           "https://images.unsplash.com/photo-1572804013309-59a88b7e92f1?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80"),
                new BigDecimal("4.8"), 89, "Flowing summer dress perfect for warm weather. Lightweight fabric with beautiful floral patterns.",
                Arrays.asList("XS", "S", "M", "L", "XL"), Arrays.asList("Floral Blue", "Floral Pink", "Solid White"), true, true),
            
            createProduct(3L, "Premium Sneakers", "premium-sneakers",
                new BigDecimal("129.99"), new BigDecimal("159.99"), 19, footwearCategory,
                "https://images.unsplash.com/photo-1549298916-b41d501d3772?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80",
                Arrays.asList("https://images.unsplash.com/photo-1549298916-b41d501d3772?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80",
                           "https://images.unsplash.com/photo-1606107557195-0e29a4b5b4aa?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80"),
                new BigDecimal("4.6"), 203, "High-quality sneakers with superior comfort and style. Perfect for both casual and athletic wear.",
                Arrays.asList("7", "8", "9", "10", "11", "12"), Arrays.asList("White", "Black", "Gray", "Navy"), true, true),
            
            createProduct(4L, "Leather Crossbody Bag", "leather-crossbody-bag",
                new BigDecimal("89.99"), new BigDecimal("119.99"), 25, accessoriesCategory,
                "https://images.unsplash.com/photo-1553062407-98eeb64c6a62?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80",
                Arrays.asList("https://images.unsplash.com/photo-1553062407-98eeb64c6a62?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80",
                           "https://images.unsplash.com/photo-1584917865442-de89df76afd3?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80"),
                new BigDecimal("4.7"), 156, "Genuine leather crossbody bag with multiple compartments. Stylish and functional for everyday use.",
                Arrays.asList("One Size"), Arrays.asList("Brown", "Black", "Tan"), true, false),
            
            createProduct(5L, "Kids Rainbow Hoodie", "kids-rainbow-hoodie",
                new BigDecimal("39.99"), new BigDecimal("49.99"), 20, kidsCategory,
                "https://images.unsplash.com/photo-1503919545889-aef636e10ad4?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80",
                Arrays.asList("https://images.unsplash.com/photo-1503919545889-aef636e10ad4?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80",
                           "https://images.unsplash.com/photo-1519238263530-99bdd11df2ea?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80"),
                new BigDecimal("4.4"), 67, "Colorful and comfortable hoodie for kids. Soft fabric with fun rainbow design.",
                Arrays.asList("2T", "3T", "4T", "5T", "6T"), Arrays.asList("Rainbow", "Pink Rainbow", "Blue Rainbow"), true, false),
            
            createProduct(6L, "Athletic Running Shorts", "athletic-running-shorts",
                new BigDecimal("34.99"), new BigDecimal("44.99"), 22, sportsCategory,
                "https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80",
                Arrays.asList("https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80",
                           "https://images.unsplash.com/photo-1544966503-7cc5ac882d5f?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80"),
                new BigDecimal("4.3"), 94, "Lightweight running shorts with moisture-wicking technology. Perfect for workouts and outdoor activities.",
                Arrays.asList("S", "M", "L", "XL"), Arrays.asList("Black", "Navy", "Gray", "Red"), true, true),
            
            createProduct(7L, "Denim Jacket", "denim-jacket",
                new BigDecimal("69.99"), new BigDecimal("89.99"), 22, mensCategory,
                "https://images.unsplash.com/photo-1551698618-1dfe5d97d256?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80",
                Arrays.asList("https://images.unsplash.com/photo-1551698618-1dfe5d97d256?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80",
                           "https://images.unsplash.com/photo-1594633312681-425c7b97ccd1?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80"),
                new BigDecimal("4.5"), 112, "Classic denim jacket with vintage wash. Timeless style that goes with everything.",
                Arrays.asList("S", "M", "L", "XL", "XXL"), Arrays.asList("Light Blue", "Dark Blue", "Black"), true, false),
            
            createProduct(8L, "Silk Blouse", "silk-blouse",
                new BigDecimal("95.99"), new BigDecimal("129.99"), 26, womensCategory,
                "https://images.unsplash.com/photo-1564257577-4d4c6b5b8b8a?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80",
                Arrays.asList("https://images.unsplash.com/photo-1564257577-4d4c6b5b8b8a?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80",
                           "https://images.unsplash.com/photo-1485968579580-b6d095142e6e?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80"),
                new BigDecimal("4.9"), 78, "Luxurious silk blouse perfect for professional and formal occasions. Elegant and comfortable.",
                Arrays.asList("XS", "S", "M", "L", "XL"), Arrays.asList("White", "Cream", "Navy", "Black"), true, true)
        );
        
        productRepository.saveAll(products);
    }
    
    private Product createProduct(Long id, String name, String slug, BigDecimal price, BigDecimal originalPrice, 
                                Integer discount, Category category, String image, List<String> images, 
                                BigDecimal rating, Integer reviews, String description, List<String> sizes, 
                                List<String> colors, boolean inStock, boolean featured) {
        Product product = new Product();
        product.setName(name);
        product.setSlug(slug);
        product.setPrice(price);
        product.setOriginalPrice(originalPrice);
        product.setDiscount(discount);
        product.setCategory(category);
        product.setImage(image);
        product.setImages(images);
        product.setRating(rating);
        product.setReviews(reviews);
        product.setDescription(description);
        product.setSizes(sizes);
        product.setColors(colors);
        product.setInStock(inStock);
        product.setFeatured(featured);
        product.setStockQuantity(inStock ? 50 : 0);
        product.setActive(true);
        return product;
    }
    
    private void initializeUsers() {
        User admin = new User();
        admin.setFirstName("Admin");
        admin.setLastName("User");
        admin.setEmail("admin@kuberfashion.com");
        admin.setPhone("1234567890");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setRole(User.Role.ADMIN);
        admin.setEnabled(true);
        
        User testUser = new User();
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setEmail("test@kuberfashion.com");
        testUser.setPhone("0987654321");
        testUser.setPassword(passwordEncoder.encode("test123"));
        testUser.setRole(User.Role.USER);
        testUser.setEnabled(true);
        
        userRepository.saveAll(Arrays.asList(admin, testUser));
    }
}
