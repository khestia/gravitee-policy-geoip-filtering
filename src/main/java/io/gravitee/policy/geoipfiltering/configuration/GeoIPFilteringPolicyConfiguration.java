/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.policy.geoipfiltering.configuration;

import io.gravitee.policy.api.PolicyConfiguration;

import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GeoIPFilteringPolicyConfiguration implements PolicyConfiguration {

    private boolean failOnUnknown = true;

    private List<Rule> whitelistRules;

    public boolean isFailOnUnknown() {
        return failOnUnknown;
    }

    public void setFailOnUnknown(boolean failOnUnknown) {
        this.failOnUnknown = failOnUnknown;
    }

    public List<Rule> getWhitelistRules() {
        return whitelistRules;
    }

    public void setWhitelistRules(List<Rule> whitelistRules) {
        this.whitelistRules = whitelistRules;
    }
}
