/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.testing.system.fixtures.connectors;

import io.debezium.testing.system.tools.kafka.ConnectorConfigBuilder;

public interface ConnectorRuntimeFixture {

    ConnectorConfigBuilder getConnectorConfig();

    void setConnectorConfig(ConnectorConfigBuilder config);
}
