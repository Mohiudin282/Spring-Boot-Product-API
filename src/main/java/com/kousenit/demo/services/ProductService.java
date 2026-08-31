package com.kousenit.demo.services;

import com.kousenit.demo.dto.ProductRequest;
import com.kousenit.demo.dto.ProductResponse;
import com.kousenit.demo.entities.Product;
import com.kousenit.demo.exceptions.InsufficientStockException;
import com.kousenit.demo.exceptions.ProductNotFoundException;
import com.kousenit.demo.exceptions.ProductValidationException;
import com.kousenit.demo.repositories.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;


@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class ProductService {
    private ProductRepository productRepository;

    public ProductResponse getProductById(Long id){
        log.info("Fetching product with id: {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        return ProductResponse.from(product);
    }

    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        log.info("Fetching all products with pagination: {}", pageable);
        return productRepository.findAll(pageable)
                .map(ProductResponse::from);
    }

    public List<ProductResponse> findProductByName(String name){
        log.info("Searching products by name: {}", name);
        return productRepository.findByNameContainingIgnoreCase(name)
                .stream()
                .map(ProductResponse::from)
                .toList();
    }

    public List<ProductResponse> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice){
        if (minPrice.compareTo(maxPrice) > 0){
            throw new IllegalArgumentException("Min price should not be greater than max price");
        }
        return productRepository.findByPriceBetween(minPrice, maxPrice)
                .stream()
                .map(ProductResponse::from)
                .toList();
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest request){
        Product product = new Product();
        product.setName(request.name());
        product.setPrice(request.price());
        product.setDescription(request.description());
        product.setQuantity(request.quantity());
        product.setSku(request.sku());
        product.setContactEmail(request.contactEmail());

        Product savedProduct = productRepository.save(product);
        return ProductResponse.from(savedProduct);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request){
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        if (!product.getSku().equals(request.sku()) && productRepository.existsBySku(request.sku())){
            throw new ProductValidationException(
                    "Sku", request.sku(), "Product with " + request.sku() + "already exist");
        }

        product.setName(request.name());
        product.setPrice(request.price());
        product.setDescription(request.description());
        product.setQuantity(request.quantity());
        product.setSku(request.sku());
        product.setContactEmail(request.contactEmail());

        Product updatedProduct = productRepository.save(product);
        return ProductResponse.from(updatedProduct);
    }

    @Transactional
    public void deleteProduct(Long id){
        if (!productRepository.existsById(id)){
            throw new ProductNotFoundException(id);
        }
        productRepository.deleteById(id);
    }

    @Transactional
    public ProductResponse updateStock(Long id, Integer newQuantity){
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        Integer oldQuantity = product.getQuantity();
        product.setQuantity(newQuantity);
        Product updatedProduct = productRepository.save(product);
        log.info("Stock update for product {}: {} -> {}", id, oldQuantity, newQuantity);
        return ProductResponse.from(updatedProduct);
    }

    @Transactional
    public ProductResponse reserveStock(Long id, Integer quantity){
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        if (!product.hasStock(quantity)){
            throw new InsufficientStockException(id, quantity, product.getQuantity());
        }

        product.decrementStock(quantity);
        Product updatedProduct = productRepository.save(product);
        log.info("Reserved {} units of product {}. Remaining stock: {}",
                quantity, id, updatedProduct.getQuantity());
        return ProductResponse.from(updatedProduct);
    }

    @Transactional
    public ProductResponse addStock(Long id, Integer quantity){
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        if (quantity <= 0 ){
            throw new IllegalArgumentException("Quantity to add must be positive");
        }

        product.incrementStock(quantity);
        Product updatedProduct = productRepository.save(product);
        log.info("Added {} units to product {}. New stock: {}",
                quantity, id, updatedProduct.getQuantity());
        return ProductResponse.from(updatedProduct);
    }

    public List<ProductResponse> getLowStockProducts(Integer threshold){
        return productRepository.findLowStockProducts(threshold)
                .stream()
                .map(ProductResponse::from)
                .toList();
    }

    public List<ProductResponse> getExpensiveProducts(BigDecimal minPrice){
        return productRepository.findExpensiveProducts(minPrice)
                .stream()
                .map(ProductResponse::from)
                .toList();
    }

    public long count(){
        return productRepository.count();
    }
}
