package com.igloosec.util.network;

import com.igloosec.util.common.KeyStrings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

public class ContentReader {

    private final Logger log = LogManager.getLogger( ContentReader.class );

    private final StringBuilder stringBuilder;

    public ContentReader() { this.stringBuilder = new StringBuilder(); }

    /**
     * 해당 URL 에 HTTP 로 연결한다.
     * @param url URL
     * @return connection
     */
    private HttpURLConnection connect( String url ) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL( url ).openConnection();
        } catch ( IOException ex ) {
            log.error( ex );
        }
        return connection;
    }

    /**
     * 해당 URL 에 HTTPS 로 연결한다.
     * @param url URL
     * @return connection
     */
    private HttpsURLConnection connectWithSSL( String url ) {
        HttpsURLConnection connection = null;
        try {
            TrustManager[] trustManagers = new TrustManager[]{new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }};
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustManagers, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory( sslContext.getSocketFactory() );
            connection = (HttpsURLConnection) new URL( url ).openConnection();
        } catch ( NoSuchAlgorithmException ex ) {
            log.error( ex );
        } catch ( KeyManagementException ex ) {
            log.error( ex );
        } catch ( MalformedURLException ex ) {
            log.error( ex );
        } catch ( IOException ex ) {
            log.error( ex );
        }
        return connection;
    }

    /**
     * 해당 URL 의 파일을 다운로드 받는다.
     * @param fileURL 다운로드할 파일 URL
     * @param filePath 저장할 파일 경로
     * @param isSSL SSL 사용 여부
     */
    public void downloadFile( String fileURL, String filePath, boolean isSSL ) {
        try {
            InputStream inputStream = null;
            if( isSSL ) {
                HttpsURLConnection connection = connectWithSSL( fileURL );
                inputStream = connection.getInputStream();
            } else {
                HttpURLConnection connection = connect( fileURL );
                inputStream = connection.getInputStream();
            }
            OutputStream outputStream = new FileOutputStream( filePath + fileURL.substring( fileURL.lastIndexOf( "/" ) ) );
            final byte[] bytes = new byte[2048];
            int length;
            while( (length = inputStream.read( bytes )) != -1 ) {
                outputStream.write( bytes, 0, length );
            }
            if( outputStream != null ) outputStream.close();
            if( inputStream != null ) inputStream.close();
        } catch ( IOException ex ) {
            log.error( ex );
        }
    }

    /**
     * 해당 URL 의 HTTP 컨텐츠를 저장한다.
     * @param contentURL 대상 URL
     * @param isSSL SSL 사용 여부
     * @return HTTP 컨텐츠
     */
    public String getContent( String contentURL, boolean isSSL ) {
        BufferedReader reader = null;
        try {
            if( isSSL ) {
                HttpsURLConnection connection = connectWithSSL( contentURL );
                reader = new BufferedReader( new InputStreamReader( connection.getInputStream(), KeyStrings.DEFAULT_FILE_ENCODING ) );
            } else {
                HttpURLConnection connection = connect( contentURL );
                reader = new BufferedReader( new InputStreamReader( connection.getInputStream(), KeyStrings.DEFAULT_FILE_ENCODING ) );
            }
            String line;
            while( (line = reader.readLine() ) != null ) {
                stringBuilder.append( line ).append( System.lineSeparator() );
            }
        } catch ( IOException ex ) {
            log.error( ex );
        } finally {
            try {
                String result = stringBuilder.toString();
                stringBuilder.setLength( 0 );
                if( reader != null ) reader.close();
                return result;
            } catch ( IOException ex ) {
                log.error( ex );
            }
        }
        return null;
    }
}
