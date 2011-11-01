/*
 * {{{ header & license
 * Copyright (c) 2006 Wisconsin Court System
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * }}}
 */
package org.docx4j.org.xhtmlrenderer.docx;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Shape;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.List;
import java.util.regex.Pattern;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.docx4j.org.xhtmlrenderer.context.StyleReference;
import org.docx4j.org.xhtmlrenderer.css.style.CalculatedStyle;
import org.docx4j.org.xhtmlrenderer.extend.NamespaceHandler;
import org.docx4j.org.xhtmlrenderer.extend.UserInterface;
import org.docx4j.org.xhtmlrenderer.layout.BoxBuilder;
import org.docx4j.org.xhtmlrenderer.layout.Layer;
import org.docx4j.org.xhtmlrenderer.layout.LayoutContext;
import org.docx4j.org.xhtmlrenderer.layout.SharedContext;
import org.docx4j.org.xhtmlrenderer.pdf.ITextFontContext;
import org.docx4j.org.xhtmlrenderer.pdf.ITextFontResolver;
import org.docx4j.org.xhtmlrenderer.pdf.ITextOutputDevice;
import org.docx4j.org.xhtmlrenderer.pdf.ITextUserAgent;
import org.docx4j.org.xhtmlrenderer.render.BlockBox;
import org.docx4j.org.xhtmlrenderer.render.PageBox;
import org.docx4j.org.xhtmlrenderer.render.RenderingContext;
import org.docx4j.org.xhtmlrenderer.render.ViewportBox;
import org.docx4j.org.xhtmlrenderer.resource.XMLResource;
import org.docx4j.org.xhtmlrenderer.simple.extend.XhtmlNamespaceHandler;
import org.docx4j.org.xhtmlrenderer.util.Configuration;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfWriter;


public class DocxRenderer {

    private final SharedContext _sharedContext;
    private final Docx4jDocxOutputDevice _outputDevice;

    private Document _doc;


    private BlockBox _root;
    public BlockBox getRootBox() {
        return _root;
    }

    public DocxRenderer() {

        _outputDevice = new Docx4jDocxOutputDevice();

        Docx4jUserAgent userAgent = new Docx4jUserAgent(_outputDevice);        
        _sharedContext = new SharedContext();
        _sharedContext.setUserAgentCallback(userAgent);
        _sharedContext.setCss(new StyleReference(userAgent));
        userAgent.setSharedContext(_sharedContext);
//        _outputDevice.setSharedContext(_sharedContext);

        // Would need the font stuff if we did createPDF; 
        // don't want to use ITextFontResolver since don't want to
        // require that JAR.  The idea is to 
        // (some methods expect that object)
//        ITextFontResolver fontResolver = new ITextFontResolver(_sharedContext);
//        _sharedContext.setFontResolver(fontResolver);        

        Docx4jReplacedElementFactory replacedElementFactory =
            new Docx4jReplacedElementFactory(_outputDevice);
        _sharedContext.setReplacedElementFactory(replacedElementFactory);

        _sharedContext.setTextRenderer(new Docx4jTextRenderer());
//        _sharedContext.setDPI(72*_dotsPerPoint);
//        _sharedContext.setDotsPerPixel(dotsPerPixel);
        _sharedContext.setPrint(true);
        _sharedContext.setInteractive(false);
    }
    
    public SharedContext getSharedContext() {
        return _sharedContext;
    }


    private Document loadDocument(final String uri) {
        return _sharedContext.getUac().getXMLResource(uri).getDocument();
    }

    public void setDocument(String uri) {
        setDocument(loadDocument(uri), uri);
    }

    public void setDocument(Document doc, String url) {
        setDocument(doc, url, new XhtmlNamespaceHandler());
    }

    public void setDocument(File file)
            throws IOException {

        File parent = file.getAbsoluteFile().getParentFile();
        setDocument(
                loadDocument(file.toURI().toURL().toExternalForm()),
                (parent == null ? "" : parent.toURI().toURL().toExternalForm())
        );
    }

    public void setDocumentFromString(String content) {
        setDocumentFromString(content, null);
    }

    public void setDocumentFromString(String content, String baseUrl) {
        InputSource is = new InputSource(new BufferedReader(new StringReader(content)));
        Document dom = XMLResource.load(is).getDocument();

        setDocument(dom, baseUrl);
    }

