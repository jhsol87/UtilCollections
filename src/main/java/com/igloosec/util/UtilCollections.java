package com.igloosec.util;

import com.igloosec.util.file.FileReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public enum UtilCollections {
    INSTANCE;

    private static final Logger log = LogManager.getLogger( UtilCollections.class );

    private static final String TARGET_INPUT_PATH = "D:\\owl\\Owl";
    private static final String TARGET_OUTPUT_PATH = "D:\\tmp\\Owl";

    private final String[] targetList = { "import org.elasticsearch" };
    private final String[] endWithList = { "java" };

    private final FileReader fileReader;

    UtilCollections() {
        this.fileReader = new FileReader();
    }

    private void runFileManager() {
        // 해당 경로의 파일 중 찾고자 하는 문자열이 포함된 파일을 찾아 지정된 경로에 저장한다.
        fileReader.searchKeywordInFileDirectory( targetList, TARGET_INPUT_PATH, TARGET_OUTPUT_PATH, endWithList );
        fileReader.cleanUnnecessaryFiles( TARGET_OUTPUT_PATH, 0 );
    }

    public static void main( String[] args ) {
        try {
            UtilCollections.INSTANCE.runFileManager();
        } catch ( Throwable t ) {
            log.error( t );
        }
    }
}
