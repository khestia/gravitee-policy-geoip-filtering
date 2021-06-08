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
package io.gravitee.policy.geoipfiltering;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.util.Maps;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.policy.api.PolicyChain;
import io.gravitee.policy.api.PolicyResult;
import io.gravitee.policy.api.annotations.OnRequest;
import io.gravitee.policy.geoipfiltering.configuration.GeoIPFilteringPolicyConfiguration;
import io.gravitee.policy.geoipfiltering.configuration.Rule;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

import java.util.function.Predicate;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GeoIPFilteringPolicy {

    private final static String GEOIP_SERVICE = "service:geoip";

    private static final String GEOIP_FILTERING_UNKNOWN = "GEOIP_FILTERING_UNKNOWN";
    private static final String GEOIP_FILTERING_INVALID = "GEOIP_FILTERING_INVALID";

    private final GeoIPFilteringPolicyConfiguration configuration;

    public GeoIPFilteringPolicy(GeoIPFilteringPolicyConfiguration configuration) {
        this.configuration = configuration;
    }

    @OnRequest
    public void onRequest(Request request, Response response, ExecutionContext context, PolicyChain policyChain) {
        Vertx vertx = context.getComponent(Vertx.class);

        vertx.eventBus().request(GEOIP_SERVICE, request.remoteAddress(), new Handler<AsyncResult<Message<JsonObject>>>() {
            @Override
            public void handle(AsyncResult<Message<JsonObject>> message) {
                if (message.failed()) {
                    if (configuration.isFailOnUnknown()) {
                        policyChain.failWith(PolicyResult.failure(
                                GEOIP_FILTERING_UNKNOWN,
                                HttpStatusCode.FORBIDDEN_403,
                                "You're not allowed to access this resource",
                                Maps.<String, Object>builder()
                                        .put("remote_address", request.remoteAddress())
                                        .build()));
                    } else {
                        policyChain.doNext(request, response);
                    }
                } else {
                    JsonObject geoData = message.result().body();

                    boolean match = compare(geoData);

                    if (match) {
                        policyChain.doNext(request, response);
                    } else {
                        policyChain.failWith(PolicyResult.failure(
                                GEOIP_FILTERING_INVALID,
                                HttpStatusCode.FORBIDDEN_403,
                                "You're not allowed to access this resource",
                                Maps.<String, Object>builder()
                                        .put("remote_address", request.remoteAddress())
                                        .put("country_iso_code", geoData.getString("country_iso_code"))
                                        .put("country_name", geoData.getString("country_name"))
                                        .put("region_name", geoData.getString("region_name"))
                                        .put("city_name", geoData.getString("city_name"))
                                        .put("timezone", geoData.getString("timezone"))
                                        .build()));
                    }
                }
            }
        });
    }

    private boolean compare(JsonObject geoData) {
        if (configuration.getWhitelistRules() != null) {
            return configuration.getWhitelistRules().stream().anyMatch(new Predicate<Rule>() {
                @Override
                public boolean test(Rule rule) {
                    switch (rule.getType()) {
                        case COUNTRY:
                            return compareCountry(geoData, rule);
                        case DISTANCE:
                            return compareDistance(geoData, rule);
                    }

                    return false;
                }
            });
        }

        return true;
    }

    private boolean compareCountry(JsonObject geoData, Rule rule) {
        return geoData != null &&
                geoData.getString("country_iso_code") != null &&
                geoData.getString("country_iso_code").equals(rule.getCountry());
    }

    private boolean compareDistance(JsonObject geoData, Rule rule) {
        Double reqLatitude = geoData.getDouble("lat");
        Double reqLongitude = geoData.getDouble("lon");

        if (reqLatitude == null || reqLongitude == null) {
            return false;
        }

        double latitude = rule.getLatitude();
        double longitude = rule.getLongitude();

        double distance = distance(latitude, reqLatitude, longitude, reqLongitude);

        return distance < rule.getDistance();
    }

    private static double distance(double lat1,
                                   double lat2, double lon1,
                                   double lon2) {
        lon1 = Math.toRadians(lon1);
        lon2 = Math.toRadians(lon2);
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);

        // Haversine formula
        double dlon = lon2 - lon1;
        double dlat = lat2 - lat1;
        double a = Math.pow(Math.sin(dlat / 2), 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.pow(Math.sin(dlon / 2),2);

        double c = 2 * Math.asin(Math.sqrt(a));

        // calculate the result in kilometers (6371 = Radius of earth in kilometers)
        return(c * 6371 * 1000);
    }
}
