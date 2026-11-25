package com.amillioncodelines.mypocket.walletcore.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.amillioncodelines.mypocket.walletcore.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@IntegrationTest
class PersonalDataStorageTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    @DisplayName("P1, P2: personal data columns exist only in the users table")
    void personalDataOnlyInUsers() {
        var tables = jdbc.queryForList("""
                SELECT DISTINCT table_name FROM information_schema.columns
                WHERE table_schema = 'public' AND column_name IN ('email', 'password_hash')
                """, String.class);

        assertThat(tables).containsExactly("users");
    }

    @Test
    @DisplayName("P2: wallets refer to their owner only through the generated user id")
    void walletsReferToOwnerById() {
        var ownerColumnType = jdbc.queryForObject("""
                SELECT data_type FROM information_schema.columns
                WHERE table_schema = 'public' AND table_name = 'wallets' AND column_name = 'owner_id'
                """, String.class);

        assertThat(ownerColumnType).isEqualTo("uuid");
    }
}
