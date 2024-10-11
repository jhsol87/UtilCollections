package com.igloosec.util.jmx;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

public class JMXReader extends TimerTask {

    private final Logger log = LogManager.getLogger( JMXReader.class );

    private static final int MAX_RETRY_COUNT = 10;

    private final Connector connector;

    private final AtomicBoolean isConnected = new AtomicBoolean();

    public JMXReader( String jmxURL ) {
        this.connector = new Connector( jmxURL );

        this.isConnected.set( false );
    }

    /**
     * JMX 에 연결한다.
     * 연결이 되면 필요한 데이터를 가져온다.
     * 연결이 안되면 MAX_RETRY_COUNT 만큼 5초마다 재시도한다.
     */
    private void connect() {
        if( isConnected.get() ) {
            getUsage();
        } else {
            int retryCount = 0;
            while( retryCount < MAX_RETRY_COUNT ) {
                isConnected.set( this.connector.connect() );
                if( !isConnected.get() ) {
                    retryCount++;
                    log.info( "Retry to connect...{}", retryCount );
                    try {
                        Thread.sleep( 5 * 1000 );
                    } catch ( InterruptedException ex ) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    log.info( "Connected..." );
                    break;
                }
            }
            if( retryCount == MAX_RETRY_COUNT ) log.info( "Exceeded max retry count..." );
        }
    }

    /**
     * 필요한 데이터를 가져온다.
     */
    private void getUsage() {
        try {
            log.info( "Thread: {}", connector.getThreadCount() );
            log.info( "CPU: {}%", connector.getCPUUsage() );
            log.info( "MEM: {}%", connector.getMemoryUsage() );
        } catch ( IOException ex ) {
            log.error( ex );
        }
    }

    @Override
    public void run() { connect(); }
}
