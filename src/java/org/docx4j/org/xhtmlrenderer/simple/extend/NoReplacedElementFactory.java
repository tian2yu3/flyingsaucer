package org.docx4j.org.xhtmlrenderer.simple.extend;

import org.docx4j.org.xhtmlrenderer.extend.ReplacedElement;
import org.docx4j.org.xhtmlrenderer.extend.ReplacedElementFactory;
import org.docx4j.org.xhtmlrenderer.extend.UserAgentCallback;
import org.docx4j.org.xhtmlrenderer.layout.LayoutContext;
import org.docx4j.org.xhtmlrenderer.render.BlockBox;
import org.w3c.dom.Element;

public class NoReplacedElementFactory implements ReplacedElementFactory {

    public ReplacedElement createReplacedElement(LayoutContext c, BlockBox box,
            UserAgentCallback uac, int cssWidth, int cssHeight) {
        return null;
    }

    public void remove(Element e) {

    }

    public void setFormSubmissionListener(FormSubmissionListener listener) {
        //TODO
    }

    public void reset() {
    }

}
