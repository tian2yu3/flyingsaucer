package org.xhtmlrenderer.docx;

import com.lowagie.text.DocumentException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.HashMap;

public class DocxCreator {

    /**
     * Renders the XML file at the given URL as a Docx file
     * at the target location.
     *
     * @param url url for the XML file to render
     * @param Docx path to the Docx file to create
     * @param DocxVersion version of Docx to output; null uses default version
     * @throws IOException       if the URL or Docx location is
     *                           invalid
     * @throws DocumentException if an error occurred
     *                           while building the Document.
     */
    public static void renderToDocx(String url, String Docx)
            throws IOException, DocumentException {

        DocxRenderer renderer = new DocxRenderer();
        renderer.setDocument(url);
        doRenderToDocx(renderer, Docx);
    }

    /**
     * Renders the XML file as a Docx file at the target location.
     *
     * @param file XML file to render
     * @param Docx  path to the Docx file to create
     * @throws IOException       if the file or Docx location is
     *                           invalid
     * @throws DocumentException if an error occurred
     *                           while building the Document.
     */
    public static void renderToDocx(File file, String Docx)
            throws IOException, DocumentException {

        renderToDocx(file, Docx, null);
    }

    /**
     * Renders the XML file as a Docx file at the target location.
     *
     * @param file XML file to render
     * @param Docx  path to the Docx file to create
     * @param DocxVersion version of Docx to output; null uses default version
     * @throws IOException       if the file or Docx location is
     *                           invalid
     * @throws DocumentException if an error occurred
     *                           while building the Document.
     */
    public static void renderToDocx(File file, String Docx, Character DocxVersion)
            throws IOException, DocumentException {

        DocxRenderer renderer = new DocxRenderer();
        renderer.setDocument(file);
        doRenderToDocx(renderer, Docx);
    }

    /**
     * Internal use, runs the render process
     * @param renderer
     * @param Docx
     * @throws com.lowagie.text.DocumentException
     * @throws java.io.IOException
     */
    private static void doRenderToDocx(DocxRenderer renderer, String Docx)
            throws IOException, DocumentException {
        OutputStream os = null;
        try {
            os = new FileOutputStream(Docx);
            renderer.layout();
            renderer.createDocx(os);

            os.close();
            os = null;
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    /**
     * Renders a file or URL to a Docx. Command line use: first
     * argument is URL or file path, second
     * argument is path to Docx file to generate.
     *
     * @param args see desc
     * @throws IOException if source could not be read, or if
     * Docx path is invalid
     * @throws DocumentException if an error occurs while building
     * the document
     */
    public static void main(String[] args) throws IOException, DocumentException {
        if (args.length < 2) {
            usage("Incorrect argument list.");
        }
        String url = args[0];
        if (url.indexOf("://") == -1) {
            // maybe it's a file
            File f = new File(url);
            if (f.exists()) {
                DocxRenderer.renderToDocx(f, args[1]);
            } else {
                usage("File to render is not found: " + url);
            }
        } else {
            DocxRenderer.renderToDocx(url, args[1]);
        }
    }


    /** prints out usage information, with optional error message
     * @param err
     */
    private static void usage(String err) {
        if (err != null && err.length() > 0) {
            System.err.println("==>" + err);
        }
        System.err.println("Usage: ... url Docx ");
        System.exit(1);
    }
}
