/*
 *
 *  Copyright (C) 2025 Bitsea GmbH
 *  *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  License-Filename: LICENSE
 * /
 *
 */

package eu.occtet.bocfrontend.user;

import eu.occtet.bocfrontend.entity.User;
import eu.occtet.bocfrontend.test_support.AuthenticatedAsAdmin;
import io.jmix.core.DataManager;
import io.jmix.core.security.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sample integration test for the User entity.
 */
@SpringBootTest
@ExtendWith(AuthenticatedAsAdmin.class)
public class UserTest {

    @Autowired
    DataManager dataManager;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    UserRepository userRepository;

    User savedUser;

    @Test
    void test_saveAndLoad() {
        // Create and save a new User
        User user = dataManager.create(User.class);
        user.setUsername("test-user-" + System.currentTimeMillis());
        user.setPassword(passwordEncoder.encode("test-passwd"));
        savedUser = dataManager.save(user);

        // Check the new user can be loaded
        User loadedUser = dataManager.load(User.class).id(user.getId()).one();
        assertThat(loadedUser).isEqualTo(user);

        // Check the new user is available through UserRepository
        UserDetails userDetails = userRepository.loadUserByUsername(user.getUsername());
        assertThat(userDetails).isEqualTo(user);
    }

    @AfterEach
    void tearDown() {
        if (savedUser != null)
            dataManager.remove(savedUser);
    }
}
