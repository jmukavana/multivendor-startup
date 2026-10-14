package com.jamlapps.meridian.catalog.internal;

import com.jamlapps.meridian.catalog.Product;
import com.jamlapps.meridian.catalog.Products;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class ProductsAdapter implements Products {

    private final ProductRepository repository;

    ProductsAdapter(ProductRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Product> findById(UUID productId) {
        return repository.findById(productId).map(ProductEntity::toProduct);
    }
}
