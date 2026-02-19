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

package eu.occtet.boc.util;

/**
 * The ExternalNotesConstants class defines constant string values used for external notes aka audit notes of an
 * inventory item
 */
public class ExternalNotesConstants {

    public static final String SECTION_SEPARATOR = "\n---\n";

    // general
    public static final String WARNING_AUDITOR_ATTENTION_REQ = "WARNING - Auditor's Attention Required:\n";
    public static final String INFO = "INFO:\n";

    public static final String COPYRIGHT_FILTER_INFO_AI_RESPONSE_MESSAGE = "INFO - AI Copyright Filter response:\n";
    public static final String LICENSE_MATCHER_INFO_AI_RESPONSE_MESSAGE = "INFO - AI License Matcher response:\n";
    // Download service
    public static final String DOWNLOAD_SERVICE_FAILURE_MSG =
            "* Failed to download resources. Please try downloading the files for this software component manually.";

    // License matcher messages
    public static final String LICENSE_URL_NOT_SUCCESSFUL =
            "url not successfully for license: %s / no spdx match possible";

    public static final String LICENSE_TEXT_MATCHED =
            "License %s matches license text";
}
