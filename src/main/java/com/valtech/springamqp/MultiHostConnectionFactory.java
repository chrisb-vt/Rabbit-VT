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