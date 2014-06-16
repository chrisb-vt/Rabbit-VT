/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.valtech.springamqp;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;

/**
 * Interface defining MultiHostConnectionFactory
 */
public interface MultiHostConnectionFactory extends ConnectionFactory {

    /**
     * Return the brokerAlias if it has been set, otherwise the broker host
     * @return
     */
    String getBrokerAlias();

    /**
     * Set the brokerAlias
     * @param brokerAlias
     */
    void setBrokerAlias(String brokerAlias);

    /**
     * Set addresses for clustering.
     * @param addresses list of addresses with form "host[:port],..."
     */
    void setAddresses(String addresses);
}