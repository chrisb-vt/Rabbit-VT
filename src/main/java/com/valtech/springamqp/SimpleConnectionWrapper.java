// Copyright (c) 2011 VocaLink Ltd
package com.valtech.springamqp;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.SimpleConnection;

import com.rabbitmq.client.Channel;


/**
 * 
 * This class wraps the SpringAMQP connection object and allows us to close the connection
 * with a timeout rather than allowing the connection close to hang (which seems to be happening a lot).
 *
 */
public class SimpleConnectionWrapper implements Connection {
    
    private static final Logger logger = LoggerFactory.getLogger(SimpleConnectionWrapper.class);
    
    private static final int TIMEOUT = 5000;
    private final SimpleConnection delegate;
    private final com.rabbitmq.client.Connection nativeConnection;

    /**
     * Default constructor
     * @param connection The native connection
     */
    public SimpleConnectionWrapper(com.rabbitmq.client.Connection connection) {
        delegate = new SimpleConnection(connection);
        nativeConnection = connection;
    }

    @Override
    public Channel createChannel(boolean transactional) throws AmqpException {
        return delegate.createChannel(transactional);
    }

    @Override
    public void close() throws AmqpException {
        try {
            logger.info("Closing connection {} {}", nativeConnection.getAddress(), nativeConnection.getPort());
            nativeConnection.close(TIMEOUT);
        } catch (IOException e) {
            throw new AmqpException(e);
        }
    }

    @Override
    public boolean isOpen() {
        return delegate.isOpen();
    }

}
