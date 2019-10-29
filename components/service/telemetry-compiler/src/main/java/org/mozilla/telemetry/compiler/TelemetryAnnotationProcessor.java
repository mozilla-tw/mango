/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.telemetry.compiler;


import org.mozilla.telemetry.annotation.TelemetryDoc;
import org.mozilla.telemetry.annotation.TelemetryExtra;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

public class TelemetryAnnotationProcessor extends AbstractProcessor {

    static final String FILE_README = "/docs/events.md";          // tracked
    static final String FILE_CSV = "/docs/amplitude.csv";    // not tracked

    // TODO: TelemetryEvent's fields are private, I'll create a PR to make them public so I can
    // test the ping format in compile time.
    static class TelemetryEventConstant {
        private static final int MAX_LENGTH_CATEGORY = 30;
        private static final int MAX_LENGTH_METHOD = 20;
        private static final int MAX_LENGTH_OBJECT = 20;
        private static final int MAX_LENGTH_VALUE = 80;
        private static final int MAX_EXTRA_KEYS = 200;
        private static final int MAX_LENGTH_EXTRA_KEY = 15;
        private static final int MAX_LENGTH_EXTRA_VALUE = 80;
    }


    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new LinkedHashSet<>();
        types.add(TelemetryDoc.class.getCanonicalName());
        return types;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
        Collection<? extends Element> annotatedElements =
                env.getElementsAnnotatedWith(TelemetryDoc.class);

        if (annotatedElements.size() == 0) {
            return false;
        }
        try {
            final String kaptGeneratedSourceFolder = fetchSourcePath();

            final String header = "| Event | category | method | object | value | extra |\n" +
                    "| ---- | ---- | ---- | ---- | ---- | ---- |\n";
            genDoc(annotatedElements, header, kaptGeneratedSourceFolder + FILE_README, '|');


            genDoc(annotatedElements, "", kaptGeneratedSourceFolder + FILE_CSV, ',');


        } catch (Exception e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, "Exception while creating Telemetry related documents" + e);
            e.printStackTrace();
        }


        return false;
    }


    private void genDoc(Collection<? extends Element> annotatedElements, String header, String path, char separator) throws FileNotFoundException {

        final File file = new File(path);
        if (file.exists()) {
            file.delete();
        }
        File directory = new File(file.getParentFile().getAbsolutePath());
        directory.mkdirs();


        final PrintWriter printWriter = new PrintWriter(new FileOutputStream(file, true));
        StringBuffer sb = new StringBuffer().append(header);

        char start = separator;
        char end = ' ';
        if (path.contains(FILE_CSV)) {
            start = ' ';
            end = ',';                      // csv needs an extra column: amplitude_property
        }

        // check duplication
        final HashMap<String, Boolean> lookup = new HashMap<>();

        for (Element type : annotatedElements) {
            if (type.getKind() == ElementKind.METHOD) {
                final TelemetryDoc annotation = type.getAnnotation(TelemetryDoc.class);
                verifyEventFormat(annotation);
                final String result = verifyEventDuplication(annotation, lookup);
                if (result != null) {
                    throw new IllegalArgumentException("Duplicate event combination:" + annotation + "\n" + result);
                }

                // value may have ',' so we add a placeholder '"' for csv files
                sb.append(start).append(annotation.name()).append(separator)
                        .append(annotation.category()).append(separator)
                        .append(annotation.method()).append(separator)
                        .append(annotation.object()).append(separator)
                        .append('"').append(annotation.value()).append('"').append(separator);

                // extras may have ',' so we add a placeholder '"' for csv files
                sb.append('"');
                for (TelemetryExtra extra : annotation.extras()) {
                    sb.append(extra.name()).append("=").append(extra.value() + ',');
                }
                sb.append('"');
                sb.append(end);
                printWriter.println(sb);
                sb = new StringBuffer();
            } else {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "This should not happen:" + type);
            }
        }


        printWriter.close();
    }

    String verifyEventDuplication(TelemetryDoc annotation, HashMap<String, Boolean> lookup) {
        String key = annotation.category() + annotation.method() + annotation.object() + annotation.value();
        if (lookup.containsKey(key)) {
            return key;
        }
        lookup.put(key, true);
        return null;

    }

    private void verifyEventFormat(TelemetryDoc annotation) {
        final String action = annotation.category();
        if (action.length() > TelemetryEventConstant.MAX_LENGTH_CATEGORY) {
            throw new IllegalArgumentException("The length of category is too long:" + action);
        }
        final String method = annotation.method();
        if (method.length() > TelemetryEventConstant.MAX_LENGTH_METHOD) {
            throw new IllegalArgumentException("The length of method is too long:" + method);
        }
        final String object = annotation.object();
        if (object.length() > TelemetryEventConstant.MAX_LENGTH_OBJECT) {
            throw new IllegalArgumentException("The length of object is too long:" + object);
        }
        final String value = annotation.value();
        if (value.length() > TelemetryEventConstant.MAX_LENGTH_VALUE) {
            throw new IllegalArgumentException("The length of value is too long:" + value);
        }
        final TelemetryExtra[] extras = annotation.extras();
        if (extras.length > TelemetryEventConstant.MAX_EXTRA_KEYS) {
            throw new IllegalArgumentException("Too many extras");
        }
        for (TelemetryExtra extra : extras) {
            final String eName = extra.name();
            final String eVal = extra.value();
            if (eName.length() > TelemetryEventConstant.MAX_LENGTH_EXTRA_KEY) {
                throw new IllegalArgumentException("The length of extra key is too long:" + eName);
            }
            if (eVal.length() > TelemetryEventConstant.MAX_LENGTH_VALUE) {
                throw new IllegalArgumentException("The length of extra value is too long:" + eVal);
            }
        }

    }

    String fetchSourcePath() throws IOException {
        JavaFileObject generationForPath = processingEnv.getFiler().createSourceFile("TempFile" + System.currentTimeMillis());
        Writer writer = generationForPath.openWriter();
        String sourcePath = new File(generationForPath.toUri().getPath()).getParentFile().getAbsolutePath();
        writer.close();
        generationForPath.delete();

        return sourcePath;
    }
}
