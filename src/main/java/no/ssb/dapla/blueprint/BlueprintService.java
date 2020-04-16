/*
 * Copyright (c) 2018, 2019 Oracle and/or its affiliates. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package no.ssb.dapla.blueprint;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.helidon.config.Config;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.neo4j.driver.Values.parameters;

public class BlueprintService implements Service {

    private static final Logger LOG = LoggerFactory.getLogger(BlueprintService.class);

    private static final ObjectMapper mapper = new ObjectMapper();
    private final Driver driver;

    BlueprintService(Config config, Driver driver) {
        this.driver = driver;
    }

    @Override
    public void update(Routing.Rules rules) {
        rules
                .put("/rev/{rev}", this::putRevisionHandler)
                .get("/rev/{rev}", this::getRevisionHandler)
        ;
    }

    private void putRevisionHandler(ServerRequest request, ServerResponse response) {
        try (Session session = driver.session()) {
            JsonNode node = mapper.valueToTree(session.writeTransaction(tx -> {
                Result result = tx.run("CREATE (r:GitRevision) SET r.revision = $rev RETURN r",
                        parameters("rev", request.path().param("rev")));
                return result.single().get("r").asNode().asMap();
            }));
            response.status(201).send(node);
        }
    }

    private void getRevisionHandler(ServerRequest request, ServerResponse response) {
        try (Session session = driver.session()) {
            JsonNode node = mapper.valueToTree(session.readTransaction(tx -> {
                Result result = tx.run("MATCH (r:GitRevision {revision: $rev}) RETURN r",
                        parameters("rev", request.path().param("rev")));
                return result.single().get("r").asNode().asMap();
            }));
            response.status(200).send(node);
        }
    }
}
