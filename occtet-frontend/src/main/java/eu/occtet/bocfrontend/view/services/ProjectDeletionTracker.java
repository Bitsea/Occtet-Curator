/*
 *  Copyright (C) 2025 Bitsea GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      https:www.apache.orglicensesLICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  License-Filename: LICENSE
 *
 *
 */

package eu.occtet.bocfrontend.view.services;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Global tracker for asynchronous project deletion processes.
 * Allows the UI to recover its state and show progress even if the user refreshes the page.
 */
@Component("boc_ProjectDeletionTracker")
public class ProjectDeletionTracker {

    private final AtomicInteger activeDeletions = new AtomicInteger(0);

    /**
     * Marks the beginning of a background deletion process.
     */
    public void start() {
        activeDeletions.incrementAndGet();
    }

    /**
     * Marks the end of a background deletion process.
     */
    public void finish() {
        activeDeletions.decrementAndGet();
    }

    /**
     * Checks if any project deletions are currently in progress.
     * @return true if a deletion is active, false otherwise.
     */
    public boolean isDeleting() {
        return activeDeletions.get() > 0;
    }
}