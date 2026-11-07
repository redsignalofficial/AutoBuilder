package dev.autobuilder.schematic.format;

import dev.autobuilder.schematic.Schematic;

import java.io.File;
import java.io.IOException;

public interface ISchematicHandler {
    boolean supports(File file);
    Schematic read(File file) throws IOException;
    void write(Schematic schematic, File file) throws IOException;
}
