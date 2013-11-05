// Copyright (c) 2012 VocaLink Ltd
package com.valtech.springamqp;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionListener;
import org.springframework.amqp.rabbit.connection.ConnectionProxy;
import org.springframework.amqp.rabbit.connection.RabbitUtils;
import org.springframework.util.StringUtils;

import com.rabbitmq.client.Address;
import com.rabbitmq.client.Channel;

/**
 * A {@link org.springframework.amqp.rabbit.connection.ConnectionFactory} implementation that returns the same Connections from all {@link #createConnection()}
 * calls, and ignores calls to {@link com.rabbitmq.client.Connection#close()}.
 *
 * @author Mark Fisher
 * @author Mark Pollack
 * @author Dave Syer
 * @author Dean Bennett
 */
public class MultiHostConnectionFactory extends AbstractMultiHostConnectionFactory {
    // CHECKSTYLE:OFF  This is a modified version of the ConnectionFactory from spring-amqp 1.0.0.RC1 to enable configuration of multiple hosts

    private static final Logger logger = LoggerFactory.getLogger(MultiHostConnectionFactory.class);

    /** Proxy Connection */
    private SharedConnectionProxy connection;

    /** Synchronization monitor for the shared Connection */
    private volatile Object connectionMonitor = new Object();
    
    /** Displayable name of the broker **/
    private String brokerAlias;

    /**
     * Create a new MultiHostConnectionFactory initializing the hostname to be the value returned from
     * InetAddress.getLocalHost(), or "localhost" if getLocalHost() throws an exception.
     */
    public MultiHostConnectionFactory() {
        this((String) null, false);
    }

    /**
     * Create a new MultiHostConnectionFactory given a host name.
     * @param port the port to connect to
     */
    public MultiHostConnectionFactory(int port) {
        this(null, port, false);
    }

    /**
     * Create a new SingleConnectionFactory given a host name.
     * @param hostString the host name or host addresses to connect to
     * @param multiHost set flag to indicate multi host string
     */
    public MultiHostConnectionFactory(String hostString, Boolean multiHost) {
        this(hostString, com.rabbitmq.client.ConnectionFactory.DEFAULT_AMQP_PORT, multiHost);
    }

    /**
     * Create a new SingleConnectionFactory given a host name.
     * @param hostString the host name or host addresses to connect to
     * @param multiHostString set flag to indicate multi host string
     */
    public MultiHostConnectionFactory(String hostString, String multiHostString) {
        this(hostString, com.rabbitmq.client.ConnectionFactory.DEFAULT_AMQP_PORT, multiHostString != null && "TRUE".equalsIgnoreCase(multiHostString));
    }

    /**
     * Create a new SingleConnectionFactory given a host name.
     * @param hostString the host name or host addresses to connect to
     * @param port the port number to connect to
     * @param multiHost set flag to indicate multi host string
     */
    public MultiHostConnectionFactory(String hostString, int port, Boolean multiHost) {
        super(new com.rabbitmq.client.ConnectionFactory());
        String hosts = hostString;
        int serverPort = port;
        if (!StringUtils.hasText(hostString)) {
            hosts = getDefaultHostName();
        }  else {
            if (multiHost) {
                setHostString(hosts);
            } else {
                Address[] addresses = Address.parseAddresses(hostString);
                if (addresses.length > 1) {
                    logger.warn("Multihost false but list of brokers supplied {} {}", hosts, addresses[0].getHost());
                }
                setHost(addresses[0].getHost());
                if (addresses[0].getPort() != -1) {
                    serverPort = addresses[0].getPort();
                }
            }
        }
        setPort(serverPort);
    }

    /**
     * Create a new SingleConnectionFactory for the given target ConnectionFactory.
     * @param rabbitConnectionFactory the target ConnectionFactory
     */
    public MultiHostConnectionFactory(com.rabbitmq.client.ConnectionFactory rabbitConnectionFactory) {
        super(rabbitConnectionFactory);
    }

