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
    // This is a modified version of the CachingConnectionFactory from spring-amqp 1.1.4.RELEASE to allow the addition of a broker alias

    /** Displayable name of the broker **/
    private String brokerAlias;

    /**
     * Default Constructor
     */
    public MultiHostConnectionFactoryImpl() {
        super();
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
