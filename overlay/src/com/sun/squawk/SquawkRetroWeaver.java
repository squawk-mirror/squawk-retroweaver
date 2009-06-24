package com.sun.squawk;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import net.sourceforge.retroweaver.RetroWeaver;
import net.sourceforge.retroweaver.Weaver;
import net.sourceforge.retroweaver.event.WeaveListener;
import net.sourceforge.retroweaver.translator.NameTranslator;

public class SquawkRetroWeaver {
    
    public static void main(String... args) {
        if (args.length != 2) {
            System.out.println("Usage");
            System.out.println("  classesDir outputDir");
            System.exit(1);
        }
        File classesDir = new File(args[0]);
        File outputDir = new File(args[1]);
        SquawkRetroWeaver weaver = new SquawkRetroWeaver();
        weaver.retroweave(classesDir, outputDir, new WeaveListener() {
            public void weavingStarted(String msg) {
                System.out.println("RetroWeaver:" + msg);
            }
            public void weavingPath(String sourcePath) {
            }
            public void weavingError(String msg) {
                System.err.println("RetroWeaver ERROR:" + msg);
            }
            public void weavingCompleted(String msg) {
                System.out.println("RetroWeaver:" + msg);
            }
        });
    }

    protected void buildFileNameSets(ArrayList<String[]> fileNameSets, File path, String trimPart) throws IOException {
        FileFilter classFilter = new FileFilter() {
            public boolean accept(File f) {
                return f.getName().endsWith(".class");
            }
        };
        
        File[] files = path.listFiles(classFilter);
        if (files != null) {
            String[] fileNames = new String[files.length];
            int i=0;
            for (File file: files) {
                String fileName = file.getCanonicalPath();
                if (fileName.startsWith(trimPart)) {
                    fileName = fileName.substring(trimPart.length());
                }
                fileNames[i] = fileName;
                i++;
            }
            fileNameSets.add(fileNames);
        }

        FileFilter subdirFilter = new FileFilter() {
            public boolean accept(File f) {
                return f.isDirectory();
            }
        };
        File[] subdirs = path.listFiles(subdirFilter);
        if (subdirs != null) {
            for (File subdir : subdirs) {
                buildFileNameSets(fileNameSets, subdir, trimPart);
            }
        }
    }

    protected void retroweaverClearMirrors(String fieldName) {
        Throwable t;
        try {
            // Clear out the mirrors defined for the stringBuilderTranslator
            Field stringBuilderTranslatorField = NameTranslator.class.getDeclaredField(fieldName);
            stringBuilderTranslatorField.setAccessible(true);
            NameTranslator stringBuilderTranslator = (NameTranslator) stringBuilderTranslatorField.get(null);
            Field field = NameTranslator.class.getDeclaredField("mirrors");
            field.setAccessible(true);
            Map<?, ?> mirrors = (Map<?, ?>) field.get(stringBuilderTranslator);
            mirrors.clear();
            field = NameTranslator.class.getDeclaredField("namespaces");
            field.setAccessible(true);
            List<?> namespaces = (List<?>) field.get(stringBuilderTranslator);
            namespaces.clear();
            t = null;
        } catch (SecurityException e) {
            t = e;
        } catch (NoSuchFieldException e) {
            t = e;
        } catch (IllegalArgumentException e) {
            t = e;
        } catch (IllegalAccessException e) {
            t = e;
        }
        if (t != null) {
            throw new RuntimeException("Problems clearing mirrors for retroweaver", t);
        }
    }
    
    public void retroweave(File classesDir, File outputDir, WeaveListener weaverListener) {
        retroweaverClearMirrors("generalTranslator");
        retroweaverClearMirrors("harmonyTranslator");
        retroweaverClearMirrors("stringBuilderTranslator");
        RetroWeaver weaver = new RetroWeaver(Weaver.VERSION_1_2);
        if (weaverListener != null) {
            weaver.setListener(weaverListener);
        }
        weaver.setLazy(true);
        weaver.setStripSignatures(true);
        weaver.setStripAttributes(true);
        try {
            ArrayList<String[]> fileNameSets = new ArrayList<String[]>();
            buildFileNameSets(fileNameSets, classesDir, classesDir.getCanonicalPath() + File.separator);
            String[][] fileNameSetsArray = fileNameSets.toArray(new String[fileNameSets.size()][]);
            File[] baseDirs = new File[fileNameSetsArray.length];
            Arrays.fill(baseDirs, classesDir);
            weaver.weave(baseDirs, fileNameSetsArray, outputDir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
}