    /**
     * Set connection listeners
     * @param listeners connection listeners
     */
    public void setConnectionListeners(List<? extends ConnectionListener> listeners) {
        super.setConnectionListeners(listeners);
        // If the connection is already alive we assume that the new listeners want to be notified
        if (this.connection != null) {
            this.getConnectionListener().onCreate(this.connection);
        }
    }

    /**
     * Add connection listeners
     * @param listener connection listener
     */
    public void addConnectionListener(ConnectionListener listener) {
        super.addConnectionListener(listener);
        // If the connection is already alive we assume that the new listener wants to be notified
        if (this.connection != null) {
            listener.onCreate(this.connection);
        }
    }

    /**
     * Create RabbitMQ connection
     * @return connection RabbitMQ connection
     * @throws AmqpException error establishing connection
     */
    public Connection createConnection() throws AmqpException {
        synchronized (this.connectionMonitor) {
            if (this.connection == null) {
                Connection target = doCreateConnection();
                if(target != null){
                    // do not create a connection proxy of there is no connection
                    this.connection = new SharedConnectionProxy(target);
                    // invoke the listener *after* this.connection is assigned
                    getConnectionListener().onCreate(target);
                }
            }
        }
        return this.connection;
    }

    /**
     * Close the underlying shared connection. The provider of this ConnectionFactory needs to care for proper shutdown.
     * <p>
     * As this bean implements DisposableBean, a bean factory will automatically invoke this on destruction of its
     * cached singletons.
     */
    public final void destroy() {
        synchronized (this.connectionMonitor) {
            if (connection != null) {
                this.connection.destroy();
                this.connection = null;
            }
        }
    }

    /**
     * Create a Connection. This implementation just delegates to the underlying Rabbit ConnectionFactory. Subclasses
     * typically will decorate the result to provide additional features.
     *
     * @return the new Connection
     */
    protected Connection doCreateConnection() {
        return createBareConnection();
    }

    /**
     * Return the brokerAlias if it has been set, otherwise the broker host
     * @return
     */
    public String getBrokerAlias() {
        return brokerAlias != null ? brokerAlias : this.getHost();
    }

    public void setBrokerAlias(String brokerAlias) {
        this.brokerAlias = brokerAlias;
    }

    @Override
    public String toString() {
        return "MultiHostConnectionFactory [host=" + getHost() + ", port=" + getPort() + "]";
    }

    /**
     * Wrap a raw Connection with a proxy that delegates every method call to it but suppresses close calls. This is
     * useful for allowing application code to handle a special framework Connection just like an ordinary Connection
     * from a Rabbit ConnectionFactory.
     */
    private class SharedConnectionProxy implements Connection, ConnectionProxy {

        private volatile Connection target;

        public SharedConnectionProxy(Connection target) {
            this.target = target;
        }

        public Channel createChannel(boolean transactional) {
            if (!isOpen()) {
                synchronized (this) {
                    if (!isOpen()) {
                        logger.debug("Detected closed connection. Opening a new one before creating Channel.");
                        target = createBareConnection();
                        if(target == null){
                            // When rabbit mq, is not up and running, we will have no connection or channel.
                            // We want to avoid null connection, trying to create a channel here
                            // We do want the server to come up, even if rabbit mq is not up and running.
                            logger.error("No connection to RabbitMQ {}", toString());
                            return null;
                        }
                        getConnectionListener().onCreate(target);
                    }
                }
            }
            Channel channel = target.createChannel(transactional);
            getChannelListener().onCreate(channel, transactional);
            return channel;
        }

        public void close() {
        }

        public void destroy() {
            if (this.target != null) {
                getConnectionListener().onClose(target);
                RabbitUtils.closeConnection(this.target);
            }
            this.target = null;
        }

        public boolean isOpen() {
            return target != null && target.isOpen();
        }

        public Connection getTargetConnection() {
            return target;
        }

        
        @Override
        public int hashCode() {
            return 31 + ((target == null) ? 0 : target.hashCode());
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            SharedConnectionProxy other = (SharedConnectionProxy) obj;
            if (target == null) {
                if (other.target != null) {
                    return false;
                }
            } else if (!target.equals(other.target)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "Shared Rabbit Connection: " + this.target;
        }

    }
    // CHECKSTYLE:ON
}