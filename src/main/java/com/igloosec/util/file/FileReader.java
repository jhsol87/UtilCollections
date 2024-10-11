package com.igloosec.util.file;

import com.igloosec.util.common.KeyStrings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class FileReader {

    private final Logger log = LogManager.getLogger( FileReader.class );

    private final StringBuilder stringBuilder;

    public FileReader() { this.stringBuilder = new StringBuilder(); }

    /**
     * 해당 경로의 파일명에 찾을 문자열이 포함된 파일을 삭제한다.
     * @param path 탐색 경로
     * @param target 찾을 문자열
     */
    public void cleanUnnecessaryFiles( String path, String target ) {
        File dir = new File( path );
        File[] fileList = dir.listFiles();
        for( File file : fileList ) {
            if( file.isFile() ) {
                try {
                    if( !file.getName().matches( ".*" + target + ".*" ) ) continue;
                    else {
                        if( file.delete() ) log.info( file.getCanonicalPath() + " is deleted..." );
                        else log.info( file.getCanonicalPath() + " cannot be deleted..." );
                    }
                } catch ( IOException ex ) {
                    log.error( ex );
                }
            } else {
                try {
                    cleanUnnecessaryFiles( file.getCanonicalPath(), target );
                } catch ( IOException ex ) {
                    log.error( ex );
                }
            }
        }
    }

    /**
     * 해당 경로의 지정된 크기 미만의 파일을 삭제한다.
     * @param path 탐색 경로
     * @param size 삭제할 파일 크기
     */
    public void cleanUnnecessaryFiles( String path, long size ) {
        File dir = new File( path );
        File[] fileList = dir.listFiles();
        for( File file : fileList ) {
            if( file.isFile() ) {
                try {
                    if( file.length() > size ) continue;
                    else {
                        if( file.delete() ) log.info( file.getCanonicalPath() + " is deleted..." );
                        else log.info( file.getCanonicalPath() + " cannot be deleted..." );
                    }
                } catch ( IOException ex ) {
                    log.error( ex );
                }
            } else {
                try {
                    cleanUnnecessaryFiles( file.getCanonicalPath(), size );
                } catch ( IOException ex ) {
                    log.error( ex );
                }
            }
        }
    }

    /**
     * 대상 텍스트로부터 시작 문자열과 종료 문자열 사이의 내용을 추출한다.
     * @param target 대상 텍스트
     * @param start 시작 문자열
     * @param containStart 시작 문자열 포함 여부
     * @param end 종료 문자열
     * @param containEnd 종료 문자열 포함 여부
     * @param output 결과 저장 경로 및 파일명
     */
    public void extractContent( String target, String start, boolean containStart, String end, boolean containEnd, String output ) {
        BufferedWriter writer = null;
        int start_idx = 0;
        int end_idx = 0;
        try {
            writer = new BufferedWriter( new OutputStreamWriter( new FileOutputStream( output ), KeyStrings.DEFAULT_FILE_ENCODING ) );
        } catch ( UnsupportedEncodingException ex ) {
            log.error( ex );
        } catch ( FileNotFoundException ex ) {
            log.error( ex );
        }
        while( ( start_idx = target.indexOf( start, start_idx ) ) != -1 ) {
            end_idx = target.indexOf( end, start_idx );
            String content = "";
            if( containStart && containEnd ) content = target.substring( start_idx, end_idx ) + end;
            else if( containStart && !containEnd ) content = target.substring( start_idx, end_idx );
            else if( !containStart && containEnd ) content = target.substring( start_idx + start.length(), end_idx );
            else content = target.substring( start_idx + start.length(), end_idx );
            try {
                writer.write( content );
                writer.newLine();
                writer.flush();
            } catch ( IOException ex ) {
                log.error( ex );
            }
            start_idx = end_idx;
        }
        try {
            if( writer != null ) writer.close();
        } catch ( IOException ex ) {
            log.error( ex );
        }
    }

    /**
     * 해당 경로의 파일의 내용을 읽어 결과 파일에 합친다.
     * 설정된 확장자 기준으로 파일을 확인한다.
     * @param path 탐색 경로
     * @param output 결과 파일
     * @param endWithList 확장자 list
     */
    public void mergeFileInDirectory( String path, String output, String[] endWithList ) {
        List<File> fileList = readDirectory( path, endWithList, new ArrayList<>() );
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter( new OutputStreamWriter( new FileOutputStream( output ), KeyStrings.DEFAULT_FILE_ENCODING ) );
            for( File file : fileList ) stringBuilder.append( readFileByLine( file ) ).append( System.lineSeparator() );
        } catch ( IOException ex ) {
            log.error( ex );
        } finally {
            try {
                writer.write( stringBuilder.toString() );
                writer.flush();
                stringBuilder.setLength( 0 );
                if( writer != null ) writer.close();
            } catch ( IOException ex ) {
                log.error( ex );
            }
        }
    }

    /**
     * 대상 파일의 내용을 읽는다.
     * @param file 대상 파일
     * @return 내용
     */
    public String readFileByLine( File file ) {
        BufferedReader reader = null;
        try {
            log.info( "Now read " + file.getCanonicalPath() + "..." );
            reader = new BufferedReader( new InputStreamReader( new FileInputStream( file ), KeyStrings.DEFAULT_FILE_ENCODING ) );
            String line;
            while( ( line = reader.readLine() ) != null ) stringBuilder.append( line ).append( System.lineSeparator() );
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

    /**
     * 대상 파일의 내용을 읽는다.
     * @param file 대상 파일
     * @param outputPath 결과 저장 경로
     */
    public void readFileByLine( File file, String outputPath ) {
        BufferedReader reader = null;
        BufferedWriter writer = null;
        try {
            log.info( "Now read " + file.getCanonicalPath() + "..." );
            reader = new BufferedReader( new InputStreamReader( new FileInputStream( file ), KeyStrings.DEFAULT_FILE_ENCODING ) );
            writer = new BufferedWriter( new OutputStreamWriter( new FileOutputStream( outputPath + "/" + file.getName() ), KeyStrings.DEFAULT_FILE_ENCODING ) );
            String line;
            while( ( line = reader.readLine() ) != null ) stringBuilder.append( line ).append( System.lineSeparator() );
        } catch ( IOException ex ) {
            log.error( ex );
        } finally {
            try {
                writer.write( stringBuilder.toString() );
                writer.flush();
                stringBuilder.setLength( 0 );
                if( reader != null ) reader.close();
                if( writer != null ) writer.close();
            } catch ( IOException ex ) {
                log.error( ex );
            }
        }
    }

    /**
     * 해당 경로의 하위 폴더까지 재귀적으로 탐색하면서 파일의 내용을 읽는다.
     * 설정된 확장자 기준으로 파일을 확인한다.
     * @param inputPath 탐색 경로
     * @param outputPath 결과 저장 경로
     * @param endWithList 확장자 list
     */
    public void readFileDirectory( String inputPath, String outputPath, String[] endWithList ) {
        List<File> fileList = readDirectory( inputPath, endWithList, new ArrayList<>() );
        for( File file : fileList ) readFileByLine( file, outputPath );
    }

    /**
     * 해당 경로의 하위 폴더까지 재귀적으로 탐색하면서 파일 list 를 만든다.
     * @param path 탐색 경로
     * @param endWithList 확장자 list
     * @param result 파일 list
     * @return 파일 list
     */
    public List<File> readDirectory( String path, String[] endWithList, List<File> result ) {
        File dir = new File( path );
        File[] fileList = dir.listFiles();
        for( File file : fileList ) {
            if( file.isFile() ) {
                boolean isTarget = false;
                for( String endWith : endWithList ) {
                    if( !file.getName().endsWith( endWith ) ) continue;
                    isTarget = true;
                }
                if( !isTarget ) continue;
                result.add( file );
            } else {
                try {
                    readDirectory( file.getCanonicalPath(), endWithList, result );
                } catch ( IOException ex ) {
                    log.error( ex );
                }
            }
        }
        return result;
    }

    /**
     * 대상 파일에서 지정된 문자열을 찾는다.
     * @param targetList 파일에서 찾을 문자열 list
     * @param file 대상 파일
     * @param outputPath 결과 저장 경로
     */
    public void searchKeywordInFile( String[] targetList, File file, String outputPath ) {
        BufferedReader reader = null;
        BufferedWriter writer = null;
        try {
            log.info( "Now read " + file.getCanonicalPath() + "..." );
            String name = file.getName();
            reader = new BufferedReader( new InputStreamReader( new FileInputStream( file ), KeyStrings.DEFAULT_FILE_ENCODING ) );
            writer = new BufferedWriter( new OutputStreamWriter( new FileOutputStream( outputPath + "/result_" + name.substring( 0, name.lastIndexOf( "." ) ) + ".txt" ), KeyStrings.DEFAULT_FILE_ENCODING ) );
            String line;
            AtomicLong lineCount = new AtomicLong( 0 );
            while( ( line = reader.readLine() ) != null ) {
                lineCount.getAndIncrement();
                for( String target : targetList ) {
                    if( line.matches( ".*" + target + ".*" ) ) {
                        String result = line.trim();
                        writer.write( target + " is in " + file.getCanonicalPath() + " at " + lineCount.get() + " lines >>>> " + result );
                        writer.newLine();
                        writer.flush();
                        break;
                    }
                }

            }
        } catch ( IOException ex ) {
            log.error( ex );
        } finally {
            try {
                if( reader != null ) reader.close();
                if( writer != null ) writer.close();
            } catch ( IOException ex ) {
                log.error( ex );
            }
        }
    }

    /**
     * 해당 경로의 하위 폴더까지 재귀적으로 탐색하면서 파일에서 지정된 문자열을 찾는다.
     * 설정된 확장자 기준으로 파일을 확인한다.
     * @param targetList 파일에서 찾을 문자열 list
     * @param inputPath 탐색 경로
     * @param outputPath 결과 저장 경로
     * @param endWithList 확장자 list
     */
    public void searchKeywordInFileDirectory( String[] targetList, String inputPath, String outputPath, String[] endWithList ) {
        List<File> fileList = readDirectory( inputPath, endWithList, new ArrayList<>() );
        for( File file : fileList ) searchKeywordInFile( targetList, file, outputPath );
    }
}
