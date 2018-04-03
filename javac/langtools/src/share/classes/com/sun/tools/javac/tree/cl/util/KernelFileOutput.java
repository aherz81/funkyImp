/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sun.tools.javac.tree.cl.util;

import com.sun.tools.javac.tree.LowerTreeImpl;
import java.io.BufferedWriter;
import java.io.Writer;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

/**
 * Encapsulate File Output for OpenCL .cl files.
 * @author Alexander Pöppl
 */
public class KernelFileOutput {

    private JavaFileObject kernelFile;
    private JavaFileManager jFileManager;
    private String fileName;

    public KernelFileOutput(String fileName, LowerTreeImpl state) {
        this.fileName = fileName;
        jFileManager = state.jc.fileManager;
    }

    public void writeToFile(String kernelString) throws IOException {
        Writer writer = null;
        BufferedWriter bufferedWriter = null;
        try {
            kernelFile = jFileManager.getJavaFileForOutput(StandardLocation.CLASS_OUTPUT, fileName, JavaFileObject.Kind.OPENCL_SOURCE, null);

            
            writer = kernelFile.openWriter();
            bufferedWriter = new BufferedWriter(writer);
            
            writeToFile(bufferedWriter, kernelString);
        } catch (IOException ex) {
            Logger.getLogger(KernelFileOutput.class.getName()).log(Level.SEVERE, "Unable to open writing streams", ex);
            throw ex;
        } catch (Throwable t) {
            throw new RuntimeException("Could not write to file!", t);
        } finally {
            try {
                if (bufferedWriter != null) {
                    bufferedWriter.close();
                }
                if (writer != null) {
                    writer.close();

                }
            } catch (IOException ex) {
                Logger.getLogger(KernelFileOutput.class.getName()).log(Level.SEVERE, "Closing the buffers failed.", ex);
                throw ex;
            }
        }
    }

    private void writeToFile(BufferedWriter bufferedWriter, String kernelString) throws IOException {
        try {
            bufferedWriter.write(kernelString);
        } catch (IOException ex) {
            Logger.getLogger(KernelFileOutput.class.getName()).log(Level.SEVERE, "Writing to File failed. Unable to write to file.", ex);
            throw ex;
        }
    }
}