    public void setDocument(Document doc, String url, NamespaceHandler nsh) {
        _doc = doc;

//        getFontResolver().flushFontFaceFonts();

        _sharedContext.reset();
        if (Configuration.isTrue("xr.cache.stylesheets", true)) {
            _sharedContext.getCss().flushStyleSheets();
        } else {
            _sharedContext.getCss().flushAllStyleSheets();
        }
        _sharedContext.setBaseURL(url);
        _sharedContext.setNamespaceHandler(nsh);
        _sharedContext.getCss().setDocumentContext(
                _sharedContext, _sharedContext.getNamespaceHandler(),
                doc, new NullUserInterface());
//        getFontResolver().importFontFaces(_sharedContext.getCss().getFontFaceRules());
    }


    public void layout() {
        LayoutContext c = newLayoutContext();
        BlockBox root = BoxBuilder.createRootBox(c, _doc);
        root.setContainingBlock(new ViewportBox(getInitialExtents(c)));
        root.layout(c);
        Dimension dim = root.getLayer().getPaintingDimension(c);
        root.getLayer().trimEmptyPages(c, dim.height);
        root.getLayer().layoutPages(c);
        _root = root;
    }

    private Rectangle getInitialExtents(LayoutContext c) {
        PageBox first = Layer.createPageBox(c, "first");

        return new Rectangle(0, 0, first.getContentWidth(c), first.getContentHeight(c));
    }

    private RenderingContext newRenderingContext() {
        RenderingContext result = _sharedContext.newRenderingContextInstance();
        result.setFontContext(new ITextFontContext());


        result.setOutputDevice(_outputDevice);

        _sharedContext.getTextRenderer().setup(result.getFontContext());

        result.setRootLayer(_root.getLayer());

        return result;
    }

    private LayoutContext newLayoutContext() {
        LayoutContext result = _sharedContext.newLayoutContextInstance();
        result.setFontContext(new ITextFontContext());

        _sharedContext.getTextRenderer().setup(result.getFontContext());

        return result;
    }

    public void createPDF(OutputStream os) throws DocumentException {
        createPDF(os, true, 0);
    }

//    public void writeNextDocument() throws DocumentException {
//        writeNextDocument(0);
//    }

//    public void writeNextDocument(int initialPageNo) throws DocumentException {
//        List pages = _root.getLayer().getPages();
//
//        RenderingContext c = newRenderingContext();
//        c.setInitialPageNo(initialPageNo);
//        PageBox firstPage = (PageBox)pages.get(0);
//        com.lowagie.text.Rectangle firstPageSize = new com.lowagie.text.Rectangle(
//                0, 0,
//                firstPage.getWidth(c) / _dotsPerPoint,
//                firstPage.getHeight(c) / _dotsPerPoint);
//
//        _outputDevice.setStartPageNo(_writer.getPageNumber());
//
//        _pdfDoc.setPageSize(firstPageSize);
//        _pdfDoc.newPage();
//
//        writePDF(pages, c, firstPageSize, _pdfDoc, _writer);
//    }
    
//
//    public void finishPDF() {
//    }
//
//    public void createPDF(OutputStream os, boolean finish) throws DocumentException {
//        createPDF(os, finish, 0);
//    }

