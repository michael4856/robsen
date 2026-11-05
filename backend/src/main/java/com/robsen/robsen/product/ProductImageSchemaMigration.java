package com.robsen.robsen.product;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Hibernate's update mode does not reliably widen an existing PostgreSQL
 * varchar column when the entity changes to TEXT. Run this idempotent schema
 * migration after Hibernate has initialized the tables and before the app is
 * marked ready to accept traffic.
 */
@Component
public class ProductImageSchemaMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public ProductImageSchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        var columnTypes = jdbcTemplate.query(
                "SELECT data_type FROM information_schema.columns "
                        + "WHERE table_schema = current_schema() "
                        + "AND table_name = 'product' AND column_name = 'image_url'",
                (result, rowNumber) -> result.getString("data_type"));

        if (columnTypes.isEmpty()) {
            throw new IllegalStateException("The product.image_url column was not created.");
        }
        if ("character varying".equals(columnTypes.getFirst())) {
            jdbcTemplate.execute("ALTER TABLE product ALTER COLUMN image_url TYPE TEXT");
        }
    }
}
