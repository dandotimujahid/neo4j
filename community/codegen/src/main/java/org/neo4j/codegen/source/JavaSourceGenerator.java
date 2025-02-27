/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.neo4j.codegen.source;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import org.neo4j.codegen.ByteCodes;
import org.neo4j.codegen.ClassWriter;
import org.neo4j.codegen.CodeGenerator;
import org.neo4j.codegen.CompilationFailureException;
import org.neo4j.codegen.TypeReference;

/**
 * {@link CodeGenerator} that generates code by writing java source files, and compiling these using {@link JavaCompiler}.
 * <p/>
 * NOTE: This generator does not support generating classes that depend on previously generated classes. If you
 *       need to generate multiple dependent classes they have to be compiled in the same compilation unit (See {@link CodeGenerator})
 */
class JavaSourceGenerator extends CodeGenerator {
    private final Configuration configuration;
    private final Map<TypeReference, StringBuilder> classes = new HashMap<>();
    private final JavaSourceCompiler compiler;

    JavaSourceGenerator(ClassLoader parentClassLoader, Configuration configuration, JavaSourceCompiler compiler) {
        super(parentClassLoader);
        this.configuration = configuration;
        this.compiler = compiler;
    }

    @Override
    protected ClassWriter generate(
            TypeReference type, TypeReference base, TypeReference[] dependencies, TypeReference[] interfaces) {
        StringBuilder target = new StringBuilder();
        synchronized (this) {
            StringBuilder old = classes.put(type, target);
            if (old != null) {
                classes.put(type, old);
                throw new IllegalStateException("Trying to generate class twice: " + type);
            }
        }
        JavaSourceClassWriter writer = new JavaSourceClassWriter(target, configuration);
        writer.declarePackage(type);
        writer.javadoc("Generated by " + getClass().getName());
        for (TypeReference dependency : dependencies) {
            writer.declareImport(dependency);
        }
        writer.publicClass(type);
        writer.extendClass(base);
        writer.implement(interfaces);
        writer.begin();
        return writer;
    }

    @Override
    protected Iterable<? extends ByteCodes> compile(ClassLoader classpathLoader) throws CompilationFailureException {
        return compiler.compile(sourceFiles(), classpathLoader);
    }

    private synchronized List<JavaSourceFile> sourceFiles() {
        List<JavaSourceFile> sourceFiles;
        synchronized (this) {
            sourceFiles = new ArrayList<>(classes.size());
            for (Map.Entry<TypeReference, StringBuilder> entry : classes.entrySet()) {
                TypeReference reference = entry.getKey();
                StringBuilder source = entry.getValue();
                configuration.visit(reference, source);
                sourceFiles.add(new JavaSourceFile(
                        configuration
                                .sourceBase()
                                .uri(reference.packageName(), reference.name(), JavaFileObject.Kind.SOURCE),
                        source));
            }
            classes.clear();
        }
        return sourceFiles;
    }
}