    /**
     * <B>NOTE:</B> Caller is responsible for cleaning up the OutputStream if something
     * goes wrong.
     */
    public void createPDF(OutputStream os, boolean finish, int initialPageNo) throws DocumentException {
        List pages = _root.getLayer().getPages();

        RenderingContext c = newRenderingContext();
        c.setInitialPageNo(initialPageNo);
        PageBox firstPage = (PageBox)pages.get(0);
//        com.lowagie.text.Rectangle firstPageSize = new com.lowagie.text.Rectangle(
//                0, 0,
//                firstPage.getWidth(c) / _dotsPerPoint,
//                firstPage.getHeight(c) / _dotsPerPoint);

//        com.lowagie.text.Document doc =
//            new com.lowagie.text.Document(firstPageSize, 0, 0, 0, 0);
//        PdfWriter writer = PdfWriter.getInstance(doc, os);
//
//        _pdfDoc = doc;
//        _writer = writer;

//        doc.open();

//        writePDF(pages, c, firstPageSize, doc, writer);
        writePDF(pages, c);

//        if (finish) {
//            doc.close();
//        }
    }


//    private void writePDF(List pages, RenderingContext c, com.lowagie.text.Rectangle firstPageSize, com.lowagie.text.Document doc, PdfWriter writer) throws DocumentException {
    private void writePDF(List pages, RenderingContext c) throws DocumentException {

//        _outputDevice.setRoot(_root);
//
//        _outputDevice.start(_doc);
//        _outputDevice.setWriter(writer);
//        _outputDevice.initializePage(writer.getDirectContent(), firstPageSize.getHeight());

        _root.getLayer().assignPagePaintingPositions(c, Layer.PAGED_MODE_PRINT);

        int pageCount = _root.getLayer().getPages().size();
        c.setPageCount(pageCount);
        for (int i = 0; i < pageCount; i++) {
            System.out.println("page " + i);
            PageBox currentPage = (PageBox)pages.get(i);
            c.setPage(i, currentPage);
//            paintPage(c, writer, currentPage);
            paintPage(c, currentPage);
//            _outputDevice.finishPage();
            if (i != pageCount - 1) {
                PageBox nextPage = (PageBox)pages.get(i+1);
//                com.lowagie.text.Rectangle nextPageSize = new com.lowagie.text.Rectangle(
//                        0, 0,
//                        nextPage.getWidth(c) / _dotsPerPoint,
//                        nextPage.getHeight(c) / _dotsPerPoint);
//                doc.setPageSize(nextPageSize);
//                doc.newPage();
//                _outputDevice.initializePage(
//                        writer.getDirectContent(), nextPageSize.getHeight());
            }
        }

//        _outputDevice.finish(c, _root);
    }

    private void paintPage(RenderingContext c, PageBox page) {    
//    private void paintPage(RenderingContext c, PdfWriter writer, PageBox page) {
//        provideMetadataToPage(writer, page);

        page.paintBackground(c, 0, Layer.PAGED_MODE_PRINT);
        page.paintMarginAreas(c, 0, Layer.PAGED_MODE_PRINT);
        page.paintBorder(c, 0, Layer.PAGED_MODE_PRINT);

        Shape working = _outputDevice.getClip();

        Rectangle content = page.getPrintClippingBounds(c);
        _outputDevice.clip(content);

        int top = -page.getPaintingTop() +
            page.getMarginBorderPadding(c, CalculatedStyle.TOP);

        int left = page.getMarginBorderPadding(c, CalculatedStyle.LEFT);

        _outputDevice.translate(left, top);
        _root.getLayer().paint(c);
        _outputDevice.translate(-left, -top);

        _outputDevice.setClip(working);
    }
    

//    private static Element getFirstChildElement(Element element) {
//        Node n = element.getFirstChild();
//        while (n != null) {
//            if (n.getNodeType() == Node.ELEMENT_NODE) {
//                return (Element)n;
//            }
//            n = n.getNextSibling();
//        }
//        return null;
//    }
//
//    private String createXPacket(String metadata) {
//        StringBuffer result = new StringBuffer(metadata.length() + 50);
//        result.append("<?xpacket begin='\uFEFF' id='W5M0MpCehiHzreSzNTczkc9d'?>\n");
//        result.append(metadata);
//        result.append("\n<?xpacket end='r'?>");
//
//        return result.toString();
//    }
//
//    public ITextOutputDevice getOutputDevice() {
//        return _outputDevice;
//    }
//
//    public SharedContext getSharedContext() {
//        return _sharedContext;
//    }
//
//    public void exportText(Writer writer) throws IOException {
//        RenderingContext c = newRenderingContext();
//        c.setPageCount(_root.getLayer().getPages().size());
//        _root.exportText(c, writer);
//    }
//
//    public BlockBox getRootBox() {
//        return _root;
//    }
//
//    public float getDotsPerPoint() {
//        return _dotsPerPoint;
//    }
//
//    public List findPagePositionsByID(Pattern pattern) {
//        return _outputDevice.findPagePositionsByID(newLayoutContext(), pattern);
//    }
//
    private static final class NullUserInterface implements UserInterface {
        public boolean isHover(Element e) {
            return false;
        }

        public boolean isActive(Element e) {
            return false;
        }

        public boolean isFocus(Element e) {
            return false;
        }
    }
//
//
//    public PdfWriter getWriter() {
//        return _writer;
//    }
}
