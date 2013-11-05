// Copyright (c) 2012 VocaLink Ltd
package com.valtech.springamqp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.ChannelListener;
import org.springframework.amqp.rabbit.connection.CompositeChannelListener;
import org.springframework.amqp.rabbit.connection.CompositeConnectionListener;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionListener;
import org.springframework.amqp.rabbit.connection.RabbitUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.util.Assert;


/**
 * Abstract connection factory for defining multiple hosts
 * @author Dave Syer
 * @author Dean Bennett
 *
 */
public abstract class AbstractMultiHostConnectionFactory implements ConnectionFactory, DisposableBean {
    private static final String ADDRESS_PORT_SEPARATOR = ":";
    // CHECKSTYLE:OFF  This is a modified version of the AbstractConnectionFactory from spring-amqp 1.0.0.RC1 to enable configuration of multiple hosts

    private static final Logger logger = LoggerFactory.getLogger(AbstractMultiHostConnectionFactory.class);

    private final com.rabbitmq.client.ConnectionFactory rabbitConnectionFactory;

    private final CompositeConnectionListener connectionListener = new CompositeConnectionListener();

    private final CompositeChannelListener channelListener = new CompositeChannelListener();

    private boolean multiHost = false;
    private String hostString;

    /**
     * Create a new SingleConnectionFactory for the given target ConnectionFactory.
     * @param rabbitConnectionFactory the target ConnectionFactory
     */
    public AbstractMultiHostConnectionFactory(com.rabbitmq.client.ConnectionFactory rabbitConnectionFactory) {
        Assert.notNull(rabbitConnectionFactory, "Target ConnectionFactory must not be null");
        this.rabbitConnectionFactory = rabbitConnectionFactory;
    }

    /**
     * Return true if multiple hosts
     * @return true if host string is for multiple hosts
     */
    public boolean isMultiHost() {
        return multiHost;
    }

    /**
     * Set admin username
     * @param username admin username
     */
    public void setUsername(String username) {
        this.rabbitConnectionFactory.setUsername(username);
    }

    /**
     * Set admin password
     * @param password admin password
     */
    public void setPassword(String password) {
        this.rabbitConnectionFactory.setPassword(password);
    }

    /**
     * Set host string
     * @param hostString host string
     */
    public void setHostString(String hostString) {
        this.multiHost = true;
        this.hostString = hostString;
    }

    /**
     * Set hostname or address for single host
     * @param host hostname or address
     */
    public void setHost(String host) {
        this.multiHost = false;
        this.rabbitConnectionFactory.setHost(host);
    }

    /**
     * Get hostname(s) or address(es) for single or multiple hosts
     * @return hostname(s) or address(es) for single or multiple hosts
     */
    public String getHost() {
        if (!isMultiHost()) {
            return rabbitConnectionFactory.getHost();
        } else {
            return hostString;
        }
    }

    /**
     * Set virtual host
     * @param virtualHost virtual host
     */
    public void setVirtualHost(String virtualHost) {
        this.rabbitConnectionFactory.setVirtualHost(virtualHost);
    }

    /**
     * Get virtual host
     * @return virtual host
     */
    public String getVirtualHost() {
        return rabbitConnectionFactory.getVirtualHost();
    }

    /**
     * Set RabbitMQ connection port
     * @param port connection port
     */
    public void setPort(int port) {
        this.rabbitConnectionFactory.setPort(port);
    }

    /**
     * Get RabbitMQ connection port
     * @return RabbitMQ connection port
     */
    public int getPort() {
        return rabbitConnectionFactory.getPort();
    }

    /**
     * A composite connection listener to be used by subclasses when creating and closing connections.
     *
     * @return the connection listener
     */
    protected ConnectionListener getConnectionListener() {
        return connectionListener;
    }

    /**
     * A composite channel listener to be used by subclasses when creating and closing channels.
     *
     * @return the channel listener
     */
    protected ChannelListener getChannelListener() {
        return channelListener;
    }

    /**
     * Set connection listeners
     * @param listeners connection listeners
     */
    public void setConnectionListeners(List<? extends ConnectionListener> listeners) {
        this.connectionListener.setDelegates(listeners);
    }

    /**
     * Add connection listener
     * @param listener connection listener
     */
    public void addConnectionListener(ConnectionListener listener) {
        this.connectionListener.addDelegate(listener);
    }

    /**
     * Set channel listeners
     * @param listeners channel listeners
     */
    public void setChannelListeners(List<? extends ChannelListener> listeners) {
        this.channelListener.setDelegates(listeners);
    }

    /**
     * Add channel listener
     * @param listener channel listener
     */
    public void addChannelListener(ChannelListener listener) {
        this.channelListener.addDelegate(listener);
    }

    /**
     * Create RabbitMQ connection for hostname.  If multi host each address specified will be tried in order specified until connect connnect is created or list axausted
     * @return RabbitMQ connection
     */
    protected final Connection createBareConnection() {
        SimpleConnectionWrapper simpleConnectionWrapper = null;
        try {
            if (isMultiHost()) {
                final String addresses = StringUtils.join(getAddresses(), ", ");
                logger.debug("Connecting to RabbitMQ hosts [" + addresses + "] with user [" + rabbitConnectionFactory.getUsername() + "]");
                return new SimpleConnectionWrapper(this.rabbitConnectionFactory.newConnection(getAddresses()));
            } else {
                logger.debug("Connecting to RabbitMQ host [" + rabbitConnectionFactory.getHost() + ADDRESS_PORT_SEPARATOR
                        + rabbitConnectionFactory.getPort() + "] with user [" + rabbitConnectionFactory.getUsername() + "]");
                simpleConnectionWrapper = new SimpleConnectionWrapper(this.rabbitConnectionFactory.newConnection());
            }
        } catch (IOException e) {
            // Not throwing the exception, because we do want to start the server, even when we do not have
            // rabbit mq up and running.
            logger.error("Error when getting AMQP connection {} {} {}", new Object[]{toString(), e.getClass(), e.getMessage()});
            logger.trace("Exception when getting connection for : " + toString() +
                         ". Full trace follows: \n" +
                         "--------------------------- TRACE START AT CONNECTION FACTORY ---------------------------- \n" +
                         ExceptionUtils.getFullStackTrace(RabbitUtils.convertRabbitAccessException(e)) +
                         " \n--------------------------- TRACE END AT CONNECTION FACTORY ---------------------------- \n"
                        , e);
        }
        return simpleConnectionWrapper;
    }

    /**
     * Get default hostname for local machine
     * @return default hostname for   machine
     */
    protected  final String getDefaultHostName() {
        String temp;
        try {
            InetAddress localMachine = InetAddress.getLocalHost();
            temp = localMachine.getHostName();
            logger.debug("Using hostname [" + temp + "] for hostname.");
        } catch (UnknownHostException e) {
            logger.warn("Could not get hostname {}", e.getMessage());
            logger.trace("Could not get host name, using 'localhost' as default value", e);
            temp = "localhost";
        }
        return temp;
    }

    private com.rabbitmq.client.Address[] getAddresses() {
        return  com.rabbitmq.client.Address.parseAddresses(hostString);
    }

    /**
     * Destroy method, do nothing
     */
    public void destroy() {
    }
    // CHECKSTYLE:ON
}