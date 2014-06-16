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
