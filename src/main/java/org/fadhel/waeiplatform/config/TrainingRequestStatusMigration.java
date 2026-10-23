package org.fadhel.waeiplatform.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class TrainingRequestStatusMigration implements CommandLineRunner {

    private static final String STATUS_CHECK_CONSTRAINT = "training_requests_chk_1";
    private static final String STATUS_CHECK = "CHECK (status IN ("
            + "'PENDING','ACCEPTED','REJECTED','PAYMENT_PENDING',"
            + "'CONFIRMED','IN_PROGRESS','COMPLETED','CANCELLED'))";

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    @Override
    public void run(String... args) throws SQLException {
        boolean mysql = isMySql();

        if (mysql) {
            dropLegacyStatusCheckConstraints();
        }

        int updated = jdbcTemplate.update(
                "UPDATE training_requests SET status = ? WHERE status = ?",
                "CONFIRMED", "PAID");

        if (mysql) {
            ensureStatusCheckConstraint();
        }

        if (updated > 0) {
            log.info("Migrated {} training requests from PAID to CONFIRMED", updated);
        }
    }

    private boolean isMySql() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            return connection.getMetaData().getDatabaseProductName()
                    .toLowerCase()
                    .contains("mysql");
        }
    }

    private void dropLegacyStatusCheckConstraints() {
        List<String> constraintNames = jdbcTemplate.queryForList("""
                SELECT tc.constraint_name
                FROM information_schema.table_constraints tc
                JOIN information_schema.check_constraints cc
                  ON cc.constraint_schema = tc.constraint_schema
                 AND cc.constraint_name = tc.constraint_name
                WHERE tc.table_schema = DATABASE()
                  AND tc.table_name = 'training_requests'
                  AND tc.constraint_type = 'CHECK'
                  AND cc.check_clause LIKE '%PAID%'
                """, String.class);

        for (String constraintName : constraintNames) {
            jdbcTemplate.execute("ALTER TABLE training_requests DROP CHECK `" + constraintName + "`");
            log.info("Dropped legacy training request status constraint {}", constraintName);
        }
    }

    private void ensureStatusCheckConstraint() {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.table_constraints
                WHERE table_schema = DATABASE()
                  AND table_name = 'training_requests'
                  AND constraint_name = ?
                  AND constraint_type = 'CHECK'
                """, Integer.class, STATUS_CHECK_CONSTRAINT);

        if (count != null && count > 0) {
            return;
        }

        jdbcTemplate.execute(
                "ALTER TABLE training_requests ADD CONSTRAINT "
                        + STATUS_CHECK_CONSTRAINT + " " + STATUS_CHECK);
        log.info("Created training request status constraint {}", STATUS_CHECK_CONSTRAINT);
    }
}
