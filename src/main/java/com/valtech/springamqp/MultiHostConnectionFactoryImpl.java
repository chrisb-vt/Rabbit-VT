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

import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
/**
 * A {@link org.springframework.amqp.rabbit.connection.CachingConnectionFactory} implementation.
 *
 * @author Mark Fisher
 * @author Mark Pollack
 * @author Dave Syer
 * @author Dean Bennett
 */
public class MultiHostConnectionFactoryImpl extends CachingConnectionFactory implements MultiHostConnectionFactory {
    // This is a modified version of the CachingConnectionFactory from spring-amqp 1.2.0.RELEASE to allow the addition of a broker alias

    /** Displayable name of the broker **/
    private String brokerAlias;

    /**
     * Default Constructor
     */
    public MultiHostConnectionFactoryImpl() {}

    /**
     * Create a new MultiHostConnectionFactoryImpl for the given host and port.
     *
     * @param host the target rabbit server
     * @param port the target port
     */
    public MultiHostConnectionFactoryImpl(String host, int port)
    {
      super(host, port);
    }    
    
    /**
     * Create a new MultiHostConnectionFactoryImpl for the given target ConnectionFactory.
     *
     * @param rabbitConnectionFactory the target ConnectionFactory
     */
    public MultiHostConnectionFactoryImpl(com.rabbitmq.client.ConnectionFactory rabbitConnectionFactory) {
        super(rabbitConnectionFactory);
    }

    /**
     * Set addresses for clustering.
     * @param addresses list of addresses with form "host[:port],..."
     */
    public void setAddresses(String addresses)
    {
      super.setAddresses(addresses);
      String firstAddress = addresses.split(",")[0];
      String[] addressParts = firstAddress.split(":");
      String firstHost = addressParts[0];
      
      int firstPort = 0;
      if (addressParts.length > 1) {
        firstPort = Integer.parseInt(addressParts[1]);
      }
      super.setHost(firstHost);
      super.setPort(firstPort);
    }
    
    /**
     * {@inheritdoc }
     */
    public String getBrokerAlias() {
        return brokerAlias != null ? brokerAlias : this.getHost();
    }

    /**
     * {@inheritdoc }
     */
    public void setBrokerAlias(String brokerAlias) {
        this.brokerAlias = brokerAlias;
    }

    @Override
    public String toString() {
        return "MultiHostConnectionFactoryImpl [host=" + getHost() + ", port=" + getPort() + "]";
    }
    
}
